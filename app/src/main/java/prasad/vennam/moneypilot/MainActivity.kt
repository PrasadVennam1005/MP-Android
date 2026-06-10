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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.feature.ai.presentation.AiChatScreen
import prasad.vennam.moneypilot.ui.ai.InsightsScreen
import prasad.vennam.moneypilot.ui.budget.ReportsTabScreen
import prasad.vennam.moneypilot.ui.categories.CategoryListScreen
import prasad.vennam.moneypilot.ui.dashboard.DashboardScreen
import prasad.vennam.moneypilot.ui.dashboard.SyncState
import prasad.vennam.moneypilot.ui.investments.InvestmentScreen
import prasad.vennam.moneypilot.ui.loans.LoanScreen
import prasad.vennam.moneypilot.ui.navigation.Destination
import prasad.vennam.moneypilot.ui.scanner.ReceiptScannerScreen
import prasad.vennam.moneypilot.ui.faq.FaqScreen
import prasad.vennam.moneypilot.ui.settings.SettingsScreen
import prasad.vennam.moneypilot.ui.theme.MoneyPilotTheme
import prasad.vennam.moneypilot.ui.transactions.AddEditTransactionScreen
import prasad.vennam.moneypilot.ui.transactions.HistoryScreen
import prasad.vennam.moneypilot.ui.viewmodel.AnalyticsViewModel
import prasad.vennam.moneypilot.ui.viewmodel.BudgetViewModel
import prasad.vennam.moneypilot.ui.viewmodel.InvestmentViewModel
import prasad.vennam.moneypilot.ui.viewmodel.MainViewModel
import prasad.vennam.moneypilot.ui.viewmodel.RestoreState
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.worker.LoanNotificationScheduler

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var loanDao: prasad.vennam.moneypilot.data.dao.LoanDao

    @Inject
    lateinit var notificationDao: prasad.vennam.moneypilot.data.dao.NotificationDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission for Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }

        // Schedule timezone-based Daily News Notifications
        prasad.vennam.moneypilot.worker.DailyNewsWorker
            .schedule(applicationContext)

        // Trigger immediate run for development testing ONLY in debug builds
        if (prasad.vennam.moneypilot.BuildConfig.DEBUG) {
            androidx.work.WorkManager.getInstance(applicationContext).enqueue(
                androidx.work.OneTimeWorkRequestBuilder<prasad.vennam.moneypilot.worker.DailyNewsWorker>().build(),
            )
        }

        // Check and trigger loan EMI reminder alerts on startup
        lifecycleScope.launch {
            LoanNotificationScheduler.checkAndTriggerLoanReminders(
                applicationContext,
                loanDao,
                notificationDao
            )
        }

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeMode by mainViewModel.themeMode.collectAsState()
            val darkTheme =
                when (themeMode) {
                    prasad.vennam.moneypilot.data.UserPreferences.ThemeMode.LIGHT -> false
                    prasad.vennam.moneypilot.data.UserPreferences.ThemeMode.DARK -> true
                    else -> androidx.compose.foundation.isSystemInDarkTheme()
                }
            MoneyPilotTheme(darkTheme = darkTheme) {
                MoneyPilotApp(analyticsHelper, mainViewModel)
            }
        }
    }
}

@Composable
fun MoneyPilotApp(
    analyticsHelper: AnalyticsHelper,
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val backStack = rememberNavBackStack(Destination.Auth() as Destination)

    val transactionViewModel: TransactionViewModel = hiltViewModel()
    val budgetViewModel: BudgetViewModel = hiltViewModel()
    val investmentViewModel: InvestmentViewModel = hiltViewModel()
    val analyticsViewModel: AnalyticsViewModel = hiltViewModel()

    val currentDestination = backStack.lastOrNull()

    val currencyCode by mainViewModel.currency.collectAsState(initial = "INR")
    val userData by mainViewModel.userData.collectAsState(initial = null)

    val context = androidx.compose.ui.platform.LocalContext.current
    val workManager = remember { androidx.work.WorkManager.getInstance(context) }
    val workInfos by workManager
        .getWorkInfosForUniqueWorkFlow(prasad.vennam.moneypilot.util.GoogleSheetsSyncHelper.SYNC_WORK_NAME)
        .collectAsState(initial = emptyList())
    val restoreState by mainViewModel.restoreState.collectAsState()
    val isSynced by mainViewModel.isSynced.collectAsState()
    val spreadsheetId by mainViewModel.spreadsheetId.collectAsState()
    val isGuest = remember(userData) { userData?.email == "guest@moneypilot.app" }

    val syncState =
        remember(isSynced, workInfos, isGuest, restoreState, spreadsheetId) {
            if (isGuest) {
                null
            } else if (restoreState is RestoreState.Checking) {
                SyncState.SYNCING
            } else if (!isSynced || spreadsheetId == null) {
                SyncState.PENDING_CONNECTION
            } else {
                val workInfo = workInfos.firstOrNull()
                when (workInfo?.state) {
                    androidx.work.WorkInfo.State.RUNNING -> SyncState.SYNCING
                    androidx.work.WorkInfo.State.FAILED -> SyncState.FAILED
                    else -> SyncState.SYNCED
                }
            }
        }

    CompositionLocalProvider(LocalCurrencyCode provides currencyCode) {
        // 1. Navigation Analytics: Track screen transitions
        LaunchedEffect(currentDestination) {
            currentDestination?.let {
                analyticsHelper.logScreenView(it::class.java.simpleName)
            }
        }

        val showNavigation =
            currentDestination !is Destination.Auth &&
                currentDestination !is Destination.ReceiptScanner &&
                currentDestination !is Destination.AiChat &&
                currentDestination !is Destination.Insights

        NavigationSuiteScaffold(
            layoutType =
                if (showNavigation) {
                    NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
                } else {
                    NavigationSuiteType.None
                },
            containerColor = MaterialTheme.colorScheme.background,
            navigationSuiteItems = {
                if (showNavigation) {
                    item(
                        selected = currentDestination is Destination.Dashboard,
                        onClick = {
                            backStack.clear()
                            backStack.add(Destination.Dashboard)
                        },
                        icon = { Icon(Icons.Rounded.Dashboard, contentDescription = stringResource(R.string.dashboard)) },
                        label = { Text(stringResource(R.string.dashboard)) },
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
                                contentDescription = stringResource(R.string.expenses),
                            )
                        },
                        label = { Text(stringResource(R.string.expenses)) },
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
                                contentDescription = stringResource(R.string.income),
                            )
                        },
                        label = { Text(stringResource(R.string.income)) },
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
                                contentDescription = stringResource(R.string.investments),
                            )
                        },
                        label = { Text(stringResource(R.string.investments)) },
                        alwaysShowLabel = currentDestination is Destination.Investments,
                    )
                    item(
                        selected = currentDestination is Destination.Reports,
                        onClick = {
                            backStack.clear()
                            backStack.add(Destination.Reports)
                        },
                        icon = { Icon(Icons.Rounded.BarChart, contentDescription = stringResource(R.string.reports)) },
                        label = { Text(stringResource(R.string.reports)) },
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
                        is Destination.Auth ->
                            NavEntry(key) {
                                prasad.vennam.moneypilot.ui.login.AuthScreen(
                                    mainViewModel = mainViewModel,
                                    analyticsHelper = analyticsHelper,
                                    skipSplash = key.skipSplash,
                                    onAuthSuccess = {
                                        backStack.clear()
                                        backStack.add(Destination.Dashboard)
                                    },
                                )
                            }

                        is Destination.Dashboard ->
                            NavEntry(key) {
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
                                    onNavigateToNotifications = {
                                        backStack.add(Destination.Notifications)
                                    },
                                    onNavigateToLoans = {
                                        backStack.add(Destination.Loans)
                                    },
                                    onNavigateToInsights = {
                                        backStack.add(Destination.Insights)
                                    },
                                    onNavigateToAiChat = {
                                        backStack.add(Destination.AiChat)
                                    },
                                    analyticsHelper = analyticsHelper,
                                )
                            }

                        is Destination.Insights ->
                            NavEntry(key) {
                                InsightsScreen(
                                    userData = userData,
                                    syncState = syncState,
                                    onProfileClick = { backStack.add(Destination.Settings) },
                                    onBackClick = { backStack.removeLastOrNull() },
                                    onNavigateToAiChat = { backStack.add(Destination.AiChat) },
                                )
                            }

                        is Destination.AiChat ->
                            NavEntry(key) {
                                AiChatScreen(
                                    onBackClick = { backStack.removeLastOrNull() },
                                )
                            }

                        is Destination.Expenses ->
                            NavEntry(key) {
                                HistoryScreen(
                                    viewModel = transactionViewModel,
                                    onAddTransaction = {
                                        backStack.add(Destination.AddEditTransaction(initialType = TransactionType.EXPENSE))
                                    },
                                    onEditTransaction = { id ->
                                        backStack.add(Destination.AddEditTransaction(id, TransactionType.EXPENSE))
                                    },
                                    userData = userData,
                                    syncState = syncState,
                                    onProfileClick = { backStack.add(Destination.Settings) },
                                    fixedType = TransactionType.EXPENSE,
                                )
                            }

                        is Destination.Income ->
                            NavEntry(key) {
                                HistoryScreen(
                                    viewModel = transactionViewModel,
                                    onAddTransaction = {
                                        backStack.add(Destination.AddEditTransaction(initialType = TransactionType.INCOME))
                                    },
                                    onEditTransaction = { id ->
                                        backStack.add(Destination.AddEditTransaction(id, TransactionType.INCOME))
                                    },
                                    userData = userData,
                                    syncState = syncState,
                                    onProfileClick = { backStack.add(Destination.Settings) },
                                    fixedType = TransactionType.INCOME,
                                )
                            }

                        is Destination.AddEditTransaction ->
                            NavEntry(key) {
                                AddEditTransactionScreen(
                                    transactionId = key.transactionId,
                                    initialType = key.initialType,
                                    viewModel = transactionViewModel,
                                    analyticsHelper = analyticsHelper,
                                    onNavigateBack = { backStack.removeLastOrNull() },
                                )
                            }

                        is Destination.Investments ->
                            NavEntry(key) {
                                InvestmentScreen(
                                    viewModel = investmentViewModel,
                                    userData = userData,
                                    syncState = syncState,
                                    onProfileClick = { backStack.add(Destination.Settings) },
                                )
                            }

                        is Destination.Loans ->
                            NavEntry(key) {
                                LoanScreen(
                                    userData = userData,
                                    syncState = syncState,
                                    onProfileClick = { backStack.add(Destination.Settings) },
                                )
                            }

                        is Destination.Reports ->
                            NavEntry(key) {
                                ReportsTabScreen(
                                    budgetViewModel = budgetViewModel,
                                    transactionViewModel = transactionViewModel,
                                    analyticsViewModel = analyticsViewModel,
                                    userData = userData,
                                    syncState = syncState,
                                    onProfileClick = { backStack.add(Destination.Settings) },
                                )
                            }

                        is Destination.Settings ->
                            NavEntry(key) {
                                SettingsScreen(
                                    transactionViewModel = transactionViewModel,
                                    budgetViewModel = budgetViewModel,
                                    investmentViewModel = investmentViewModel,
                                    mainViewModel = mainViewModel,
                                    analyticsHelper = analyticsHelper,
                                    onLogout = {
                                        backStack.clear()
                                        backStack.add(Destination.Auth(skipSplash = true))
                                    },
                                    onNavigateToCategories = {
                                        backStack.add(Destination.ManageCategories)
                                    },
                                    onNavigateToNotifications = {
                                        backStack.add(Destination.Notifications)
                                    },
                                    onNavigateToFAQ = {
                                        backStack.add(Destination.FAQ)
                                    },
                                    onAccountDeleted = {
                                        backStack.clear()
                                        backStack.add(Destination.Auth(skipSplash = false))
                                    },
                                )
                            }

                        is Destination.ManageCategories ->
                            NavEntry(key) {
                                CategoryListScreen(
                                    viewModel = transactionViewModel,
                                    onNavigateBack = { backStack.removeLastOrNull() },
                                )
                            }

                        is Destination.ReceiptScanner ->
                            NavEntry(key) {
                                ReceiptScannerScreen(
                                    onNavigateBack = { backStack.removeLastOrNull() },
                                    transactionViewModel = transactionViewModel,
                                    analyticsHelper = analyticsHelper,
                                )
                            }

                        is Destination.Notifications ->
                            NavEntry(key) {
                                prasad.vennam.moneypilot.ui.notifications.NotificationsScreen(
                                    onNavigateBack = { backStack.removeLastOrNull() },
                                )
                            }

                        is Destination.FAQ ->
                            NavEntry(key) {
                                FaqScreen(
                                    onNavigateBack = { backStack.removeLastOrNull() },
                                )
                            }

                        else -> error("Unknown destination: $key")
                    }
                },
            )
        }
    }
}
