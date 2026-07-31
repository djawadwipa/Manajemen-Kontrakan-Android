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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import id.djawadwipa.manajemenkontrakan.data.local.InvoiceEntity
import id.djawadwipa.manajemenkontrakan.data.local.PaymentEntity
import id.djawadwipa.manajemenkontrakan.ui.components.LabelValue
import id.djawadwipa.manajemenkontrakan.ui.components.ScreenHeader
import id.djawadwipa.manajemenkontrakan.util.toDateLabel
import id.djawadwipa.manajemenkontrakan.util.toRupiah
import java.time.LocalDate

@Composable
fun PaymentHistoryScreen(
    invoice: InvoiceEntity,
    payments: List<PaymentEntity>,
    onBack: () -> Unit,
    onUpdate: (PaymentEntity) -> Unit,
    onCancel: (PaymentEntity) -> Unit,
    onDelete: (PaymentEntity) -> Unit,
) {
    var editing by remember { mutableStateOf<PaymentEntity?>(null) }
    var canceling by remember { mutableStateOf<PaymentEntity?>(null) }
    var deleting by remember { mutableStateOf<PaymentEntity?>(null) }

    val orderedPayments = payments.sortedWith(
        compareByDescending<PaymentEntity> { it.paymentDate }
            .thenByDescending { it.installmentNumber },
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
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
                    title = "Riwayat Pembayaran",
                    subtitle = "${invoice.unitId} • ${invoice.tenantName} • ${invoice.period}",
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
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LabelValue("Tagihan", invoice.amount.toRupiah())
                    LabelValue("Pembayaran aktif", invoice.paid.toRupiah())
                    LabelValue(
                        "Sisa",
                        (invoice.amount - invoice.paid)
                            .coerceAtLeast(0)
                            .toRupiah(),
                    )
                    LabelValue("Status tagihan", invoice.status)
                }
            }
        }

        if (orderedPayments.isEmpty()) {
            item {
                Text(
                    text = "Belum ada pembayaran untuk tagihan ini.",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(
            items = orderedPayments,
            key = { it.id },
        ) { payment ->
            PaymentHistoryCard(
                payment = payment,
                onEdit = { editing = payment },
                onCancel = { canceling = payment },
                onDelete = { deleting = payment },
            )
        }
    }

    editing?.let { payment ->
        PaymentEditDialog(
            payment = payment,
            invoiceAmount = invoice.amount,
            otherActivePayments = payments
                .filter { it.id != payment.id && it.status == "AKTIF" }
                .sumOf { it.amount },
            onDismiss = { editing = null },
            onSave = {
                onUpdate(it)
                editing = null
            },
        )
    }

    canceling?.let { payment ->
        ConfirmationDialog(
            title = "Batalkan pembayaran?",
            message = buildString {
                append("Pembayaran cicilan ke-${payment.installmentNumber} ")
                append("senilai ${payment.amount.toRupiah()} akan tetap tersimpan ")
                append("sebagai riwayat, tetapi tidak dihitung ke tagihan.")
            },
            confirmLabel = "Batalkan pembayaran",
            onDismiss = { canceling = null },
            onConfirm = {
                onCancel(payment)
                canceling = null
            },
        )
    }

    deleting?.let { payment ->
        ConfirmationDialog(
            title = "Hapus pembayaran permanen?",
            message = buildString {
                append("Pembayaran cicilan ke-${payment.installmentNumber} ")
                append("senilai ${payment.amount.toRupiah()} akan dihapus permanen. ")
                append("Tagihan akan dihitung ulang.")
            },
            confirmLabel = "Hapus permanen",
            onDismiss = { deleting = null },
            onConfirm = {
                onDelete(payment)
                deleting = null
            },
        )
    }
}

@Composable
private fun PaymentHistoryCard(
    payment: PaymentEntity,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val isCanceled = payment.status == "DIBATALKAN"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCanceled) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
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
                        text = "Cicilan ke-${payment.installmentNumber}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = payment.paymentDate.toDateLabel(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = payment.status,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isCanceled) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }

            LabelValue("Nominal", payment.amount.toRupiah())
            LabelValue("Metode", payment.method)
            LabelValue(
                "Nomor bukti",
                payment.receiptNumber.ifBlank { "-" },
            )
            LabelValue(
                "Catatan",
                payment.note.ifBlank { "-" },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (!isCanceled) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit pembayaran",
                        )
                    }
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Batalkan pembayaran",
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus pembayaran",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentEditDialog(
    payment: PaymentEntity,
    invoiceAmount: Long,
    otherActivePayments: Long,
    onDismiss: () -> Unit,
    onSave: (PaymentEntity) -> Unit,
) {
    var paymentDate by remember(payment.id) {
        mutableStateOf(LocalDate.ofEpochDay(payment.paymentDate).toString())
    }
    var amount by remember(payment.id) {
        mutableStateOf(payment.amount.toString())
    }
    var method by remember(payment.id) {
        mutableStateOf(payment.method)
    }
    var receipt by remember(payment.id) {
        mutableStateOf(payment.receiptNumber)
    }
    var note by remember(payment.id) {
        mutableStateOf(payment.note)
    }

    val parsedDate = runCatching {
        LocalDate.parse(paymentDate).toEpochDay()
    }.getOrNull()
    val parsedAmount = amount.toLongOrNull() ?: 0L
    val maximumAmount = (invoiceAmount - otherActivePayments)
        .coerceAtLeast(0L)
    val isValid = parsedDate != null &&
        parsedAmount in 1..maximumAmount

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text("Edit pembayaran") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        text = "Cicilan ke-${payment.installmentNumber}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                item {
                    OutlinedTextField(
                        value = paymentDate,
                        onValueChange = { paymentDate = it },
                        label = { Text("Tanggal pembayaran") },
                        supportingText = { Text("Format: yyyy-MM-dd") },
                        isError = parsedDate == null,
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
                        label = { Text("Nominal") },
                        supportingText = {
                            Text("Maksimal ${maximumAmount.toRupiah()}")
                        },
                        isError = parsedAmount !in 1..maximumAmount,
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
                enabled = isValid,
                onClick = {
                    onSave(
                        payment.copy(
                            paymentDate = requireNotNull(parsedDate),
                            amount = parsedAmount,
                            method = method,
                            receiptNumber = receipt.trim(),
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
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Kembali")
            }
        },
    )
}
