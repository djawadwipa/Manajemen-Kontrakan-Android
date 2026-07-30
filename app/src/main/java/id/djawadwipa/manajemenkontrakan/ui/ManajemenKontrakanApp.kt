package id.djawadwipa.manajemenkontrakan.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import id.djawadwipa.manajemenkontrakan.ui.screens.DashboardScreen
import id.djawadwipa.manajemenkontrakan.ui.screens.ExpensesScreen
import id.djawadwipa.manajemenkontrakan.ui.screens.InvoicesScreen
import id.djawadwipa.manajemenkontrakan.ui.screens.ReportsScreen
import id.djawadwipa.manajemenkontrakan.ui.screens.SettingsScreen
import id.djawadwipa.manajemenkontrakan.ui.screens.UnitsScreen

private data class Destination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val destinations = listOf(
    Destination("dashboard", "Dasbor", Icons.Default.Dashboard),
    Destination("units", "Unit", Icons.Default.Apartment),
    Destination("invoices", "Tagihan", Icons.Default.ReceiptLong),
    Destination("expenses", "Biaya", Icons.Default.Payments),
    Destination("reports", "Laporan", Icons.Default.Assessment),
    Destination("settings", "Atur", Icons.Default.Settings),
)

@Composable
fun ManajemenKontrakanApp(viewModel: MainViewModel, onExit: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val openDestination: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = { openDestination(destination.route) },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = {
                            Text(
                                text = destination.label,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                fontSize = 10.sp,
                            )
                        },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") {
                DashboardScreen(
                    state = state,
                    onOpenReports = { openDestination("reports") },
                    onOpenInvoices = { openDestination("invoices") },
                    onOpenUnits = { openDestination("units") },
                    onOpenExpenses = { openDestination("expenses") },
                )
            }
            composable("units") { UnitsScreen(state.units, viewModel::saveUnit, viewModel::deleteUnit) }
            composable("invoices") { InvoicesScreen(state.invoices, state.settings, viewModel::recordPayment, viewModel::regenerateInvoices) }
            composable("expenses") { ExpensesScreen(state, viewModel::saveExpense, viewModel::deleteExpense) }
            composable("reports") { ReportsScreen(state, viewModel) }
            composable("settings") { SettingsScreen(state, viewModel, onExit) }
        }
    }
}
