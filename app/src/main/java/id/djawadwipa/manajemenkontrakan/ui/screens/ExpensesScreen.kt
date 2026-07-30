package id.djawadwipa.manajemenkontrakan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.djawadwipa.manajemenkontrakan.data.local.ExpenseCategoryEntity
import id.djawadwipa.manajemenkontrakan.data.local.ExpenseEntity
import id.djawadwipa.manajemenkontrakan.data.local.RentalUnitEntity
import id.djawadwipa.manajemenkontrakan.ui.MainUiState
import id.djawadwipa.manajemenkontrakan.ui.components.LabelValue
import id.djawadwipa.manajemenkontrakan.ui.components.ScreenHeader
import id.djawadwipa.manajemenkontrakan.util.toDateLabel
import id.djawadwipa.manajemenkontrakan.util.toRupiah
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

@Composable
fun ExpensesScreen(state: MainUiState, onSave: (ExpenseEntity) -> Unit, onDelete: (ExpenseEntity) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = { showDialog = true }) { Icon(Icons.Default.Add, "Tambah pengeluaran") } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { ScreenHeader("Pengeluaran", "Pisahkan biaya usaha, campuran, dan pribadi") }
            items(state.expenses, key = { it.id }) { expense ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text(expense.description, style = MaterialTheme.typography.titleMedium); Text("${expense.category} • ${expense.unitName}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            IconButton(onClick = { onDelete(expense) }) { Icon(Icons.Default.Delete, "Hapus") }
                        }
                        LabelValue("Nominal", expense.amount.toRupiah())
                        LabelValue("Tanggal", expense.expenseDate.toDateLabel())
                        LabelValue("Kelompok", expense.groupName)
                    }
                }
            }
        }
    }
    if (showDialog) ExpenseDialog(state.units, state.categories, { showDialog = false }) { onSave(it); showDialog = false }
}

@Composable
private fun ExpenseDialog(units: List<RentalUnitEntity>, categories: List<ExpenseCategoryEntity>, onDismiss: () -> Unit, onSave: (ExpenseEntity) -> Unit) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Tunai") }
    var receipt by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.firstOrNull()) }
    var unit by remember { mutableStateOf<RentalUnitEntity?>(null) }
    var include by remember { mutableStateOf(category?.groupName == "Usaha") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    val valid = description.isNotBlank() && amount.toLongOrNull()?.let { it > 0 } == true && category != null
    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text("Tambah pengeluaran") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { OutlinedTextField(description, { description = it }, label = { Text("Uraian") }) }
                item { OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Nominal") }, singleLine = true) }
                item {
                    Box {
                        OutlinedButton(
                            onClick = { categoryExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(category?.name ?: "Pilih kategori")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                        ) {
                            categories.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.name) },
                                    onClick = {
                                        category = item
                                        include = item.groupName == "Usaha"
                                        categoryExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    Box {
                        OutlinedButton(
                            onClick = { unitExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Unit: ${unit?.name ?: "Umum"}")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Umum") },
                                onClick = {
                                    unit = null
                                    unitExpanded = false
                                },
                            )
                            units.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.name) },
                                    onClick = {
                                        unit = item
                                        unitExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
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
                item { Row { Checkbox(include, { include = it }); Text("Masuk laporan laba-rugi usaha", Modifier.padding(top = 12.dp)) } }
            }
        },
        confirmButton = { Button(enabled = valid, onClick = {
            val now = LocalDate.now()
            onSave(ExpenseEntity("EXP-${UUID.randomUUID()}", now.toEpochDay(), String.format(Locale.ROOT, "%04d-%02d", now.year, now.monthValue), unit?.id, unit?.name ?: "Umum", category!!.name, category!!.groupName, description, amount.toLong(), method, receipt, include))
        }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
