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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RentalRepository @Inject constructor(private val database: AppDatabase) {
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
            database.invoiceDao().insertAll(SeedData.invoices(SeedData.settings.activeYear, SeedData.units))
        }
    }

    suspend fun upsertUnit(unit: RentalUnitEntity) {
        database.rentalUnitDao().upsert(unit)
    }

    suspend fun deleteUnit(unit: RentalUnitEntity) = database.rentalUnitDao().delete(unit)

    suspend fun regenerateInvoices(year: Int) = database.withTransaction {
        database.invoiceDao().insertAll(SeedData.invoices(year, database.rentalUnitDao().getAll()))
    }

    suspend fun recordPayment(payment: PaymentEntity) = database.withTransaction {
        val invoice = database.invoiceDao().getById(payment.invoiceId) ?: return@withTransaction
        val remaining = (invoice.amount - invoice.paid).coerceAtLeast(0)
        require(payment.amount in 1..remaining) { "Nominal pembayaran melebihi sisa tagihan." }
        database.paymentDao().insert(payment)
        val total = database.paymentDao().totalForInvoice(invoice.id)
        val status = when {
            total >= invoice.amount -> "LUNAS"
            total > 0 -> "CICILAN"
            invoice.dueDate < LocalDate.now().toEpochDay() -> "MENUNGGAK"
            else -> "MENUNGGU"
        }
        database.invoiceDao().updatePayment(invoice.id, total, status)
    }

    suspend fun upsertExpense(expense: ExpenseEntity) = database.expenseDao().upsert(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = database.expenseDao().delete(expense)
    suspend fun updateSettings(setting: AppSettingEntity) = database.appSettingDao().upsert(setting)

    suspend fun exportPayload(): BackupPayload = BackupPayload(
        createdAtEpochMillis = System.currentTimeMillis(),
        settings = database.appSettingDao().get() ?: AppSettingEntity(),
        units = database.rentalUnitDao().getAll(),
        invoices = database.invoiceDao().getAll(),
        payments = database.paymentDao().getAll(),
        categories = database.expenseCategoryDao().getAll(),
        expenses = database.expenseDao().getAll(),
    )

    suspend fun restorePayload(payload: BackupPayload) = database.withTransaction {
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
    }
}
