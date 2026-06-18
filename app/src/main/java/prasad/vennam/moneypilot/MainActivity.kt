package prasad.vennam.moneypilot

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import prasad.vennam.moneypilot.billing.BillingManager
import prasad.vennam.moneypilot.data.AppLinks
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.feature.ai.presentation.AiChatScreen
import prasad.vennam.moneypilot.ui.ai.InsightsScreen
import prasad.vennam.moneypilot.ui.budget.ReportsTabScreen
import prasad.vennam.moneypilot.ui.categories.CategoryListScreen
import prasad.vennam.moneypilot.ui.dashboard.DashboardScreen
import prasad.vennam.moneypilot.ui.dashboard.SyncState
import prasad.vennam.moneypilot.ui.emergencyfund.EmergencyFundScreen
import prasad.vennam.moneypilot.ui.faq.FaqScreen
import prasad.vennam.moneypilot.ui.investments.InvestmentScreen
import prasad.vennam.moneypilot.ui.learnfinance.ArticleDetailScreen
import prasad.vennam.moneypilot.ui.learnfinance.LearnFinanceScreen
import prasad.vennam.moneypilot.ui.loans.EmiCalculatorScreen
import prasad.vennam.moneypilot.ui.loans.LoanScreen
import prasad.vennam.moneypilot.ui.navigation.Destination
import prasad.vennam.moneypilot.ui.news.NewsScreen
import prasad.vennam.moneypilot.ui.news.NewsWebViewScreen
import prasad.vennam.moneypilot.ui.scanner.ReceiptScannerScreen
import prasad.vennam.moneypilot.ui.settings.SettingsScreen
import prasad.vennam.moneypilot.ui.theme.MoneyPilotTheme
import prasad.vennam.moneypilot.ui.transactions.AddEditTransactionScreen
import prasad.vennam.moneypilot.ui.transactions.HistoryScreen
import prasad.vennam.moneypilot.ui.viewmodel.AnalyticsViewModel
import prasad.vennam.moneypilot.ui.viewmodel.BudgetViewModel
import prasad.vennam.moneypilot.ui.viewmodel.InvestmentViewModel
import prasad.vennam.moneypilot.ui.viewmodel.LearnFinanceViewModel
import prasad.vennam.moneypilot.ui.viewmodel.MainViewModel
import prasad.vennam.moneypilot.ui.viewmodel.RestoreState
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this) {}

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
        if (BuildConfig.DEBUG) {
            androidx.work.WorkManager.getInstance(applicationContext).enqueue(
                androidx.work.OneTimeWorkRequestBuilder<prasad.vennam.moneypilot.worker.DailyNewsWorker>().build(),
            )
        }

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                mainViewModel.checkLoanReminders()
            }
            val themeMode by mainViewModel.themeMode.collectAsState()
            val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()
            // isAuthenticated is reset only on ON_STOP (app backgrounded), NOT on ON_PAUSE
            // This prevents the biometric prompt itself (which pauses the activity) from re-locking.
            var isAuthenticated by remember { mutableStateOf(false) }

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                        // Only lock when the app goes fully to background, not for overlays/dialogs
                        isAuthenticated = false
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Auto-show biometric prompt once when the screen comes into focus and is not yet authenticated
            LaunchedEffect(isBiometricEnabled) {
                if (isBiometricEnabled && !isAuthenticated) {
                    prasad.vennam.moneypilot.util.BiometricHelper.authenticate(
                        activity = this@MainActivity,
                        title = "Unlock MoneyPilot",
                        subtitle = "Verify your identity to open the app",
                        onSuccess = { isAuthenticated = true },
                        onError = {
                            // Silently ignore; user can tap Unlock button
                        }
                    )
                }
            }

            val darkTheme =
                when (themeMode) {
                    UserPreferences.ThemeMode.LIGHT -> false
                    UserPreferences.ThemeMode.DARK -> true
                    else -> androidx.compose.foundation.isSystemInDarkTheme()
                }
            MoneyPilotTheme(darkTheme = darkTheme) {
                if (isBiometricEnabled && !isAuthenticated) {
                    BiometricLockScreen(
                        onUnlockClick = {
                            prasad.vennam.moneypilot.util.BiometricHelper.authenticate(
                                activity = this@MainActivity,
                                title = "Unlock MoneyPilot",
                                subtitle = "Verify your identity to open the app",
                                onSuccess = { isAuthenticated = true },
                                onError = {
                                    android.widget.Toast.makeText(
                                        this@MainActivity,
                                        "Authentication failed. Please try again.",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    )
                } else {
                    MoneyPilotApp(analyticsHelper, mainViewModel, userPreferences)
                }
            }
        }
    }
}

/**
 * Lock screen shown when biometric authentication is required.
 * Extracted as its own composable to prevent unnecessary recomposition
 * of MoneyPilotApp when auth state changes.
 */
@Composable
fun BiometricLockScreen(onUnlockClick: () -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Locked",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "MoneyPilot is locked",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Verify your identity to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onUnlockClick) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock")
            }
        }
    }
}

@Composable
fun MoneyPilotApp(
    analyticsHelper: AnalyticsHelper,
    mainViewModel: MainViewModel = hiltViewModel(),
    userPreferences: UserPreferences,
) {
    val backStack = rememberNavBackStack(Destination.Auth() as Destination)

    val transactionViewModel: TransactionViewModel = hiltViewModel()
    val budgetViewModel: BudgetViewModel = hiltViewModel()
    val investmentViewModel: InvestmentViewModel = hiltViewModel()
    val analyticsViewModel: AnalyticsViewModel = hiltViewModel()
    val learnFinanceViewModel: LearnFinanceViewModel = hiltViewModel()

    val currentDestination = backStack.lastOrNull()

    val defaultCurrency = stringResource(R.string.default_currency_code)
    val currencyCode by mainViewModel.currency.collectAsState(initial = defaultCurrency)
    val userData by mainViewModel.userData.collectAsState(initial = null)
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState(initial = false)

    val context = androidx.compose.ui.platform.LocalContext.current
    val interstitialAdManager =
        remember {
            prasad.vennam.moneypilot.ads
                .InterstitialAdManager(context)
                .apply { loadAd() }
        }
    val workManager = remember { androidx.work.WorkManager.getInstance(context) }
    val workInfos by workManager
        .getWorkInfosForUniqueWorkFlow(prasad.vennam.moneypilot.util.GoogleSheetsSyncHelper.SYNC_WORK_NAME)
        .collectAsState(initial = emptyList())
    val restoreState by mainViewModel.restoreState.collectAsState()
    val isSynced by mainViewModel.isSynced.collectAsState()
    val spreadsheetId by mainViewModel.spreadsheetId.collectAsState()
    val isPremium by mainViewModel.isPremium.collectAsState()
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
            isLoggedIn &&
                    currentDestination !is Destination.Auth &&
                    currentDestination !is Destination.ReceiptScanner &&
                    currentDestination !is Destination.AiChat &&
                    currentDestination !is Destination.Insights &&
                    currentDestination !is Destination.TermsOfService &&
                    currentDestination !is Destination.PrivacyPolicy &&
                    currentDestination !is Destination.EmiCalculator &&
                    currentDestination !is Destination.LearnFinance

        NavigationSuiteScaffold(
            layoutType =
                if (showNavigation) {
                    NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
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
                        selected = currentDestination is Destination.History,
                        onClick = {
                            backStack.clear()
                            backStack.add(Destination.History)
                        },
                        icon = {
                            Icon(
                                Icons.Rounded.History,
                                contentDescription = stringResource(R.string.transactions),
                            )
                        },
                        label = { Text(stringResource(R.string.transactions)) },
                        alwaysShowLabel = currentDestination is Destination.History,
                    )
                    item(
                        selected = currentDestination is Destination.Loans,
                        onClick = {
                            backStack.clear()
                            backStack.add(Destination.Loans())
                        },
                        icon = {
                            Icon(
                                Icons.Rounded.AccountBalance,
                                contentDescription = stringResource(R.string.loans),
                            )
                        },
                        label = { Text(stringResource(R.string.loans)) },
                        alwaysShowLabel = currentDestination is Destination.Loans,
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
                                    onNavigateToTerms = {
                                        backStack.add(Destination.TermsOfService)
                                    },
                                    onNavigateToPrivacy = {
                                        backStack.add(Destination.PrivacyPolicy)
                                    },
                                    onAuthSuccess = {
                                        backStack.clear()
                                        backStack.add(Destination.Dashboard)
                                    },
                                )
                            }

                        is Destination.Dashboard ->
                            NavEntry(key) {
                                DashboardScreen(
                                    mainViewModel = mainViewModel,
                                    onNavigateToAddTransaction = { type ->
                                        backStack.add(Destination.AddEditTransaction(initialType = type))
                                    },
                                    onNavigateToAddInvestment = {
                                        backStack.add(Destination.Investments)
                                    },
                                    onNavigateToHistory = {
                                        backStack.add(Destination.History)
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
                                        backStack.add(Destination.Loans())
                                    },
                                    onNavigateToInsights = {
                                        backStack.add(Destination.Insights)
                                    },
                                    onNavigateToAiChat = {
                                        backStack.add(Destination.AiChat)
                                    },
                                    onNavigateToEmergencyFund = {
                                        backStack.add(Destination.EmergencyFund)
                                    },
                                    onNavigateToNews = {
                                        backStack.add(Destination.FinancialNews)
                                    },
                                    onNavigateToSandbox = {
                                        backStack.add(Destination.FinancialSandbox)
                                    },
                                    onNavigateToEmiCalculator = {
                                        if (isPremium) {
                                            backStack.add(Destination.EmiCalculator)
                                        } else {
                                            (context as? android.app.Activity)?.let { activity ->
                                                interstitialAdManager.showAd(activity) {
                                                    backStack.add(Destination.EmiCalculator)
                                                }
                                            } ?: backStack.add(Destination.EmiCalculator)
                                        }
                                    },
                                    onNavigateToLearnFinance = {
                                        backStack.add(Destination.LearnFinance)
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

                        is Destination.History ->
                            NavEntry(key) {
                                HistoryScreen(
                                    viewModel = transactionViewModel,
                                    onAddTransaction = { type ->
                                        backStack.add(Destination.AddEditTransaction(initialType = type))
                                    },
                                    onEditTransaction = { id, type ->
                                        backStack.add(Destination.AddEditTransaction(id, type))
                                    },
                                    userData = userData,
                                    syncState = syncState,
                                    isPremium = isPremium,
                                    onProfileClick = { backStack.add(Destination.Settings) },
                                    fixedType = null,
                                )
                            }

                        is Destination.AddEditTransaction ->
                            NavEntry(key) {
                                AddEditTransactionScreen(
                                    transactionId = key.transactionId,
                                    initialType = key.initialType,
                                    viewModel = transactionViewModel,
                                    analyticsHelper = analyticsHelper,
                                    interstitialAdManager = interstitialAdManager,
                                    isPremium = isPremium,
                                    onNavigateBack = { backStack.removeLastOrNull() },
                                )
                            }

                        is Destination.Investments ->
                            NavEntry(key) {
                                InvestmentScreen(
                                    viewModel = investmentViewModel,
                                    userData = userData,
                                    syncState = syncState,
                                    isPremium = isPremium,
                                    onProfileClick = { backStack.add(Destination.Settings) },
                                )
                            }

                        is Destination.Loans ->
                            NavEntry(key) {
                                LoanScreen(
                                    userData = userData,
                                    syncState = syncState,
                                    isPremium = isPremium,
                                    onProfileClick = { backStack.add(Destination.Settings) },
                                    onNavigateToEmiCalculator = {
                                        if (isPremium) {
                                            backStack.add(Destination.EmiCalculator)
                                        } else {
                                            (context as? android.app.Activity)?.let { activity ->
                                                interstitialAdManager.showAd(activity) {
                                                    backStack.add(Destination.EmiCalculator)
                                                }
                                            } ?: backStack.add(Destination.EmiCalculator)
                                        }
                                    },
                                    prefillAmount = key.prefillAmount,
                                    prefillRate = key.prefillRate,
                                    prefillTenureMonths = key.prefillTenureMonths,
                                    prefillEmi = key.prefillEmi,
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
                                    isPremium = isPremium,
                                    onProfileClick = { backStack.add(Destination.Settings) },
                                )
                            }

                        is Destination.Settings ->
                            NavEntry(key) {
                                SettingsScreen(
                                    transactionViewModel = transactionViewModel,
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
                                    onNavigateToTerms = {
                                        backStack.add(Destination.TermsOfService)
                                    },
                                    onNavigateToPrivacy = {
                                        backStack.add(Destination.PrivacyPolicy)
                                    },
                                    onNavigateToPremium = {
                                        backStack.add(Destination.PremiumScreen)
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
                                    onNavigateToWeb = { url, title ->
                                        backStack.add(Destination.NewsWebFrame(url = url, title = title))
                                    },
                                )
                            }

                        is Destination.FAQ ->
                            NavEntry(key) {
                                FaqScreen(
                                    onNavigateBack = { backStack.removeLastOrNull() },
                                )
                            }

                        is Destination.EmergencyFund ->
                            NavEntry(key) {
                                EmergencyFundScreen(
                                    userPreferences = userPreferences,
                                    onNavigateBack = { backStack.removeLastOrNull() },
                                )
                            }

                        is Destination.TermsOfService ->
                            NavEntry(key) {
                                NewsWebViewScreen(
                                    url = AppLinks.TERMS,
                                    title = "Terms of Service",
                                    showBookmark = false,
                                    onBack = { backStack.removeLastOrNull() },
                                )
                            }

                        is Destination.PrivacyPolicy ->
                            NavEntry(key) {
                                NewsWebViewScreen(
                                    url = AppLinks.PRIVACY_POLICY,
                                    title = "Privacy Policy",
                                    showBookmark = false,
                                    onBack = { backStack.removeLastOrNull() },
                                )
                            }

                        is Destination.FinancialNews ->
                            NavEntry(key) {
                                NewsScreen(
                                    onBack = { backStack.removeLastOrNull() },
                                    onNavigateToWeb = { url, title ->
                                        backStack.add(Destination.NewsWebFrame(url = url, title = title))
                                    },
                                )
                            }

                        is Destination.NewsWebFrame ->
                            NavEntry(key) {
                                NewsWebViewScreen(
                                    url = key.url,
                                    title = key.title,
                                    onBack = { backStack.removeLastOrNull() },
                                )
                            }

                        is Destination.FinancialSandbox ->
                            NavEntry(key) {
                                prasad.vennam.moneypilot.ui.sandbox.FinancialSandboxScreen(
                                    onBack = { backStack.removeLastOrNull() },
                                )
                            }

                        is Destination.EmiCalculator ->
                            NavEntry(key) {
                                EmiCalculatorScreen(
                                    onBack = { backStack.removeLastOrNull() },
                                    isPremium = isPremium,
                                    onNavigateToSaveLoan = { amount, rate, months, emi ->
                                        backStack.add(
                                            Destination.Loans(
                                                prefillAmount = amount,
                                                prefillRate = rate,
                                                prefillTenureMonths = months,
                                                prefillEmi = emi,
                                            ),
                                        )
                                    },
                                    onNavigateToCompare = {
                                    },
                                )
                            }

                        is Destination.LearnFinance ->
                            NavEntry(key) {
                                LearnFinanceScreen(
                                    viewModel = learnFinanceViewModel,
                                    onBack = { backStack.removeLastOrNull() },
                                    onArticleClick = { articleId ->
                                        backStack.add(Destination.ArticleDetail(articleId))
                                    },
                                    isPremium = isPremium
                                )
                            }

                        is Destination.ArticleDetail ->
                            NavEntry(key) {
                                ArticleDetailScreen(
                                    articleId = key.articleId,
                                    viewModel = learnFinanceViewModel,
                                    onBack = { backStack.removeLastOrNull() },
                                    onArticleClick = { articleId ->
                                        backStack.add(Destination.ArticleDetail(articleId))
                                    }
                                )
                            }

                        is Destination.PremiumScreen ->
                            NavEntry(key) {
                                prasad.vennam.moneypilot.ui.premium.PremiumScreen(
                                    onBackClick = { backStack.removeLastOrNull() },
                                )
                            }

                        else -> error("Unknown destination: $key")
                    }
                },
            )
        }
    }
}
