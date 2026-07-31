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
    version = 5,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE app_settings " +
                        "ADD COLUMN closedPeriods TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE app_settings " +
                        "ADD COLUMN notificationEnabled " +
                        "INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE app_settings " +
                        "ADD COLUMN dueReminderDays INTEGER NOT NULL DEFAULT 3",
                )
                db.execSQL(
                    "ALTER TABLE app_settings " +
                        "ADD COLUMN notificationHour INTEGER NOT NULL DEFAULT 8",
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE app_settings " +
                        "ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'SYSTEM'",
                )
                db.execSQL(
                    "ALTER TABLE app_settings " +
                        "ADD COLUMN lockEnabled INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE app_settings " +
                        "ADD COLUMN pinSalt TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "ALTER TABLE app_settings " +
                        "ADD COLUMN pinHash TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "ALTER TABLE app_settings " +
                        "ADD COLUMN biometricEnabled INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE app_settings " +
                        "ADD COLUMN autoLockMinutes INTEGER NOT NULL DEFAULT 1",
                )
            }
        }
    }
}
