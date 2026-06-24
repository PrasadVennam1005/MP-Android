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
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import prasad.vennam.moneypilot.billing.BillingManager
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.ui.dashboard.SyncState
import prasad.vennam.moneypilot.ui.navigation.Destination
import prasad.vennam.moneypilot.ui.navigation.moneyPilotNavEntry
import prasad.vennam.moneypilot.ui.theme.MoneyPilotTheme
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


        // Schedule timezone-based Daily News Notifications
        prasad.vennam.moneypilot.worker.DailyNewsWorker
            .schedule(applicationContext)

        // Schedule timezone-based Daily Subscription Notifications
        prasad.vennam.moneypilot.worker.SubscriptionReminderWorker
            .schedule(applicationContext)

        // Trigger immediate run for development testing ONLY in debug builds
        if (BuildConfig.DEBUG) {
            androidx.work.WorkManager.getInstance(applicationContext).enqueue(
                androidx.work.OneTimeWorkRequestBuilder<prasad.vennam.moneypilot.worker.DailyNewsWorker>().build(),
            )
            androidx.work.WorkManager.getInstance(applicationContext).enqueue(
                androidx.work.OneTimeWorkRequestBuilder<prasad.vennam.moneypilot.worker.SubscriptionReminderWorker>().build(),
            )
        }

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                mainViewModel.checkLoanReminders()
            }
            val themeMode by mainViewModel.themeMode.collectAsState()
            val isBiometricEnabled by mainViewModel.isBiometricEnabled.collectAsState()
            // isAuthenticated tracks whether user passed biometric check.
            var isAuthenticated by remember { mutableStateOf(false) }
            var lastBackgroundTime by remember { mutableStateOf(0L) }

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                        // Store the timestamp when the app is backgrounded
                        lastBackgroundTime = System.currentTimeMillis()
                    } else if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                        // Require re-authentication only if the app has been in the background for > 1 minute (60s)
                        if (lastBackgroundTime != 0L) {
                            val elapsed = System.currentTimeMillis() - lastBackgroundTime
                            if (elapsed > 60_000L) {
                                isAuthenticated = false
                            }
                            lastBackgroundTime = 0L // Reset
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Auto-show biometric prompt when the screen comes into focus or authentication state changes
            LaunchedEffect(isBiometricEnabled, isAuthenticated) {
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
                // Wrap the main app and the biometric lock screen in a Box overlay structure.
                // This keeps MoneyPilotApp composed in memory, preserving navigation backstack and screen state
                // instead of resetting it back to the home/first screen when locking.
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                    MoneyPilotApp(analyticsHelper, mainViewModel, userPreferences)

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
                    }
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
        val showNavigation =
            isLoggedIn && (
                currentDestination is Destination.Dashboard ||
                currentDestination is Destination.History ||
                currentDestination is Destination.Loans ||
                currentDestination is Destination.Investments ||
                currentDestination is Destination.Reports
            )

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
                   moneyPilotNavEntry(
                        key = key,
                        backStack = backStack,
                        analyticsHelper = analyticsHelper,
                        mainViewModel = mainViewModel,
                        transactionViewModel = transactionViewModel,
                        budgetViewModel = budgetViewModel,
                        investmentViewModel = investmentViewModel,
                        analyticsViewModel = analyticsViewModel,
                        learnFinanceViewModel = learnFinanceViewModel,
                        userPreferences = userPreferences,
                        isPremium = isPremium,
                        userData = userData,
                        syncState = syncState,
                        interstitialAdManager = interstitialAdManager,
                        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
                    )
                },
            )
        }
    }
}
