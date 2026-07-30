package id.djawadwipa.manajemenkontrakan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RentalUnitEntity::class,
        InvoiceEntity::class,
        PaymentEntity::class,
        ExpenseCategoryEntity::class,
        ExpenseEntity::class,
        AppSettingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rentalUnitDao(): RentalUnitDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun appSettingDao(): AppSettingDao
}
