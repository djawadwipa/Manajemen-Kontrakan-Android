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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import id.djawadwipa.manajemenkontrakan.ui.components.LabelValue
import id.djawadwipa.manajemenkontrakan.ui.components.ScreenHeader
import java.util.UUID

@Composable
fun ExpenseCategoriesScreen(
    categories: List<ExpenseCategoryEntity>,
    expenses: List<ExpenseEntity>,
    onBack: () -> Unit,
    onSave: (ExpenseCategoryEntity) -> Unit,
    onDelete: (ExpenseCategoryEntity) -> Unit,
) {
    var editing by remember {
        mutableStateOf<ExpenseCategoryEntity?>(null)
    }
    var showDialog by remember { mutableStateOf(false) }
    var deleting by remember {
        mutableStateOf<ExpenseCategoryEntity?>(null)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showDialog = true
                },
            ) {
                Icon(Icons.Default.Add, "Tambah kategori")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Kembali",
                        )
                    }
                    ScreenHeader(
                        title = "Kategori Pengeluaran",
                        subtitle = "Kelompok dan aturan laporan laba-rugi",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            items(categories, key = { it.id }) { category ->
                val usageCount = expenses.count {
                    it.category == category.name
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            editing = category
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
                                    category.name,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    if (usageCount == 0) {
                                        "Belum digunakan"
                                    } else {
                                        "Dipakai $usageCount pengeluaran"
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editing = category
                                        showDialog = true
                                    },
                                ) {
                                    Icon(Icons.Default.Edit, "Edit kategori")
                                }
                                IconButton(
                                    enabled = usageCount == 0,
                                    onClick = { deleting = category },
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Hapus kategori",
                                    )
                                }
                            }
                        }
                        LabelValue("Kelompok", category.groupName)
                        LabelValue(
                            "Aturan laba-rugi",
                            category.profitLossRule,
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        ExpenseCategoryDialog(
            existing = editing,
            onDismiss = { showDialog = false },
            onSave = {
                onSave(it)
                showDialog = false
            },
        )
    }

    deleting?.let { category ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Hapus kategori?") },
            text = {
                Text(
                    "Kategori ${category.name} akan dihapus permanen.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(category)
                        deleting = null
                    },
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text("Batal")
                }
            },
        )
    }
}

@Composable
private fun ExpenseCategoryDialog(
    existing: ExpenseCategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (ExpenseCategoryEntity) -> Unit,
) {
    val groups = listOf(
        "Usaha",
        "Campuran",
        "Pribadi",
        "Perlu Review",
    )
    val rules = listOf(
        "Ya",
        "Tidak",
        "Perlu review",
        "Perlu alokasi",
    )

    var name by remember(existing?.id) {
        mutableStateOf(existing?.name.orEmpty())
    }
    var group by remember(existing?.id) {
        mutableStateOf(existing?.groupName ?: groups.first())
    }
    var rule by remember(existing?.id) {
        mutableStateOf(existing?.profitLossRule ?: rules.first())
    }
    var groupExpanded by remember { mutableStateOf(false) }
    var ruleExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) {
                    "Tambah kategori"
                } else {
                    "Edit kategori"
                },
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama kategori") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { groupExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Kelompok: $group")
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                            )
                        }
                        DropdownMenu(
                            expanded = groupExpanded,
                            onDismissRequest = {
                                groupExpanded = false
                            },
                        ) {
                            groups.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        group = option
                                        groupExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { ruleExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Aturan laba-rugi: $rule")
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                            )
                        }
                        DropdownMenu(
                            expanded = ruleExpanded,
                            onDismissRequest = {
                                ruleExpanded = false
                            },
                        ) {
                            rules.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        rule = option
                                        ruleExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        ExpenseCategoryEntity(
                            id = existing?.id
                                ?: "CAT-${UUID.randomUUID()}",
                            name = name.trim(),
                            groupName = group,
                            profitLossRule = rule,
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
