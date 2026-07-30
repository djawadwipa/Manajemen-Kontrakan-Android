package id.djawadwipa.manajemenkontrakan.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.djawadwipa.manajemenkontrakan.R
import id.djawadwipa.manajemenkontrakan.data.local.AppSettingEntity
import id.djawadwipa.manajemenkontrakan.ui.MainUiState
import id.djawadwipa.manajemenkontrakan.ui.MainViewModel
import id.djawadwipa.manajemenkontrakan.ui.components.ScreenHeader
import id.djawadwipa.manajemenkontrakan.util.CsvExporter

private enum class PasswordAction { BACKUP, RESTORE }

@Composable
fun SettingsScreen(state: MainUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val pendingBackup by viewModel.pendingBackup.collectAsStateWithLifecycle()
    var passwordAction by remember { mutableStateOf<PasswordAction?>(null) }
    var restoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var year by remember(state.settings.activeYear) { mutableStateOf(state.settings.activeYear.toString()) }
    var dashboardMonth by remember(state.settings.dashboardMonth) { mutableStateOf(state.settings.dashboardMonth.toString()) }
    var openingCash by remember(state.settings.openingCash) { mutableStateOf(state.settings.openingCash.toString()) }
    var reserve by remember(state.settings.reservePercent) { mutableStateOf((state.settings.reservePercent * 100).toInt().toString()) }
    var dueDay by remember(state.settings.defaultDueDay) { mutableStateOf(state.settings.defaultDueDay.toString()) }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { pendingBackup?.let { bytes -> writeBytes(context, it, bytes) } }
        viewModel.clearPendingBackup()
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { restoreBytes = readBytes(context, it); passwordAction = PasswordAction.RESTORE }
    }
    val openCsv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importUnitsCsv(readBytes(context, it).decodeToString()) }
    }
    val createTemplate = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { writeBytes(context, it, CsvExporter.unitTemplate().encodeToByteArray()) }
    }

    LaunchedEffect(pendingBackup) {
        if (pendingBackup != null) createBackup.launch("manajemen-kontrakan-${state.settings.activeYear}.mkbackup")
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item { ScreenHeader("Pengaturan & Data", "Semua data tersimpan lokal pada perangkat") }
        item {
            Image(painterResource(R.drawable.logo_mk), "Logo Manajemen Kontrakan", Modifier.fillMaxWidth().height(150.dp).padding(12.dp), contentScale = ContentScale.Fit)
        }
        item {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Parameter bisnis", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(year, { year = it.filter(Char::isDigit) }, label = { Text("Tahun aktif") }, singleLine = true)
                    OutlinedTextField(dashboardMonth, { dashboardMonth = it.filter(Char::isDigit) }, label = { Text("Bulan dashboard (1–12)") }, singleLine = true)
                    OutlinedTextField(openingCash, { openingCash = it.filter(Char::isDigit) }, label = { Text("Saldo kas awal") }, singleLine = true)
                    OutlinedTextField(reserve, { reserve = it.filter(Char::isDigit) }, label = { Text("Dana perbaikan (%)") }, singleLine = true)
                    OutlinedTextField(dueDay, { dueDay = it.filter(Char::isDigit) }, label = { Text("Jatuh tempo default") }, singleLine = true)
                    Button(onClick = {
                        val new = AppSettingEntity(
                            activeYear = year.toIntOrNull() ?: state.settings.activeYear,
                            dashboardMonth = dashboardMonth.toIntOrNull()?.coerceIn(1, 12) ?: state.settings.dashboardMonth,
                            openingCash = openingCash.toLongOrNull() ?: state.settings.openingCash,
                            reservePercent = (reserve.toDoubleOrNull() ?: 15.0) / 100,
                            defaultDueDay = dueDay.toIntOrNull()?.coerceIn(1, 31) ?: 10,
                            bookStatus = state.settings.bookStatus,
                        )
                        viewModel.updateSettings(new)
                    }) { Icon(Icons.Default.Save, null); Text(" Simpan pengaturan") }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Backup terenkripsi", style = MaterialTheme.typography.titleLarge)
                    Text("AES-256-GCM, PBKDF2, dan pemeriksaan checksum SHA-256. Kata sandi tidak disimpan.")
                    Button(onClick = { passwordAction = PasswordAction.BACKUP }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Download, null); Text(" Buat backup .mkbackup") }
                    Button(onClick = { openBackup.launch(arrayOf("application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FileOpen, null); Text(" Pulihkan backup") }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Impor unit CSV", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { createTemplate.launch("template-unit-kontrakan.csv") }, modifier = Modifier.weight(1f)) { Text("Template") }
                        Button(onClick = { openCsv.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) }, modifier = Modifier.weight(1f)) { Text("Impor") }
                    }
                }
            }
        }
        item { Text("Versi 0.1.0 • ${state.units.size} unit • tanpa akun dan tanpa cloud", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }

    passwordAction?.let { action ->
        PasswordDialog(action == PasswordAction.BACKUP, onDismiss = { passwordAction = null }) { password ->
            if (action == PasswordAction.BACKUP) viewModel.prepareBackup(password) else restoreBytes?.let { viewModel.restoreBackup(it, password) }
            passwordAction = null
            restoreBytes = null
        }
    }
}

@Composable
private fun PasswordDialog(creating: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val valid = password.length >= 8 && (!creating || password == confirmation)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (creating) "Kata sandi backup" else "Buka backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (creating) "Gunakan minimal 8 karakter. Simpan kata sandi dengan aman; aplikasi tidak menyimpannya." else "Masukkan kata sandi yang digunakan saat backup dibuat.")
                OutlinedTextField(password, { password = it }, label = { Text("Kata sandi") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                if (creating) OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Ulangi kata sandi") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            }
        },
        confirmButton = { Button(enabled = valid, onClick = { onConfirm(password) }) { Text(if (creating) "Lanjutkan" else "Pulihkan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

private fun readBytes(context: Context, uri: Uri): ByteArray = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("File tidak dapat dibaca.")
private fun writeBytes(context: Context, uri: Uri, bytes: ByteArray) { context.contentResolver.openOutputStream(uri, "w")?.use { it.write(bytes) } ?: error("File tidak dapat ditulis.") }
