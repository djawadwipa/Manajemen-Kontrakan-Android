package id.djawadwipa.manajemenkontrakan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import id.djawadwipa.manajemenkontrakan.data.local.InvoiceEntity
import id.djawadwipa.manajemenkontrakan.data.local.PaymentEntity
import id.djawadwipa.manajemenkontrakan.ui.components.LabelValue
import id.djawadwipa.manajemenkontrakan.ui.components.ScreenHeader
import id.djawadwipa.manajemenkontrakan.util.toDateLabel
import id.djawadwipa.manajemenkontrakan.util.toRupiah
import java.time.LocalDate

@Composable
fun InvoiceDetailScreen(
    invoice: InvoiceEntity,
    payments: List<PaymentEntity>,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onUpdate: (InvoiceEntity) -> Unit,
    onDelete: (InvoiceEntity) -> Unit,
) {
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val activePayments = payments.filter { it.status == "AKTIF" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                    )
                }
                ScreenHeader(
                    title = "Detail Tagihan",
                    subtitle = "${invoice.unitId} • ${invoice.tenantName}",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LabelValue("Periode", invoice.period)
                    LabelValue(
                        "Tanggal dibuat",
                        invoice.invoiceDate.toDateLabel(),
                    )
                    LabelValue(
                        "Jatuh tempo",
                        invoice.dueDate.toDateLabel(),
                    )
                    LabelValue("Nominal", invoice.amount.toRupiah())
                    LabelValue("Dibayar", invoice.paid.toRupiah())
                    LabelValue(
                        "Sisa",
                        (invoice.amount - invoice.paid)
                            .coerceAtLeast(0L)
                            .toRupiah(),
                    )
                    LabelValue(
                        "Target dana cadangan",
                        invoice.reserveTarget.toRupiah(),
                    )
                    LabelValue("Status", invoice.status)
                    LabelValue(
                        "Catatan",
                        invoice.note.ifBlank { "-" },
                    )
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
                Button(
                    onClick = onOpenHistory,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                    )
                    Text(
                        text = "Riwayat pembayaran (${payments.size})",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                OutlinedButton(
                    onClick = { showEdit = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                    )
                    Text(
                        text = "Edit tagihan",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                OutlinedButton(
                    onClick = { showDelete = true },
                    enabled = payments.isEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                    )
                    Text(
                        text = "Hapus tagihan",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                if (payments.isNotEmpty()) {
                    Text(
                        text = "Tagihan tidak dapat dihapus karena memiliki riwayat pembayaran.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showEdit) {
        InvoiceEditDialog(
            invoice = invoice,
            activePaymentTotal = activePayments.sumOf { it.amount },
            onDismiss = { showEdit = false },
            onSave = {
                onUpdate(it)
                showEdit = false
            },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Hapus tagihan?") },
            text = {
                Text(
                    "Tagihan ${invoice.unitId} periode ${invoice.period} akan dihapus permanen.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(invoice)
                        showDelete = false
                        onBack()
                    },
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Batal")
                }
            },
        )
    }
}

@Composable
private fun InvoiceEditDialog(
    invoice: InvoiceEntity,
    activePaymentTotal: Long,
    onDismiss: () -> Unit,
    onSave: (InvoiceEntity) -> Unit,
) {
    var invoiceDate by remember(invoice.id) {
        mutableStateOf(
            LocalDate.ofEpochDay(invoice.invoiceDate).toString(),
        )
    }
    var dueDate by remember(invoice.id) {
        mutableStateOf(
            LocalDate.ofEpochDay(invoice.dueDate).toString(),
        )
    }
    var amount by remember(invoice.id) {
        mutableStateOf(invoice.amount.toString())
    }
    var reserveTarget by remember(invoice.id) {
        mutableStateOf(invoice.reserveTarget.toString())
    }
    var note by remember(invoice.id) {
        mutableStateOf(invoice.note)
    }

    val parsedInvoiceDate = runCatching {
        LocalDate.parse(invoiceDate)
    }.getOrNull()
    val parsedDueDate = runCatching {
        LocalDate.parse(dueDate)
    }.getOrNull()
    val parsedAmount = amount.toLongOrNull() ?: 0L
    val parsedReserve = reserveTarget.toLongOrNull() ?: -1L
    val valid = parsedInvoiceDate != null &&
        parsedDueDate != null &&
        parsedDueDate >= parsedInvoiceDate &&
        parsedAmount >= activePaymentTotal &&
        parsedAmount > 0L &&
        parsedReserve in 0L..parsedAmount

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text("Edit tagihan") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        text = "${invoice.unitId} • ${invoice.period}",
                        style = MaterialTheme.typography.titleMedium,
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
                        supportingText = {
                            Text(
                                "Minimal pembayaran aktif ${activePaymentTotal.toRupiah()}",
                            )
                        },
                        isError = parsedAmount < activePaymentTotal ||
                            parsedAmount <= 0L,
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
                    onSave(
                        invoice.copy(
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
