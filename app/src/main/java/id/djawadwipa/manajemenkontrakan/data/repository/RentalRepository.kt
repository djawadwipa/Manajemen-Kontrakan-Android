package id.djawadwipa.manajemenkontrakan.data.repository

import androidx.room.withTransaction
import id.djawadwipa.manajemenkontrakan.data.local.AppDatabase
import id.djawadwipa.manajemenkontrakan.data.local.AppSettingEntity
import id.djawadwipa.manajemenkontrakan.data.local.ExpenseCategoryEntity
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
        val closed = closedPeriods(currentSettings())
        database.invoiceDao().insertAll(
            SeedData.invoices(
                year,
                database.rentalUnitDao().getAll(),
            ).filterNot { it.period in closed },
        )
    }

    suspend fun createInvoice(invoice: InvoiceEntity) =
        database.withTransaction {
            requirePeriodOpen(invoice.period)
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
            requirePeriodOpen(existing.period)
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
            requirePeriodOpen(existing.period)
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
        parsePeriod(invoice.period)
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

            requirePeriodOpen(existing.period)
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

            requirePeriodOpen(existing.period)
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

            requirePeriodOpen(existing.period)
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
        requirePeriodOpen(invoice.period)

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
        database.withTransaction {
            val existing = database.expenseDao().getById(expense.id)
            existing?.let { requirePeriodOpen(it.period) }
            requirePeriodOpen(expense.period)

            val expenseDate = LocalDate.ofEpochDay(expense.expenseDate)
            require(expense.period == YearMonth.from(expenseDate).toString()) {
                "Periode pengeluaran harus sesuai dengan tanggal pengeluaran."
            }
            require(expense.description.isNotBlank()) {
                "Uraian pengeluaran wajib diisi."
            }
            require(expense.amount > 0L) {
                "Nominal pengeluaran harus lebih dari nol."
            }

            database.expenseDao().upsert(
                expense.copy(
                    description = expense.description.trim(),
                    receiptNumber = expense.receiptNumber.trim(),
                    note = expense.note.trim(),
                ),
            )
        }

    suspend fun deleteExpense(expense: ExpenseEntity) =
        database.withTransaction {
            val existing = requireNotNull(
                database.expenseDao().getById(expense.id),
            ) {
                "Pengeluaran tidak ditemukan."
            }
            requirePeriodOpen(existing.period)
            database.expenseDao().delete(existing)
        }

    suspend fun upsertExpenseCategory(category: ExpenseCategoryEntity) =
        database.withTransaction {
            val cleaned = category.copy(
                name = category.name.trim(),
                groupName = category.groupName.trim(),
                profitLossRule = category.profitLossRule.trim(),
            )
            require(cleaned.name.isNotBlank()) {
                "Nama kategori wajib diisi."
            }
            require(cleaned.groupName.isNotBlank()) {
                "Kelompok kategori wajib dipilih."
            }
            require(cleaned.profitLossRule.isNotBlank()) {
                "Aturan laba-rugi wajib dipilih."
            }

            val categories = database.expenseCategoryDao().getAll()
            require(
                categories.none {
                    it.id != cleaned.id &&
                        it.name.equals(cleaned.name, ignoreCase = true)
                },
            ) {
                "Nama kategori sudah digunakan."
            }

            val existing = categories.firstOrNull { it.id == cleaned.id }
            val affectedExpenses = if (existing == null) {
                emptyList()
            } else {
                database.expenseDao().getAll()
                    .filter { it.category == existing.name }
            }
            val closed = closedPeriods(currentSettings())
            require(affectedExpenses.none { it.period in closed }) {
                "Kategori digunakan pada periode yang sudah ditutup. Buka buku kembali sebelum mengubah kategori."
            }

            database.expenseCategoryDao().upsert(cleaned)

            if (existing != null) {
                val updatedExpenses = affectedExpenses.map { expense ->
                    expense.copy(
                        category = cleaned.name,
                        groupName = cleaned.groupName,
                        includeInProfitLoss = when (
                            cleaned.profitLossRule
                        ) {
                            "Ya" -> true
                            "Tidak" -> false
                            else -> expense.includeInProfitLoss
                        },
                    )
                }
                database.expenseDao().upsertAll(updatedExpenses)
            }
        }

    suspend fun deleteExpenseCategory(category: ExpenseCategoryEntity) =
        database.withTransaction {
            val existing = database.expenseCategoryDao().getAll()
                .firstOrNull { it.id == category.id }
                ?: return@withTransaction
            require(
                database.expenseDao().getAll()
                    .none { it.category == existing.name },
            ) {
                "Kategori masih digunakan oleh pengeluaran dan tidak dapat dihapus."
            }
            database.expenseCategoryDao().delete(existing)
        }

    suspend fun updateSettings(setting: AppSettingEntity) =
        database.withTransaction {
            database.appSettingDao().upsert(normalizedSettings(setting))
        }

    suspend fun closeBook(period: String) = database.withTransaction {
        parsePeriod(period)
        val settings = currentSettings()
        val closed = closedPeriods(settings).toMutableSet()
        require(closed.add(period)) {
            "Periode $period sudah ditutup."
        }
        database.appSettingDao().upsert(
            normalizedSettings(
                settings.copy(closedPeriods = serializePeriods(closed)),
            ),
        )
    }

    suspend fun reopenBook(period: String) = database.withTransaction {
        parsePeriod(period)
        val settings = currentSettings()
        val closed = closedPeriods(settings).toMutableSet()
        require(closed.remove(period)) {
            "Periode $period sudah terbuka."
        }
        database.appSettingDao().upsert(
            normalizedSettings(
                settings.copy(closedPeriods = serializePeriods(closed)),
            ),
        )
    }

    suspend fun exportPayload(): BackupPayload = BackupPayload(
        createdAtEpochMillis = System.currentTimeMillis(),
        settings = currentSettings(),
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

        database.appSettingDao().upsert(normalizedSettings(payload.settings))
        database.rentalUnitDao().upsertAll(payload.units)
        database.expenseCategoryDao().upsertAll(payload.categories)
        database.invoiceDao().insertAll(payload.invoices)
        database.paymentDao().insertAll(payload.payments)
        database.expenseDao().upsertAll(payload.expenses)

        payload.invoices.forEach { invoice ->
            recalculateInvoice(invoice.id)
        }
    }

    private suspend fun currentSettings(): AppSettingEntity =
        database.appSettingDao().get() ?: AppSettingEntity()

    private suspend fun requirePeriodOpen(period: String) {
        parsePeriod(period)
        require(period !in closedPeriods(currentSettings())) {
            "Periode $period sudah ditutup. Buka buku kembali sebelum mengubah transaksi."
        }
    }

    private fun parsePeriod(period: String): YearMonth =
        runCatching { YearMonth.parse(period) }
            .getOrElse {
                throw IllegalArgumentException(
                    "Periode harus berformat yyyy-MM.",
                )
            }

    private fun closedPeriods(settings: AppSettingEntity): Set<String> =
        settings.closedPeriods
            .split(',')
            .map(String::trim)
            .filter(String::isNotBlank)
            .filter { runCatching { YearMonth.parse(it) }.isSuccess }
            .toSortedSet()

    private fun serializePeriods(periods: Set<String>): String =
        periods.toSortedSet().joinToString(",")

    private fun dashboardPeriod(settings: AppSettingEntity): String =
        YearMonth.of(
            settings.activeYear,
            settings.dashboardMonth.coerceIn(1, 12),
        ).toString()

    private fun normalizedSettings(
        setting: AppSettingEntity,
    ): AppSettingEntity {
        val closed = closedPeriods(setting)
        return setting.copy(
            bookStatus = if (dashboardPeriod(setting) in closed) {
                "CLOSED"
            } else {
                "OPEN"
            },
            closedPeriods = serializePeriods(closed),
        )
    }
}
