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
import id.djawadwipa.manajemenkontrakan.data.local.RentalUnitEntity
import id.djawadwipa.manajemenkontrakan.ui.components.LabelValue
import id.djawadwipa.manajemenkontrakan.ui.components.ScreenHeader
import id.djawadwipa.manajemenkontrakan.util.toRupiah
import java.util.Locale
import java.util.UUID

@Composable
fun UnitsScreen(
    units: List<RentalUnitEntity>,
    onSave: (RentalUnitEntity) -> Unit,
    onDelete: (RentalUnitEntity) -> Unit,
) {
    var editing by remember {
        mutableStateOf<RentalUnitEntity?>(null)
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
                Icon(Icons.Default.Add, "Tambah unit")
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                ScreenHeader(
                    "Unit & Penyewa",
                    "${units.size} unit tercatat pada perangkat",
                )
            }
            items(units, key = { it.id }) { unit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable {
                            editing = unit
                            showDialog = true
                        },
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    unit.name,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    "${unit.code} • ${unit.tenantName}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editing = unit
                                        showDialog = true
                                    },
                                ) {
                                    Icon(Icons.Default.Edit, "Edit")
                                }
                                IconButton(
                                    onClick = { onDelete(unit) },
                                ) {
                                    Icon(Icons.Default.Delete, "Hapus")
                                }
                            }
                        }
                        LabelValue("Tarif", unit.rate.toRupiah())
                        LabelValue(
                            "Siklus",
                            "${unit.frequency} • jatuh tempo tgl ${unit.dueDay}",
                        )
                        LabelValue(
                            "Cadangan per tagihan",
                            formatPercent(unit.reservePercent),
                        )
                        LabelValue("Status", unit.status)
                    }
                }
            }
        }
    }

    if (showDialog) {
        UnitDialog(
            existing = editing,
            onDismiss = { showDialog = false },
            onSave = {
                onSave(it)
                showDialog = false
            },
        )
    }
}

@Composable
private fun UnitDialog(
    existing: RentalUnitEntity?,
    onDismiss: () -> Unit,
    onSave: (RentalUnitEntity) -> Unit,
) {
    var code by remember(existing) {
        mutableStateOf(existing?.code.orEmpty())
    }
    var name by remember(existing) {
        mutableStateOf(existing?.name.orEmpty())
    }
    var tenant by remember(existing) {
        mutableStateOf(existing?.tenantName.orEmpty())
    }
    var rate by remember(existing) {
        mutableStateOf(existing?.rate?.toString().orEmpty())
    }
    var frequency by remember(existing) {
        mutableStateOf(existing?.frequency ?: "Bulanan")
    }
    var dueDay by remember(existing) {
        mutableStateOf(existing?.dueDay?.toString() ?: "10")
    }
    var reservePercent by remember(existing) {
        mutableStateOf(
            ((existing?.reservePercent ?: 0.15) * 100.0)
                .let { value ->
                    if (value % 1.0 == 0.0) {
                        value.toInt().toString()
                    } else {
                        String.format(Locale.ROOT, "%.2f", value)
                            .trimEnd('0')
                            .trimEnd('.')
                    }
                },
        )
    }
    var status by remember(existing) {
        mutableStateOf(existing?.status ?: "Aktif")
    }
    var notes by remember(existing) {
        mutableStateOf(existing?.notes.orEmpty())
    }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    val frequencies = listOf(
        "Bulanan" to 1,
        "Triwulanan" to 3,
        "Semester" to 6,
        "Tahunan" to 12,
    )
    val interval = frequencies.firstOrNull {
        it.first == frequency
    }?.second ?: 1
    val parsedRate = rate.toLongOrNull()
    val parsedDueDay = dueDay.toIntOrNull()
    val parsedReservePercent = reservePercent.toDoubleOrNull()
    val valid = code.isNotBlank() &&
        name.isNotBlank() &&
        parsedRate != null &&
        parsedRate >= 0L &&
        parsedDueDay in 1..31 &&
        parsedReservePercent != null &&
        parsedReservePercent in 0.0..100.0

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "Tambah unit" else "Edit unit")
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("ID Unit") },
                        enabled = existing == null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Unit") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = tenant,
                        onValueChange = { tenant = it },
                        label = { Text("Penyewa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = rate,
                        onValueChange = {
                            rate = it.filter(Char::isDigit)
                        },
                        label = { Text("Tarif per siklus") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { frequencyExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Frekuensi: $frequency")
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                            )
                        }
                        DropdownMenu(
                            expanded = frequencyExpanded,
                            onDismissRequest = {
                                frequencyExpanded = false
                            },
                        ) {
                            frequencies.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.first) },
                                    onClick = {
                                        frequency = item.first
                                        frequencyExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = dueDay,
                        onValueChange = {
                            dueDay = it.filter(Char::isDigit)
                        },
                        label = { Text("Tanggal jatuh tempo") },
                        isError = parsedDueDay !in 1..31,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = reservePercent,
                        onValueChange = {
                            reservePercent = it.filter { char ->
                                char.isDigit() || char == '.' || char == ','
                            }.replace(',', '.')
                        },
                        label = { Text("Cadangan per tagihan (%)") },
                        supportingText = {
                            Text("Nilai 0 sampai 100 persen")
                        },
                        isError = parsedReservePercent == null ||
                            parsedReservePercent !in 0.0..100.0,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { statusExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Status: $status")
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                            )
                        }
                        DropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = {
                                statusExpanded = false
                            },
                        ) {
                            listOf("Aktif", "Kosong", "Nonaktif")
                                .forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item) },
                                        onClick = {
                                            status = item
                                            statusExpanded = false
                                        },
                                    )
                                }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
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
                        RentalUnitEntity(
                            id = existing?.id
                                ?: code.ifBlank {
                                    UUID.randomUUID().toString()
                                },
                            code = code.trim(),
                            name = name.trim(),
                            tenantName = tenant.trim(),
                            frequency = frequency,
                            rate = requireNotNull(parsedRate),
                            intervalMonths = interval,
                            reservePercent =
                                requireNotNull(parsedReservePercent) / 100.0,
                            status = status,
                            dueDay = requireNotNull(parsedDueDay),
                            notes = notes.trim(),
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

private fun formatPercent(value: Double): String {
    val percentage = value * 100.0
    return if (percentage % 1.0 == 0.0) {
        "${percentage.toInt()}%"
    } else {
        String.format(Locale.ROOT, "%.2f%%", percentage)
            .replace(".00%", "%")
            .replace("0%", "%")
    }
}
