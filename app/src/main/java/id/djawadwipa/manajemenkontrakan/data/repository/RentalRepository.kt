package id.djawadwipa.manajemenkontrakan.data.repository

import androidx.room.withTransaction
import id.djawadwipa.manajemenkontrakan.data.local.AppDatabase
import id.djawadwipa.manajemenkontrakan.data.local.AppSettingEntity
import id.djawadwipa.manajemenkontrakan.data.local.ExpenseEntity
import id.djawadwipa.manajemenkontrakan.data.local.InvoiceEntity
import id.djawadwipa.manajemenkontrakan.data.local.PaymentEntity
import id.djawadwipa.manajemenkontrakan.data.local.RentalUnitEntity
import id.djawadwipa.manajemenkontrakan.util.BackupPayload
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RentalRepository @Inject constructor(
    private val database: AppDatabase,
) {
    val units = database.rentalUnitDao().observeAll()
    val invoices = database.invoiceDao().observeAll()
    val payments = database.paymentDao().observeAll()
    val expenses = database.expenseDao().observeAll()
    val categories = database.expenseCategoryDao().observeAll()
    val settings = database.appSettingDao().observe()

    suspend fun seedIfEmpty() = database.withTransaction {
        if (database.rentalUnitDao().count() == 0) {
            database.appSettingDao().upsert(SeedData.settings)
            database.rentalUnitDao().upsertAll(SeedData.units)
            database.expenseCategoryDao().upsertAll(SeedData.categories)
            database.invoiceDao().insertAll(
                SeedData.invoices(
                    SeedData.settings.activeYear,
                    SeedData.units,
                ),
            )
        }
    }

    suspend fun upsertUnit(unit: RentalUnitEntity) {
        database.rentalUnitDao().upsert(unit)
    }

    suspend fun deleteUnit(unit: RentalUnitEntity) =
        database.rentalUnitDao().delete(unit)

    suspend fun regenerateInvoices(year: Int) = database.withTransaction {
        database.invoiceDao().insertAll(
            SeedData.invoices(
                year,
                database.rentalUnitDao().getAll(),
            ),
        )
    }

    suspend fun createInvoice(invoice: InvoiceEntity) =
        database.withTransaction {
            val unit = requireNotNull(
                database.rentalUnitDao().getById(invoice.unitId),
            ) {
                "Unit tidak ditemukan."
            }
            require(unit.status == "Aktif") {
                "Tagihan hanya dapat dibuat untuk unit aktif."
            }
            require(
                database.invoiceDao().getByUnitAndPeriod(
                    invoice.unitId,
                    invoice.period,
                ) == null,
            ) {
                "Tagihan unit untuk periode tersebut sudah ada."
            }

            database.invoiceDao().upsert(
                normalizedInvoice(
                    invoice = invoice,
                    unit = unit,
                    paid = 0L,
                ),
            )
        }

    suspend fun updateInvoice(invoice: InvoiceEntity) =
        database.withTransaction {
            val existing = requireNotNull(
                database.invoiceDao().getById(invoice.id),
            ) {
                "Tagihan tidak ditemukan."
            }
            require(existing.unitId == invoice.unitId) {
                "Unit tagihan tidak dapat diubah."
            }
            require(existing.period == invoice.period) {
                "Periode tagihan tidak dapat diubah."
            }

            val unit = requireNotNull(
                database.rentalUnitDao().getById(existing.unitId),
            ) {
                "Unit tidak ditemukan."
            }
            val paid = database.paymentDao().totalForInvoice(existing.id)

            database.invoiceDao().upsert(
                normalizedInvoice(
                    invoice = invoice.copy(
                        unitId = existing.unitId,
                        period = existing.period,
                    ),
                    unit = unit,
                    paid = paid,
                ),
            )
        }

    suspend fun deleteInvoice(invoice: InvoiceEntity) =
        database.withTransaction {
            val existing = requireNotNull(
                database.invoiceDao().getById(invoice.id),
            ) {
                "Tagihan tidak ditemukan."
            }
            require(
                database.paymentDao().getForInvoice(existing.id).isEmpty(),
            ) {
                "Tagihan memiliki riwayat pembayaran dan tidak dapat dihapus."
            }

            database.invoiceDao().delete(existing)
        }

    private fun normalizedInvoice(
        invoice: InvoiceEntity,
        unit: RentalUnitEntity,
        paid: Long,
    ): InvoiceEntity {
        runCatching { YearMonth.parse(invoice.period) }
            .getOrElse {
                throw IllegalArgumentException(
                    "Periode tagihan harus berformat yyyy-MM.",
                )
            }
        require(invoice.invoiceDate <= invoice.dueDate) {
            "Tanggal jatuh tempo tidak boleh sebelum tanggal dibuat."
        }
        require(invoice.amount > 0L) {
            "Nominal tagihan harus lebih dari nol."
        }
        require(invoice.amount >= paid) {
            "Nominal tagihan tidak boleh lebih kecil dari pembayaran aktif."
        }
        require(invoice.reserveTarget in 0L..invoice.amount) {
            "Target dana cadangan harus berada antara nol dan nominal tagihan."
        }

        return invoice.copy(
            tenantName = unit.tenantName,
            paid = paid,
            status = invoiceStatus(
                amount = invoice.amount,
                paid = paid,
                dueDate = invoice.dueDate,
            ),
            note = invoice.note.trim(),
        )
    }

    suspend fun recordPayment(payment: PaymentEntity) =
        database.withTransaction {
            savePaymentAndRecalculate(payment)
        }

    suspend fun updatePayment(payment: PaymentEntity) =
        database.withTransaction {
            val existing =
                requireNotNull(database.paymentDao().getById(payment.id)) {
                    "Pembayaran tidak ditemukan."
                }

            require(existing.invoiceId == payment.invoiceId) {
                "Pembayaran tidak dapat dipindahkan ke tagihan lain."
            }
            require(existing.status == "AKTIF") {
                "Pembayaran yang dibatalkan tidak dapat diedit."
            }

            savePaymentAndRecalculate(payment)
        }

    suspend fun cancelPayment(payment: PaymentEntity) =
        database.withTransaction {
            val existing =
                requireNotNull(database.paymentDao().getById(payment.id)) {
                    "Pembayaran tidak ditemukan."
                }

            require(existing.status == "AKTIF") {
                "Pembayaran sudah dibatalkan."
            }

            database.paymentDao().insert(
                existing.copy(
                    status = "DIBATALKAN",
                    canceledAt = System.currentTimeMillis(),
                ),
            )
            recalculateInvoice(existing.invoiceId)
        }

    suspend fun deletePayment(payment: PaymentEntity) =
        database.withTransaction {
            val existing =
                requireNotNull(database.paymentDao().getById(payment.id)) {
                    "Pembayaran tidak ditemukan."
                }

            database.paymentDao().delete(existing)
            recalculateInvoice(existing.invoiceId)
        }

    private suspend fun savePaymentAndRecalculate(
        payment: PaymentEntity,
    ) {
        val invoice =
            requireNotNull(database.invoiceDao().getById(payment.invoiceId)) {
                "Tagihan tidak ditemukan."
            }

        val existing = database.paymentDao().getById(payment.id)
        val currentTotal =
            database.paymentDao().totalForInvoice(invoice.id)
        val totalWithoutCurrent =
            currentTotal - if (existing?.status == "AKTIF") {
                existing.amount
            } else {
                0L
            }
        val newTotal = totalWithoutCurrent + payment.amount

        require(existing?.status != "DIBATALKAN") {
            "Pembayaran yang dibatalkan tidak dapat diedit."
        }
        require(payment.amount > 0) {
            "Nominal pembayaran harus lebih dari nol."
        }
        require(newTotal <= invoice.amount) {
            "Nominal pembayaran melebihi sisa tagihan."
        }

        val installmentNumber =
            existing?.installmentNumber
                ?: database.paymentDao()
                    .maxInstallmentNumber(invoice.id) + 1

        database.paymentDao().insert(
            payment.copy(
                invoiceId = invoice.id,
                unitId = invoice.unitId,
                tenantName = invoice.tenantName,
                period = invoice.period,
                installmentNumber = installmentNumber,
                status = "AKTIF",
                canceledAt = null,
            ),
        )

        recalculateInvoice(invoice.id)
    }

    private suspend fun recalculateInvoice(invoiceId: String) {
        val invoice = database.invoiceDao().getById(invoiceId) ?: return
        val total = database.paymentDao().totalForInvoice(invoiceId)
        database.invoiceDao().updatePayment(
            id = invoice.id,
            paid = total,
            status = invoiceStatus(
                amount = invoice.amount,
                paid = total,
                dueDate = invoice.dueDate,
            ),
        )
    }

    private fun invoiceStatus(
        amount: Long,
        paid: Long,
        dueDate: Long,
    ): String = when {
        paid >= amount -> "LUNAS"
        paid > 0L -> "CICILAN"
        dueDate < LocalDate.now().toEpochDay() -> "MENUNGGAK"
        else -> "MENUNGGU"
    }

    suspend fun upsertExpense(expense: ExpenseEntity) =
        database.expenseDao().upsert(expense)

    suspend fun deleteExpense(expense: ExpenseEntity) =
        database.expenseDao().delete(expense)

    suspend fun updateSettings(setting: AppSettingEntity) =
        database.appSettingDao().upsert(setting)

    suspend fun exportPayload(): BackupPayload = BackupPayload(
        createdAtEpochMillis = System.currentTimeMillis(),
        settings =
            database.appSettingDao().get() ?: AppSettingEntity(),
        units = database.rentalUnitDao().getAll(),
        invoices = database.invoiceDao().getAll(),
        payments = database.paymentDao().getAll(),
        categories = database.expenseCategoryDao().getAll(),
        expenses = database.expenseDao().getAll(),
    )

    suspend fun restorePayload(
        payload: BackupPayload,
    ) = database.withTransaction {
        database.paymentDao().deleteAll()
        database.expenseDao().deleteAll()
        database.invoiceDao().deleteAll()
        database.expenseCategoryDao().deleteAll()
        database.rentalUnitDao().deleteAll()
        database.appSettingDao().deleteAll()

        database.appSettingDao().upsert(payload.settings)
        database.rentalUnitDao().upsertAll(payload.units)
        database.expenseCategoryDao().upsertAll(payload.categories)
        database.invoiceDao().insertAll(payload.invoices)
        database.paymentDao().insertAll(payload.payments)
        database.expenseDao().upsertAll(payload.expenses)

        payload.invoices.forEach { invoice ->
            recalculateInvoice(invoice.id)
        }
    }
}
