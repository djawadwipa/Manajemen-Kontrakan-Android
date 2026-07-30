package id.djawadwipa.manajemenkontrakan.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.djawadwipa.manajemenkontrakan.ui.MainUiState
import id.djawadwipa.manajemenkontrakan.ui.components.KpiCard
import id.djawadwipa.manajemenkontrakan.ui.components.ScreenHeader
import id.djawadwipa.manajemenkontrakan.util.toRupiah
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(state: MainUiState) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            val month = Month.of(state.settings.dashboardMonth.coerceIn(1, 12))
                .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("id-ID"))
            ScreenHeader("Dashboard", "Ringkasan $month ${state.settings.activeYear}")
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard("Saldo kas", state.cashBalance.toRupiah(), "Saldo kumulatif", Modifier.weight(1f))
                KpiCard("Piutang bulan", state.dashboardReceivable.toRupiah(), "${state.dashboardOverdue} menunggak", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard("Unit terisi", "${state.activeUnits}/${state.units.size}", "Okupansi ${(state.occupancyRate * 100).toInt()}%", Modifier.weight(1f))
                KpiCard("Penerimaan bulan", state.dashboardReceived.toRupiah(), "Koleksi ${(state.dashboardCollectionRate * 100).toInt()}%", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard("Pengeluaran bulan", state.dashboardExpenses.toRupiah(), modifier = Modifier.weight(1f))
                KpiCard("Dana perbaikan", state.reserveFund.toRupiah(), modifier = Modifier.weight(1f))
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Potensi vs penerimaan ${state.settings.activeYear}", style = MaterialTheme.typography.titleLarge)
                    Text("Grafik bulanan berdasarkan database lokal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MonthlyBarChart(state, Modifier.fillMaxWidth().height(230.dp).padding(top = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun MonthlyBarChart(state: MainUiState, modifier: Modifier = Modifier) {
    val summaries = state.monthlySummary
    val max = summaries.maxOfOrNull { maxOf(it.billed, it.received) }?.coerceAtLeast(1) ?: 1
    val billedColor = MaterialTheme.colorScheme.primary
    val receivedColor = MaterialTheme.colorScheme.tertiary
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val groupWidth = size.width / 12f
            val barWidth = groupWidth * 0.28f
            summaries.forEachIndexed { index, item ->
                val x = index * groupWidth + groupWidth * 0.18f
                val billedHeight = size.height * item.billed / max.toFloat()
                val receivedHeight = size.height * item.received / max.toFloat()
                drawRect(billedColor, Offset(x, size.height - billedHeight), Size(barWidth, billedHeight))
                drawRect(receivedColor, Offset(x + barWidth + 3.dp.toPx(), size.height - receivedHeight), Size(barWidth, receivedHeight))
            }
            drawLine(Color.Gray.copy(alpha = 0.35f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            summaries.forEach { Text(it.month.month.getDisplayName(TextStyle.NARROW, Locale.forLanguageTag("id-ID")), style = MaterialTheme.typography.labelSmall) }
        }
    }
}
