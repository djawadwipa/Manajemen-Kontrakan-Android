package id.djawadwipa.manajemenkontrakan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RentalUnitEntity::class,
        InvoiceEntity::class,
        PaymentEntity::class,
        ExpenseCategoryEntity::class,
        ExpenseEntity::class,
        AppSettingEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rentalUnitDao(): RentalUnitDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE payments " +
                        "ADD COLUMN status TEXT NOT NULL DEFAULT 'AKTIF'",
                )
                db.execSQL(
                    "ALTER TABLE payments ADD COLUMN canceledAt INTEGER",
                )
            }
        }
    }
}
