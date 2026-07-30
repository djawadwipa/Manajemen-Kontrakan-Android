package id.djawadwipa.manajemenkontrakan.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.djawadwipa.manajemenkontrakan.ui.MainUiState
import id.djawadwipa.manajemenkontrakan.ui.MainViewModel
import id.djawadwipa.manajemenkontrakan.ui.components.KpiCard
import id.djawadwipa.manajemenkontrakan.ui.components.LabelValue
import id.djawadwipa.manajemenkontrakan.ui.components.ScreenHeader
import id.djawadwipa.manajemenkontrakan.util.toRupiah
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ReportsScreen(state: MainUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val pending by viewModel.pendingCsv.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { pending?.let { bytes -> writeBytes(context, it, bytes) } }
        viewModel.clearPendingCsv()
    }
    LaunchedEffect(pending) { if (pending != null) launcher.launch("laporan-kontrakan-${state.settings.activeYear}.csv") }
    LazyColumn(Modifier.fillMaxSize()) {
        item { ScreenHeader("Laporan", "Arus kas, laba-rugi basis kas, piutang, dan kinerja unit") }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard(
                    "Surplus usaha",
                    (state.totalReceived - state.businessExpenses -
                        (state.totalReceived * state.settings.reservePercent).toLong()).toRupiah(),
                    modifier = Modifier.weight(1f),
                )
                KpiCard("Saldo akhir", state.cashBalance.toRupiah(), modifier = Modifier.weight(1f))
            }
        }
        item {
            Button(onClick = viewModel::prepareCsvReport, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Icon(Icons.Default.Download, null)
                Text(" Ekspor laporan CSV")
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Rekap bulanan ${state.settings.activeYear}", style = MaterialTheme.typography.titleLarge)
                    state.monthlySummary.forEach { item ->
                        val month = item.month.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("id-ID"))
                        Text(month, style = MaterialTheme.typography.titleMedium)
                        LabelValue("Potensi", item.billed.toRupiah())
                        LabelValue("Diterima", item.received.toRupiah())
                        LabelValue("Pengeluaran", item.expense.toRupiah())
                    }
                }
            }
        }
    }
}

private fun writeBytes(context: Context, uri: Uri, bytes: ByteArray) {
    context.contentResolver.openOutputStream(uri, "w")?.use { it.write(bytes) }
}
