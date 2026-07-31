package id.djawadwipa.manajemenkontrakan.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import id.djawadwipa.manajemenkontrakan.ui.components.LabelValue
import id.djawadwipa.manajemenkontrakan.ui.components.ScreenHeader
import id.djawadwipa.manajemenkontrakan.util.toDateLabel
import id.djawadwipa.manajemenkontrakan.util.toRupiah

@Composable
fun InvoicesScreen(
    invoices: List<InvoiceEntity>,
    settings: AppSettingEntity,
    onPayment: (InvoiceEntity, Long, String, String, String) -> Unit,
    onRegenerate: (Int) -> Unit,
    onOpenHistory: (InvoiceEntity) -> Unit,
) {
    var filter by remember { mutableStateOf("SEMUA") }
    var paymentInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }
    val filtered = invoices.filter { filter == "SEMUA" || it.status == filter }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ScreenHeader("Tagihan", "Tagihan otomatis bulanan, triwulanan, dan tahunan", Modifier.weight(1f))
                IconButton(onClick = { onRegenerate(settings.activeYear) }, modifier = Modifier.padding(top = 14.dp, end = 12.dp)) { Icon(Icons.Default.Refresh, "Buat tagihan") }
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("SEMUA", "MENUNGGAK", "CICILAN", "LUNAS")
                    .chunked(2)
                    .forEach { filterRow ->
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
                    .clickable {
                        onOpenHistory(invoice)
                    },
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${invoice.unitId} • ${invoice.period}", style = MaterialTheme.typography.titleMedium)
                            Text(invoice.tenantName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (invoice.status != "LUNAS") IconButton(onClick = { paymentInvoice = invoice }) { Icon(Icons.Default.AddCard, "Catat pembayaran") }
                    }
                    LabelValue("Tagihan", invoice.amount.toRupiah())
                    LabelValue("Dibayar", invoice.paid.toRupiah())
                    LabelValue("Sisa", (invoice.amount - invoice.paid).coerceAtLeast(0).toRupiah())
                    LabelValue("Jatuh tempo", invoice.dueDate.toDateLabel())
                    LabelValue("Status", invoice.status)
                }
            }
        }
    }
    paymentInvoice?.let { invoice -> PaymentDialog(invoice, { paymentInvoice = null }) { amount, method, receipt, note -> onPayment(invoice, amount, method, receipt, note); paymentInvoice = null } }
}

@Composable
private fun PaymentDialog(invoice: InvoiceEntity, onDismiss: () -> Unit, onSave: (Long, String, String, String) -> Unit) {
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
                item { Text("${invoice.unitId} • ${invoice.tenantName} • ${invoice.period}") }
                item { Text("Sisa ${remaining.toRupiah()}", style = MaterialTheme.typography.titleMedium) }
                item { OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Nominal") }, singleLine = true) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                item { OutlinedTextField(receipt, { receipt = it }, label = { Text("Nomor bukti") }, singleLine = true) }
                item { OutlinedTextField(note, { note = it }, label = { Text("Catatan") }) }
            }
        },
        confirmButton = { Button(enabled = amountValue in 1..remaining, onClick = { onSave(amountValue, method, receipt, note) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
