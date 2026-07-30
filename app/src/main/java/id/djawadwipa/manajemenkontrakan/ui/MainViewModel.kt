package id.djawadwipa.manajemenkontrakan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.djawadwipa.manajemenkontrakan.data.local.AppSettingEntity
import id.djawadwipa.manajemenkontrakan.data.local.ExpenseCategoryEntity
import id.djawadwipa.manajemenkontrakan.data.local.ExpenseEntity
import id.djawadwipa.manajemenkontrakan.data.local.InvoiceEntity
import id.djawadwipa.manajemenkontrakan.data.local.PaymentEntity
import id.djawadwipa.manajemenkontrakan.data.local.RentalUnitEntity
import id.djawadwipa.manajemenkontrakan.data.repository.RentalRepository
import id.djawadwipa.manajemenkontrakan.util.BackupCrypto
import id.djawadwipa.manajemenkontrakan.util.CsvExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val repository: RentalRepository) : ViewModel() {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    private val _pendingBackup = MutableStateFlow<ByteArray?>(null)
    val pendingBackup: StateFlow<ByteArray?> = _pendingBackup
    private val _pendingCsv = MutableStateFlow<ByteArray?>(null)
    val pendingCsv: StateFlow<ByteArray?> = _pendingCsv

    val state: StateFlow<MainUiState> = combine(
        repository.units,
        repository.invoices,
        repository.payments,
        repository.expenses,
        repository.categories,
        repository.settings,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        MainUiState(
            units = values[0] as List<RentalUnitEntity>,
            invoices = refreshStatuses(values[1] as List<InvoiceEntity>),
            payments = values[2] as List<PaymentEntity>,
            expenses = values[3] as List<ExpenseEntity>,
            categories = values[4] as List<ExpenseCategoryEntity>,
            settings = values[5] as AppSettingEntity? ?: AppSettingEntity(),
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            runCatching { repository.seedIfEmpty() }
                .onFailure { _message.value = "Gagal menyiapkan database: ${it.message}" }
        }
    }

    fun clearMessage() { _message.value = null }
    fun clearPendingBackup() { _pendingBackup.value = null }
    fun clearPendingCsv() { _pendingCsv.value = null }

    fun saveUnit(unit: RentalUnitEntity) = launchAction("Unit disimpan") { repository.upsertUnit(unit) }
    fun deleteUnit(unit: RentalUnitEntity) = launchAction("Unit dihapus") { repository.deleteUnit(unit) }
    fun saveExpense(expense: ExpenseEntity) = launchAction("Pengeluaran disimpan") { repository.upsertExpense(expense) }
    fun deleteExpense(expense: ExpenseEntity) = launchAction("Pengeluaran dihapus") { repository.deleteExpense(expense) }
    fun updateSettings(setting: AppSettingEntity) = launchAction("Pengaturan disimpan") { repository.updateSettings(setting) }
    fun regenerateInvoices(year: Int) = launchAction("Tagihan tahun $year dibuat") { repository.regenerateInvoices(year) }

    fun recordPayment(invoice: InvoiceEntity, amount: Long, method: String, receipt: String, note: String) =
        launchAction("Pembayaran tercatat") {
            val installment = state.value.payments.count { it.invoiceId == invoice.id } + 1
            repository.recordPayment(
                PaymentEntity(
                    id = "PAY-${UUID.randomUUID()}",
                    invoiceId = invoice.id,
                    unitId = invoice.unitId,
                    tenantName = invoice.tenantName,
                    period = invoice.period,
                    paymentDate = LocalDate.now().toEpochDay(),
                    installmentNumber = installment,
                    amount = amount,
                    method = method,
                    receiptNumber = receipt,
                    note = note,
                ),
            )
        }

    fun prepareBackup(password: String) = launchAction(null) {
        _pendingBackup.value = BackupCrypto.encrypt(repository.exportPayload(), password.toCharArray())
    }

    fun restoreBackup(bytes: ByteArray, password: String) = launchAction("Backup berhasil dipulihkan") {
        val payload = BackupCrypto.decrypt(bytes, password.toCharArray())
        repository.restorePayload(payload)
    }

    fun prepareCsvReport() = viewModelScope.launch {
        val s = state.value
        _pendingCsv.value = CsvExporter.report(s.units, s.invoices, s.payments, s.expenses).encodeToByteArray()
    }

    fun importUnitsCsv(text: String) = launchAction("Data unit berhasil diimpor") {
        CsvExporter.parseUnits(text).forEach { repository.upsertUnit(it) }
    }

    private fun launchAction(success: String?, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { if (success != null) _message.value = success }
                .onFailure { _message.value = it.message ?: "Terjadi kesalahan." }
        }
    }

    private fun refreshStatuses(items: List<InvoiceEntity>): List<InvoiceEntity> {
        val today = LocalDate.now().toEpochDay()
        return items.map { invoice ->
            val status = when {
                invoice.paid >= invoice.amount -> "LUNAS"
                invoice.paid > 0 -> "CICILAN"
                invoice.dueDate < today -> "MENUNGGAK"
                else -> "MENUNGGU"
            }
            if (status == invoice.status) invoice else invoice.copy(status = status)
        }
    }
}

data class MainUiState(
    val units: List<RentalUnitEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val categories: List<ExpenseCategoryEntity> = emptyList(),
    val settings: AppSettingEntity = AppSettingEntity(),
    val isLoading: Boolean = true,
) {
    val activeUnits get() = units.count { it.status == "Aktif" }
    val emptyUnits get() = units.count { it.status == "Kosong" }
    val totalBilled get() = invoices.sumOf { it.amount }
    val totalReceived get() = payments.sumOf { it.amount }
    val receivable get() = invoices.sumOf { (it.amount - it.paid).coerceAtLeast(0) }
    val totalExpenses get() = expenses.sumOf { it.amount }
    val businessExpenses get() = expenses.filter { it.includeInProfitLoss }.sumOf { it.amount }
    val reserveFund get() = (totalReceived * settings.reservePercent).toLong() - expenses.filter { it.category == "Perbaikan & Renovasi" }.sumOf { it.amount }
    val collectionRate get() = if (totalBilled == 0L) 0.0 else totalReceived.toDouble() / totalBilled
    val occupancyRate get() = if (units.isEmpty()) 0.0 else activeUnits.toDouble() / units.size
    val cashBalance get() = settings.openingCash + totalReceived - totalExpenses
    val dashboardPeriod: String
        get() = String.format(Locale.ROOT, "%04d-%02d", settings.activeYear, settings.dashboardMonth.coerceIn(1, 12))
    val dashboardInvoices get() = invoices.filter { it.period == dashboardPeriod }
    val dashboardBilled get() = dashboardInvoices.sumOf { it.amount }
    val dashboardReceived get() = payments.filter { it.period == dashboardPeriod }.sumOf { it.amount }
    val dashboardExpenses get() = expenses.filter { it.period == dashboardPeriod }.sumOf { it.amount }
    val dashboardReceivable get() = dashboardInvoices.sumOf { (it.amount - it.paid).coerceAtLeast(0) }
    val dashboardCollectionRate get() = if (dashboardBilled == 0L) 0.0 else dashboardReceived.toDouble() / dashboardBilled
    val dashboardOverdue get() = dashboardInvoices.count { it.status == "MENUNGGAK" }
    val monthlySummary: List<MonthlySummary> get() = (1..12).map { month ->
        val period = String.format(Locale.ROOT, "%04d-%02d", settings.activeYear, month)
        val bills = invoices.filter { it.period == period }
        val pay = payments.filter { it.period == period }.sumOf { it.amount }
        val exp = expenses.filter { it.period == period }.sumOf { it.amount }
        MonthlySummary(YearMonth.of(settings.activeYear, month), bills.sumOf { it.amount }, pay, exp)
    }
}

data class MonthlySummary(val month: YearMonth, val billed: Long, val received: Long, val expense: Long)
