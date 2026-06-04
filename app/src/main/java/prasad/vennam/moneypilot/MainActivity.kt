package prasad.vennam.moneypilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.ui.budget.BudgetScreen
import prasad.vennam.moneypilot.ui.dashboard.DashboardScreen
import prasad.vennam.moneypilot.ui.investments.InvestmentScreen
import prasad.vennam.moneypilot.ui.navigation.Destination
import prasad.vennam.moneypilot.ui.splash.SplashScreen
import prasad.vennam.moneypilot.ui.login.LoginScreen
import prasad.vennam.moneypilot.ui.scanner.ReceiptScannerScreen
import prasad.vennam.moneypilot.ui.settings.SettingsScreen
import prasad.vennam.moneypilot.ui.categories.CategoryListScreen
import prasad.vennam.moneypilot.ui.theme.MoneyPilotTheme
import prasad.vennam.moneypilot.ui.transactions.AddEditTransactionScreen
import prasad.vennam.moneypilot.ui.transactions.HistoryScreen
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.ui.viewmodel.BudgetViewModel
import prasad.vennam.moneypilot.ui.viewmodel.InvestmentViewModel
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.ui.viewmodel.MainViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoneyPilotTheme {
                MoneyPilotApp(analyticsHelper)
            }
        }
    }
}

@Composable
fun MoneyPilotApp(analyticsHelper: AnalyticsHelper) {
    val backStack = rememberNavBackStack(Destination.Splash as Destination)
    val mainViewModel: MainViewModel = hiltViewModel()
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()

    val transactionViewModel: TransactionViewModel = hiltViewModel()
    val budgetViewModel: BudgetViewModel = hiltViewModel()
    val investmentViewModel: InvestmentViewModel = hiltViewModel()

    val currentDestination = backStack.lastOrNull()
    
    // 1. Navigation Analytics: Track screen transitions
    LaunchedEffect(currentDestination) {
        currentDestination?.let {
            analyticsHelper.logScreenView(it::class.java.simpleName)
        }
    }

    val showNavigation = currentDestination !is Destination.Splash && currentDestination !is Destination.Login && currentDestination !is Destination.ReceiptScanner

    NavigationSuiteScaffold(
        containerColor = MaterialTheme.colorScheme.background,
        navigationSuiteItems = {
            if (showNavigation) {
                item(
                    selected = currentDestination is Destination.Dashboard,
                    onClick = {
                        backStack.clear()
                        backStack.add(Destination.Dashboard)
                    },
                    icon = { Icon(Icons.Rounded.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    alwaysShowLabel = currentDestination is Destination.Dashboard,
                )
                item(
                    selected = currentDestination is Destination.Expenses,
                    onClick = {
                        backStack.clear()
                        backStack.add(Destination.Expenses)
                    },
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Rounded.TrendingDown,
                            contentDescription = "Expenses"
                        )
                    },
                    label = { Text("Expenses") },
                    alwaysShowLabel = currentDestination is Destination.Expenses,
                )
                item(
                    selected = currentDestination is Destination.Income,
                    onClick = {
                        backStack.clear()
                        backStack.add(Destination.Income)
                    },
                    icon = {
                        Icon(
                            Icons.AutoMirrored.Rounded.TrendingUp,
                            contentDescription = "Income"
                        )
                    },
                    label = { Text("Income") },
                    alwaysShowLabel = currentDestination is Destination.Income,
                )
                item(
                    selected = currentDestination is Destination.Investments,
                    onClick = {
                        backStack.clear()
                        backStack.add(Destination.Investments)
                    },
                    icon = {
                        Icon(
                            Icons.Rounded.AccountBalanceWallet,
                            contentDescription = "Investments"
                        )
                    },
                    label = { Text("Investment") },
                    alwaysShowLabel = currentDestination is Destination.Investments,
                )
                item(
                    selected = currentDestination is Destination.Reports,
                    onClick = {
                        backStack.clear()
                        backStack.add(Destination.Reports)
                    },
                    icon = { Icon(Icons.Rounded.BarChart, contentDescription = "Reports") },
                    label = { Text("Reports") },
                    alwaysShowLabel = currentDestination is Destination.Reports,
                )
            }
        },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            modifier = Modifier,
            entryProvider = { key ->
                when (key) {
                    is Destination.Splash -> NavEntry(key) {
                        SplashScreen(
                            onSplashFinished = {
                                backStack.clear()
                                if (isLoggedIn) {
                                    backStack.add(Destination.Dashboard)
                                } else {
                                    backStack.add(Destination.Login)
                                }
                            }
                        )
                    }

                    is Destination.Login -> NavEntry(key) {
                        LoginScreen(
                            mainViewModel = mainViewModel,
                            onLoginSuccess = {
                                backStack.clear()
                                backStack.add(Destination.Dashboard)
                            },
                            analyticsHelper = analyticsHelper
                        )
                    }

                    is Destination.Dashboard -> NavEntry(key) {
                        DashboardScreen(
                            transactionViewModel = transactionViewModel,
                            investmentViewModel = investmentViewModel,
                            budgetViewModel = budgetViewModel,
                            mainViewModel = mainViewModel,
                            onNavigateToAddTransaction = { type ->
                                backStack.add(Destination.AddEditTransaction(initialType = type))
                            },
                            onNavigateToAddInvestment = {
                                backStack.add(Destination.Investments)
                            },
                            onNavigateToHistory = {
                                backStack.add(Destination.Expenses)
                            },
                            onNavigateToBudgets = {
                                backStack.add(Destination.Reports)
                            },
                            onNavigateToSettings = {
                                backStack.add(Destination.Settings)
                            },
                            onNavigateToScanner = {
                                backStack.add(Destination.ReceiptScanner)
                            },
                            analyticsHelper = analyticsHelper
                        )
                    }

                    is Destination.Expenses -> NavEntry(key) {
                        HistoryScreen(
                            viewModel = transactionViewModel,
                            onAddTransaction = {
                                backStack.add(Destination.AddEditTransaction(initialType = TransactionType.EXPENSE))
                            },
                            onEditTransaction = { id ->
                                backStack.add(Destination.AddEditTransaction(id, TransactionType.EXPENSE))
                            },
                            fixedType = TransactionType.EXPENSE
                        )
                    }

                    is Destination.Income -> NavEntry(key) {
                        HistoryScreen(
                            viewModel = transactionViewModel,
                            onAddTransaction = {
                                backStack.add(Destination.AddEditTransaction(initialType = TransactionType.INCOME))
                            },
                            onEditTransaction = { id ->
                                backStack.add(Destination.AddEditTransaction(id, TransactionType.INCOME))
                            },
                            fixedType = TransactionType.INCOME
                        )
                    }

                    is Destination.AddEditTransaction -> NavEntry(key) {
                        AddEditTransactionScreen(
                            transactionId = key.transactionId,
                            initialType = key.initialType,
                            viewModel = transactionViewModel,
                            analyticsHelper = analyticsHelper,
                            onNavigateBack = { backStack.removeLastOrNull() }
                        )
                    }

                    is Destination.Investments -> NavEntry(key) {
                        InvestmentScreen(viewModel = investmentViewModel)
                    }

                    is Destination.Reports -> NavEntry(key) {
                        BudgetScreen(
                            budgetViewModel = budgetViewModel,
                            transactionViewModel = transactionViewModel
                        )
                    }

                    is Destination.Settings -> NavEntry(key) {
                        SettingsScreen(
                            transactionViewModel = transactionViewModel,
                            budgetViewModel = budgetViewModel,
                            investmentViewModel = investmentViewModel,
                            mainViewModel = mainViewModel,
                            analyticsHelper = analyticsHelper,
                            onLogout = {
                                backStack.clear()
                                backStack.add(Destination.Login)
                            },
                            onNavigateToCategories = {
                                backStack.add(Destination.ManageCategories)
                            }
                        )
                    }

                    is Destination.ManageCategories -> NavEntry(key) {
                        CategoryListScreen(
                            viewModel = transactionViewModel,
                            onNavigateBack = { backStack.removeLastOrNull() }
                        )
                    }

                    is Destination.ReceiptScanner -> NavEntry(key) {
                        ReceiptScannerScreen(
                            onNavigateBack = { backStack.removeLastOrNull() },
                            transactionViewModel = transactionViewModel,
                            analyticsHelper = analyticsHelper
                        )
                    }

                    else -> error("Unknown destination: $key")
                }
            }
        )
    }
}
