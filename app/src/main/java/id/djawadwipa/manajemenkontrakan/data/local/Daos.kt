package id.djawadwipa.manajemenkontrakan.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RentalUnitDao {
    @Query("SELECT * FROM rental_units ORDER BY code") fun observeAll(): Flow<List<RentalUnitEntity>>
    @Query("SELECT * FROM rental_units ORDER BY code") suspend fun getAll(): List<RentalUnitEntity>
    @Query("SELECT COUNT(*) FROM rental_units") suspend fun count(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: RentalUnitEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(items: List<RentalUnitEntity>)
    @Delete suspend fun delete(item: RentalUnitEntity)
    @Query("DELETE FROM rental_units") suspend fun deleteAll()
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY period DESC, unitId") fun observeAll(): Flow<List<InvoiceEntity>>
    @Query("SELECT * FROM invoices ORDER BY period, unitId") suspend fun getAll(): List<InvoiceEntity>
    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1") suspend fun getById(id: String): InvoiceEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: InvoiceEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(items: List<InvoiceEntity>)
    @Query("UPDATE invoices SET paid = :paid, status = :status WHERE id = :id") suspend fun updatePayment(id: String, paid: Long, status: String)
    @Query("DELETE FROM invoices") suspend fun deleteAll()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC") fun observeAll(): Flow<List<PaymentEntity>>
    @Query("SELECT * FROM payments ORDER BY paymentDate") suspend fun getAll(): List<PaymentEntity>
    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE invoiceId = :invoiceId") suspend fun totalForInvoice(invoiceId: String): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(item: PaymentEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<PaymentEntity>)
    @Query("DELETE FROM payments") suspend fun deleteAll()
}

@Dao
interface ExpenseCategoryDao {
    @Query("SELECT * FROM expense_categories ORDER BY name") fun observeAll(): Flow<List<ExpenseCategoryEntity>>
    @Query("SELECT * FROM expense_categories ORDER BY name") suspend fun getAll(): List<ExpenseCategoryEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(items: List<ExpenseCategoryEntity>)
    @Query("DELETE FROM expense_categories") suspend fun deleteAll()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC") fun observeAll(): Flow<List<ExpenseEntity>>
    @Query("SELECT * FROM expenses ORDER BY expenseDate") suspend fun getAll(): List<ExpenseEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: ExpenseEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(items: List<ExpenseEntity>)
    @Delete suspend fun delete(item: ExpenseEntity)
    @Query("DELETE FROM expenses") suspend fun deleteAll()
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings WHERE id = 1") fun observe(): Flow<AppSettingEntity?>
    @Query("SELECT * FROM app_settings WHERE id = 1") suspend fun get(): AppSettingEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: AppSettingEntity)
    @Query("DELETE FROM app_settings") suspend fun deleteAll()
}
