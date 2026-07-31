package id.djawadwipa.manajemenkontrakan.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Payments
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import id.djawadwipa.manajemenkontrakan.ui.screens.DashboardScreen
import id.djawadwipa.manajemenkontrakan.ui.screens.ExpenseCategoriesScreen
import id.djawadwipa.manajemenkontrakan.ui.screens.ExpensesScreen
import id.djawadwipa.manajemenkontrakan.ui.screens.InvoiceDetailScreen
import id.djawadwipa.manajemenkontrakan.ui.screens.InvoicesScreen
import id.djawadwipa.manajemenkontrakan.ui.screens.PaymentHistoryScreen
import id.djawadwipa.manajemenkontrakan.ui.screens.ReportsScreen
import id.djawadwipa.manajemenkontrakan.ui.screens.SettingsScreen
import id.djawadwipa.manajemenkontrakan.ui.screens.UnitsScreen

private data class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val destinations = listOf(
    Destination("dashboard", "Dasbor", Icons.Default.Dashboard),
    Destination("units", "Unit", Icons.Default.Apartment),
    Destination(
        "invoices",
        "Tagihan",
        Icons.AutoMirrored.Filled.ReceiptLong,
    ),
    Destination("expenses", "Biaya", Icons.Default.Payments),
    Destination("reports", "Laporan", Icons.Default.Assessment),
    Destination("settings", "Atur", Icons.Default.Settings),
)

@Composable
fun ManajemenKontrakanApp(
    viewModel: MainViewModel,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val openDestination: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
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
            if (currentRoute in destinations.map { it.route }) {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                openDestination(destination.route)
                            },
                            icon = {
                                Icon(
                                    destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
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
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding),
        ) {
            composable("dashboard") {
                DashboardScreen(
                    state = state,
                    onOpenReports = {
                        openDestination("reports")
                    },
                    onOpenInvoices = {
                        openDestination("invoices")
                    },
                    onOpenUnits = {
                        openDestination("units")
                    },
                    onOpenExpenses = {
                        openDestination("expenses")
                    },
                )
            }

            composable("units") {
                UnitsScreen(
                    units = state.units,
                    onSave = viewModel::saveUnit,
                    onDelete = viewModel::deleteUnit,
                )
            }

            composable("invoices") {
                InvoicesScreen(
                    invoices = state.invoices,
                    units = state.units,
                    settings = state.settings,
                    onPayment = viewModel::recordPayment,
                    onRegenerate = viewModel::regenerateInvoices,
                    onCreateInvoice = viewModel::createInvoice,
                    onOpenDetail = { invoice ->
                        navController.navigate(
                            "invoice/${Uri.encode(invoice.id)}",
                        )
                    },
                )
            }

            composable("invoice/{invoiceId}") { backStackEntry ->
                val invoiceId = Uri.decode(
                    backStackEntry.arguments
                        ?.getString("invoiceId")
                        .orEmpty(),
                )
                val invoice = state.invoices.firstOrNull {
                    it.id == invoiceId
                }

                if (invoice == null) {
                    Text("Tagihan tidak ditemukan.")
                } else {
                    InvoiceDetailScreen(
                        invoice = invoice,
                        payments = state.payments.filter {
                            it.invoiceId == invoice.id
                        },
                        onBack = { navController.popBackStack() },
                        onOpenHistory = {
                            navController.navigate(
                                "invoice/${Uri.encode(invoice.id)}/payments",
                            )
                        },
                        onUpdate = viewModel::updateInvoice,
                        onDelete = viewModel::deleteInvoice,
                    )
                }
            }

            composable(
                "invoice/{invoiceId}/payments",
            ) { backStackEntry ->
                val invoiceId = Uri.decode(
                    backStackEntry.arguments
                        ?.getString("invoiceId")
                        .orEmpty(),
                )
                val invoice = state.invoices.firstOrNull {
                    it.id == invoiceId
                }

                if (invoice == null) {
                    Text("Tagihan tidak ditemukan.")
                } else {
                    PaymentHistoryScreen(
                        invoice = invoice,
                        payments = state.payments.filter {
                            it.invoiceId == invoice.id
                        },
                        onBack = { navController.popBackStack() },
                        onUpdate = viewModel::updatePayment,
                        onCancel = viewModel::cancelPayment,
                        onDelete = viewModel::deletePayment,
                    )
                }
            }

            composable("expenses") {
                ExpensesScreen(
                    state = state,
                    onSave = viewModel::saveExpense,
                    onDelete = viewModel::deleteExpense,
                    onOpenCategories = {
                        navController.navigate("expense-categories")
                    },
                )
            }

            composable("expense-categories") {
                ExpenseCategoriesScreen(
                    categories = state.categories,
                    expenses = state.expenses,
                    onBack = { navController.popBackStack() },
                    onSave = viewModel::saveExpenseCategory,
                    onDelete = viewModel::deleteExpenseCategory,
                )
            }

            composable("reports") {
                ReportsScreen(state, viewModel)
            }

            composable("settings") {
                SettingsScreen(state, viewModel, onExit)
            }
        }
    }
}
