package id.djawadwipa.manajemenkontrakan.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "rental_units", indices = [Index(value = ["code"], unique = true)])
data class RentalUnitEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val tenantName: String,
    val frequency: String,
    val rate: Long,
    val intervalMonths: Int,
    val reservePercent: Double,
    val status: String,
    val dueDay: Int,
    val notes: String = "",
)

@Serializable
@Entity(
    tableName = "invoices",
    foreignKeys = [ForeignKey(
        entity = RentalUnitEntity::class,
        parentColumns = ["id"],
        childColumns = ["unitId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["unitId"]), Index(value = ["unitId", "period"], unique = true)],
)
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val tenantName: String,
    val period: String,
    val invoiceDate: Long,
    val dueDate: Long,
    val amount: Long,
    val reserveTarget: Long,
    val paid: Long = 0,
    val status: String = "MENUNGGU",
    val note: String = "",
)

@Serializable
@Entity(
    tableName = "payments",
    foreignKeys = [ForeignKey(
        entity = InvoiceEntity::class,
        parentColumns = ["id"],
        childColumns = ["invoiceId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["invoiceId"]), Index(value = ["unitId"])],
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val invoiceId: String,
    val unitId: String,
    val tenantName: String,
    val period: String,
    val paymentDate: Long,
    val installmentNumber: Int,
    val amount: Long,
    val method: String,
    val receiptNumber: String = "",
    val note: String = "",
    val status: String = "AKTIF",
    val canceledAt: Long? = null,
)

@Serializable
@Entity(tableName = "expense_categories", indices = [Index(value = ["name"], unique = true)])
data class ExpenseCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val groupName: String,
    val profitLossRule: String,
)

@Serializable
@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(
        entity = RentalUnitEntity::class,
        parentColumns = ["id"],
        childColumns = ["unitId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index(value = ["unitId"]), Index(value = ["period"])],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val expenseDate: Long,
    val period: String,
    val unitId: String?,
    val unitName: String,
    val category: String,
    val groupName: String,
    val description: String,
    val amount: Long,
    val method: String,
    val receiptNumber: String = "",
    val includeInProfitLoss: Boolean,
    val note: String = "",
)

@Serializable
@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val id: Int = 1,
    val activeYear: Int = 2026,
    val dashboardMonth: Int = 7,
    val openingCash: Long = 2_307_262,
    val reservePercent: Double = 0.15,
    val defaultDueDay: Int = 10,
    val bookStatus: String = "OPEN",
    val closedPeriods: String = "",
    val notificationEnabled: Boolean = false,
    val dueReminderDays: Int = 3,
    val notificationHour: Int = 8,
)
