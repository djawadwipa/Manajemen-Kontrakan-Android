package id.djawadwipa.manajemenkontrakan.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import id.djawadwipa.manajemenkontrakan.ui.MainUiState
import id.djawadwipa.manajemenkontrakan.ui.components.KpiCard
import id.djawadwipa.manajemenkontrakan.ui.components.ScreenHeader
import id.djawadwipa.manajemenkontrakan.util.toRupiah
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    state: MainUiState,
    onOpenReports: () -> Unit,
    onOpenInvoices: () -> Unit,
    onOpenUnits: () -> Unit,
    onOpenExpenses: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            val month = Month.of(
                state.settings.dashboardMonth.coerceIn(1, 12),
            ).getDisplayName(
                TextStyle.FULL,
                Locale.forLanguageTag("id-ID"),
            )
            ScreenHeader(
                "Dashboard",
                "Ringkasan $month ${state.settings.activeYear}",
            )
        }
        item {
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KpiCard(
                    "Saldo kas",
                    state.cashBalance.toRupiah(),
                    "Saldo kumulatif",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenReports,
                )
                KpiCard(
                    "Piutang bulan",
                    state.dashboardReceivable.toRupiah(),
                    "${state.dashboardOverdue} menunggak",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenInvoices,
                )
            }
        }
        item {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KpiCard(
                    "Unit terisi",
                    "${state.activeUnits}/${state.units.size}",
                    "Okupansi ${(state.occupancyRate * 100).toInt()}%",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenUnits,
                )
                KpiCard(
                    "Penerimaan bulan",
                    state.dashboardReceived.toRupiah(),
                    "Koleksi ${(state.dashboardCollectionRate * 100).toInt()}%",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenInvoices,
                )
            }
        }
        item {
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KpiCard(
                    "Pengeluaran bulan",
                    state.dashboardExpenses.toRupiah(),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenExpenses,
                )
                KpiCard(
                    "Dana perbaikan",
                    state.reserveFund.toRupiah(),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenReports,
                )
            }
        }
        item {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Potensi, penerimaan, dan pengeluaran " +
                            state.settings.activeYear,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        "Perbandingan bulanan berdasarkan database lokal",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MonthlyChartLegend()
                    MonthlyBarChart(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyChartLegend() {
    val billedColor = MaterialTheme.colorScheme.primary
    val receivedColor = MaterialTheme.colorScheme.tertiary
    val expenseColor = MaterialTheme.colorScheme.error

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChartLegendItem(
            label = "Potensi",
            color = billedColor,
            modifier = Modifier.weight(1f),
        )
        ChartLegendItem(
            label = "Penerimaan",
            color = receivedColor,
            modifier = Modifier.weight(1f),
        )
        ChartLegendItem(
            label = "Pengeluaran",
            color = expenseColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ChartLegendItem(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(Modifier.size(10.dp)) {
            drawRect(color = color)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun MonthlyBarChart(
    state: MainUiState,
    modifier: Modifier = Modifier,
) {
    val summaries = state.monthlySummary
    val maximum = summaries.maxOfOrNull {
        maxOf(it.billed, it.received, it.expense)
    }?.coerceAtLeast(1L) ?: 1L
    val billedColor = MaterialTheme.colorScheme.primary
    val receivedColor = MaterialTheme.colorScheme.tertiary
    val expenseColor = MaterialTheme.colorScheme.error

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val groupWidth = size.width / 12f
            val barGap = 1.dp.toPx()
            val barWidth = groupWidth * 0.2f
            val barsWidth = barWidth * 3f + barGap * 2f

            summaries.forEachIndexed { index, item ->
                val groupStart = index * groupWidth
                val x = groupStart + (groupWidth - barsWidth) / 2f
                val billedHeight =
                    size.height * item.billed / maximum.toFloat()
                val receivedHeight =
                    size.height * item.received / maximum.toFloat()
                val expenseHeight =
                    size.height * item.expense / maximum.toFloat()

                drawRect(
                    color = billedColor,
                    topLeft = Offset(x, size.height - billedHeight),
                    size = Size(barWidth, billedHeight),
                )
                drawRect(
                    color = receivedColor,
                    topLeft = Offset(
                        x + barWidth + barGap,
                        size.height - receivedHeight,
                    ),
                    size = Size(barWidth, receivedHeight),
                )
                drawRect(
                    color = expenseColor,
                    topLeft = Offset(
                        x + (barWidth + barGap) * 2f,
                        size.height - expenseHeight,
                    ),
                    size = Size(barWidth, expenseHeight),
                )
            }

            drawLine(
                color = Color.Gray.copy(alpha = 0.35f),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx(),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            summaries.forEach { summary ->
                Text(
                    text = summary.month.month.getDisplayName(
                        TextStyle.SHORT,
                        Locale.forLanguageTag("id-ID"),
                    ),
                    modifier = Modifier.rotate(-35f),
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                )
            }
        }
    }
}
