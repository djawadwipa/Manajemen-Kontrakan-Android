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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
fun ExpensesScreen(
    state: MainUiState,
    onSave: (ExpenseEntity) -> Unit,
    onDelete: (ExpenseEntity) -> Unit,
    onOpenCategories: () -> Unit,
) {
    var editing by remember {
        mutableStateOf<ExpenseEntity?>(null)
    }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showDialog = true
                },
            ) {
                Icon(Icons.Default.Add, "Tambah pengeluaran")
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Column {
                    ScreenHeader(
                        "Pengeluaran",
                        "Catat, periksa, dan edit seluruh biaya",
                    )
                    OutlinedButton(
                        onClick = onOpenCategories,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text("Kelola kategori pengeluaran")
                    }
                }
            }

            items(state.expenses, key = { it.id }) { expense ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable {
                            editing = expense
                            showDialog = true
                        },
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    expense.description,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    "${expense.category} • ${expense.unitName}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        editing = expense
                                        showDialog = true
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        "Edit pengeluaran",
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(expense) },
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Hapus pengeluaran",
                                    )
                                }
                            }
                        }

                        LabelValue("Nominal", expense.amount.toRupiah())
                        LabelValue("Tanggal", expense.expenseDate.toDateLabel())
                        LabelValue("Kelompok", expense.groupName)
                        LabelValue("Metode", expense.method)
                        LabelValue(
                            "Nomor bukti",
                            expense.receiptNumber.ifBlank { "-" },
                        )
                        LabelValue(
                            "Catatan",
                            expense.note.ifBlank { "-" },
                        )
                        LabelValue(
                            "Laporan laba-rugi",
                            if (expense.includeInProfitLoss) {
                                "Masuk"
                            } else {
                                "Tidak masuk"
                            },
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        ExpenseDialog(
            existing = editing,
            units = state.units,
            categories = state.categories,
            onDismiss = { showDialog = false },
            onSave = {
                onSave(it)
                showDialog = false
            },
        )
    }
}

@Composable
private fun ExpenseDialog(
    existing: ExpenseEntity?,
    units: List<RentalUnitEntity>,
    categories: List<ExpenseCategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (ExpenseEntity) -> Unit,
) {
    val initialCategory = existing?.let { expense ->
        categories.firstOrNull { it.name == expense.category }
    } ?: categories.firstOrNull()
    val initialUnit = existing?.unitId?.let { unitId ->
        units.firstOrNull { it.id == unitId }
    }

    var expenseDate by remember(existing?.id) {
        mutableStateOf(
            existing?.expenseDate
                ?.let { LocalDate.ofEpochDay(it).toString() }
                ?: LocalDate.now().toString(),
        )
    }
    var description by remember(existing?.id) {
        mutableStateOf(existing?.description.orEmpty())
    }
    var amount by remember(existing?.id) {
        mutableStateOf(existing?.amount?.toString().orEmpty())
    }
    var method by remember(existing?.id) {
        mutableStateOf(existing?.method ?: "Tunai")
    }
    var receipt by remember(existing?.id) {
        mutableStateOf(existing?.receiptNumber.orEmpty())
    }
    var note by remember(existing?.id) {
        mutableStateOf(existing?.note.orEmpty())
    }
    var category by remember(existing?.id, categories) {
        mutableStateOf(initialCategory)
    }
    var unit by remember(existing?.id, units) {
        mutableStateOf(initialUnit)
    }
    var include by remember(existing?.id) {
        mutableStateOf(
            existing?.includeInProfitLoss
                ?: (initialCategory?.profitLossRule == "Ya"),
        )
    }
    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    val parsedDate = runCatching {
        LocalDate.parse(expenseDate)
    }.getOrNull()
    val parsedAmount = amount.toLongOrNull()
    val valid = description.isNotBlank() &&
        parsedAmount != null &&
        parsedAmount > 0L &&
        parsedDate != null &&
        category != null

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) {
                    "Tambah pengeluaran"
                } else {
                    "Edit pengeluaran"
                },
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = expenseDate,
                        onValueChange = { expenseDate = it },
                        label = { Text("Tanggal pengeluaran") },
                        supportingText = { Text("Format: yyyy-MM-dd") },
                        isError = parsedDate == null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Uraian") },
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
                        isError = parsedAmount == null || parsedAmount <= 0L,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { categoryExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(category?.name ?: "Pilih kategori")
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                            )
                        }
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = {
                                categoryExpanded = false
                            },
                        ) {
                            categories.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${item.name} • ${item.groupName}",
                                        )
                                    },
                                    onClick = {
                                        category = item
                                        include = item.profitLossRule == "Ya"
                                        categoryExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { unitExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Unit: ${unit?.name ?: "Umum"}")
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                            )
                        }
                        DropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = {
                                unitExpanded = false
                            },
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
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = include,
                            onCheckedChange = { include = it },
                        )
                        Text(
                            "Masuk laporan laba-rugi usaha",
                            Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    val selectedDate = requireNotNull(parsedDate)
                    val selectedCategory = requireNotNull(category)
                    onSave(
                        ExpenseEntity(
                            id = existing?.id
                                ?: "EXP-${UUID.randomUUID()}",
                            expenseDate = selectedDate.toEpochDay(),
                            period = String.format(
                                Locale.ROOT,
                                "%04d-%02d",
                                selectedDate.year,
                                selectedDate.monthValue,
                            ),
                            unitId = unit?.id,
                            unitName = unit?.name ?: "Umum",
                            category = selectedCategory.name,
                            groupName = selectedCategory.groupName,
                            description = description.trim(),
                            amount = requireNotNull(parsedAmount),
                            method = method,
                            receiptNumber = receipt.trim(),
                            includeInProfitLoss = include,
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
