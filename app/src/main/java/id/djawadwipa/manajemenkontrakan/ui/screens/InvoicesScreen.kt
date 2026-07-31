package id.djawadwipa.manajemenkontrakan.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.djawadwipa.manajemenkontrakan.data.local.AppSettingEntity
import id.djawadwipa.manajemenkontrakan.data.local.InvoiceEntity
import id.djawadwipa.manajemenkontrakan.data.local.RentalUnitEntity
import id.djawadwipa.manajemenkontrakan.ui.components.LabelValue
import id.djawadwipa.manajemenkontrakan.ui.components.ScreenHeader
import id.djawadwipa.manajemenkontrakan.util.toDateLabel
import id.djawadwipa.manajemenkontrakan.util.toRupiah
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.math.roundToLong

@Composable
fun InvoicesScreen(
    invoices: List<InvoiceEntity>,
    units: List<RentalUnitEntity>,
    settings: AppSettingEntity,
    onPayment: (InvoiceEntity, Long, String, String, String) -> Unit,
    onRegenerate: (Int) -> Unit,
    onCreateInvoice: (InvoiceEntity) -> Unit,
    onOpenDetail: (InvoiceEntity) -> Unit,
) {
    var filter by remember { mutableStateOf("SEMUA") }
    var paymentInvoice by remember {
        mutableStateOf<InvoiceEntity?>(null)
    }
    var showManualInvoice by remember { mutableStateOf(false) }

    val filtered = invoices.filter {
        filter == "SEMUA" || it.status == filter
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ScreenHeader(
                    title = "Tagihan",
                    subtitle = "Otomatis dan manual, lengkap dengan riwayat pembayaran",
                    modifier = Modifier.weight(1f),
                )
                Row(
                    modifier = Modifier.padding(top = 14.dp, end = 8.dp),
                ) {
                    IconButton(onClick = { showManualInvoice = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Buat tagihan manual",
                        )
                    }
                    IconButton(
                        onClick = {
                            onRegenerate(settings.activeYear)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Buat tagihan otomatis",
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "SEMUA",
                    "MENUNGGU",
                    "MENUNGGAK",
                    "CICILAN",
                    "LUNAS",
                ).chunked(2).forEach { filterRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        filterRow.forEach { item ->
                            FilterChip(
                                selected = filter == item,
                                onClick = { filter = item },
                                label = { Text(item, maxLines = 1) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        items(filtered, key = { it.id }) { invoice ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { onOpenDetail(invoice) },
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "${invoice.unitId} • ${invoice.period}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = invoice.tenantName,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (invoice.status != "LUNAS") {
                            IconButton(
                                onClick = {
                                    paymentInvoice = invoice
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCard,
                                    contentDescription = "Catat pembayaran",
                                )
                            }
                        }
                    }
                    LabelValue("Tagihan", invoice.amount.toRupiah())
                    LabelValue("Dibayar", invoice.paid.toRupiah())
                    LabelValue(
                        "Sisa",
                        (invoice.amount - invoice.paid)
                            .coerceAtLeast(0)
                            .toRupiah(),
                    )
                    LabelValue(
                        "Tanggal dibuat",
                        invoice.invoiceDate.toDateLabel(),
                    )
                    LabelValue(
                        "Jatuh tempo",
                        invoice.dueDate.toDateLabel(),
                    )
                    LabelValue(
                        "Target cadangan",
                        invoice.reserveTarget.toRupiah(),
                    )
                    LabelValue("Status", invoice.status)
                }
            }
        }
    }

    paymentInvoice?.let { invoice ->
        PaymentDialog(
            invoice = invoice,
            onDismiss = { paymentInvoice = null },
            onSave = { amount, method, receipt, note ->
                onPayment(invoice, amount, method, receipt, note)
                paymentInvoice = null
            },
        )
    }

    if (showManualInvoice) {
        ManualInvoiceDialog(
            units = units,
            invoices = invoices,
            onDismiss = { showManualInvoice = false },
            onSave = {
                onCreateInvoice(it)
                showManualInvoice = false
            },
        )
    }
}

@Composable
private fun ManualInvoiceDialog(
    units: List<RentalUnitEntity>,
    invoices: List<InvoiceEntity>,
    onDismiss: () -> Unit,
    onSave: (InvoiceEntity) -> Unit,
) {
    val availableUnits = units.filter { it.status == "Aktif" }
    var selectedUnitId by remember(availableUnits) {
        mutableStateOf(availableUnits.firstOrNull()?.id.orEmpty())
    }
    var expanded by remember { mutableStateOf(false) }
    var period by remember {
        mutableStateOf(YearMonth.now().toString())
    }
    var invoiceDate by remember {
        mutableStateOf(LocalDate.now().toString())
    }
    var dueDate by remember {
        mutableStateOf(
            availableUnits.firstOrNull()?.let {
                defaultDueDate(YearMonth.now(), it.dueDay)
            } ?: LocalDate.now().toString(),
        )
    }
    var amount by remember(availableUnits) {
        mutableStateOf(
            availableUnits.firstOrNull()?.rate?.toString().orEmpty(),
        )
    }
    var reserveTarget by remember(availableUnits) {
        mutableStateOf(
            availableUnits.firstOrNull()?.let {
                (it.rate * it.reservePercent)
                    .roundToLong()
                    .toString()
            }.orEmpty(),
        )
    }
    var note by remember { mutableStateOf("") }

    val selectedUnit = availableUnits.firstOrNull {
        it.id == selectedUnitId
    }
    val parsedPeriod = runCatching {
        YearMonth.parse(period)
    }.getOrNull()
    val parsedInvoiceDate = runCatching {
        LocalDate.parse(invoiceDate)
    }.getOrNull()
    val parsedDueDate = runCatching {
        LocalDate.parse(dueDate)
    }.getOrNull()
    val parsedAmount = amount.toLongOrNull() ?: 0L
    val parsedReserve = reserveTarget.toLongOrNull() ?: -1L
    val duplicate = selectedUnit != null && parsedPeriod != null &&
        invoices.any {
            it.unitId == selectedUnit.id &&
                it.period == parsedPeriod.toString()
        }
    val valid = selectedUnit != null &&
        parsedPeriod != null &&
        parsedInvoiceDate != null &&
        parsedDueDate != null &&
        parsedDueDate >= parsedInvoiceDate &&
        parsedAmount > 0L &&
        parsedReserve in 0L..parsedAmount &&
        !duplicate

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text("Buat tagihan manual") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                selectedUnit?.let {
                                    "${it.code} • ${it.name}"
                                } ?: "Pilih unit aktif",
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            availableUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${unit.code} • ${unit.name}")
                                    },
                                    onClick = {
                                        selectedUnitId = unit.id
                                        amount = unit.rate.toString()
                                        reserveTarget =
                                            (unit.rate * unit.reservePercent)
                                                .roundToLong()
                                                .toString()
                                        parsedPeriod?.let {
                                            dueDate = defaultDueDate(
                                                it,
                                                unit.dueDay,
                                            )
                                        }
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = period,
                        onValueChange = { period = it },
                        label = { Text("Periode") },
                        supportingText = {
                            Text(
                                when {
                                    duplicate -> "Tagihan periode ini sudah ada"
                                    else -> "Format: yyyy-MM"
                                },
                            )
                        },
                        isError = parsedPeriod == null || duplicate,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    OutlinedTextField(
                        value = invoiceDate,
                        onValueChange = { invoiceDate = it },
                        label = { Text("Tanggal dibuat") },
                        supportingText = { Text("Format: yyyy-MM-dd") },
                        isError = parsedInvoiceDate == null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Tanggal jatuh tempo") },
                        supportingText = { Text("Format: yyyy-MM-dd") },
                        isError = parsedDueDate == null ||
                            (parsedInvoiceDate != null &&
                                parsedDueDate != null &&
                                parsedDueDate < parsedInvoiceDate),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = {
                            amount = it.filter(Char::isDigit)
                        },
                        label = { Text("Nominal tagihan") },
                        isError = parsedAmount <= 0L,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    OutlinedTextField(
                        value = reserveTarget,
                        onValueChange = {
                            reserveTarget = it.filter(Char::isDigit)
                        },
                        label = { Text("Target dana cadangan") },
                        supportingText = {
                            Text("Maksimal sebesar nominal tagihan")
                        },
                        isError = parsedReserve !in 0L..parsedAmount,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    val unit = requireNotNull(selectedUnit)
                    onSave(
                        InvoiceEntity(
                            id = "INV-MANUAL-${UUID.randomUUID()}",
                            unitId = unit.id,
                            tenantName = unit.tenantName,
                            period = requireNotNull(parsedPeriod).toString(),
                            invoiceDate = requireNotNull(parsedInvoiceDate)
                                .toEpochDay(),
                            dueDate = requireNotNull(parsedDueDate)
                                .toEpochDay(),
                            amount = parsedAmount,
                            reserveTarget = parsedReserve,
                            note = note.trim(),
                        ),
                    )
                },
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    )
}

@Composable
private fun PaymentDialog(
    invoice: InvoiceEntity,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String) -> Unit,
) {
    val remaining = (invoice.amount - invoice.paid).coerceAtLeast(0)
    var amount by remember { mutableStateOf(remaining.toString()) }
    var method by remember { mutableStateOf("Transfer") }
    var receipt by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val amountValue = amount.toLongOrNull() ?: 0

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text("Catat pembayaran") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        "${invoice.unitId} • ${invoice.tenantName} • ${invoice.period}",
                    )
                }
                item {
                    Text(
                        text = "Sisa ${remaining.toRupiah()}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = {
                            amount = it.filter(Char::isDigit)
                        },
                        label = { Text("Nominal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Metode pembayaran",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("Tunai", "Transfer").forEach { option ->
                                FilterChip(
                                    selected = method == option,
                                    onClick = { method = option },
                                    label = { Text(option) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = receipt,
                        onValueChange = { receipt = it },
                        label = { Text("Nomor bukti") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = amountValue in 1..remaining,
                onClick = {
                    onSave(amountValue, method, receipt, note)
                },
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    )
}

private fun defaultDueDate(
    period: YearMonth,
    dueDay: Int,
): String = period.atDay(
    dueDay.coerceIn(1, period.lengthOfMonth()),
).toString()
