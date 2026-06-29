package prasad.vennam.moneypilot.ui.dashboard

import prasad.vennam.moneypilot.ui.components.BaseBottomSheet
import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.PendingTransaction
import prasad.vennam.moneypilot.data.entity.TimeFrame
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.ui.budget.utils.getCategoryIcon
import prasad.vennam.moneypilot.ui.components.AdBannerView
import prasad.vennam.moneypilot.ui.dashboard.components.BudgetProgressSection
import prasad.vennam.moneypilot.ui.dashboard.components.CategoryBreakdownBottomSheet
import prasad.vennam.moneypilot.ui.dashboard.components.DashboardTopBar
import prasad.vennam.moneypilot.ui.dashboard.components.ExpenseChartCard
import prasad.vennam.moneypilot.ui.dashboard.components.KPISection
import prasad.vennam.moneypilot.ui.dashboard.components.LearnFinancePromoCard
import prasad.vennam.moneypilot.ui.dashboard.components.LoanSection
import prasad.vennam.moneypilot.ui.dashboard.components.PaymentAppsSection
import prasad.vennam.moneypilot.ui.dashboard.components.QuickActionSection
import prasad.vennam.moneypilot.ui.dashboard.components.RecentTransactionsCard
import prasad.vennam.moneypilot.ui.dashboard.components.SectionHeader
import prasad.vennam.moneypilot.ui.settings.LoginRequiredDialog
import prasad.vennam.moneypilot.ui.viewmodel.DashboardViewModel
import prasad.vennam.moneypilot.ui.viewmodel.MainViewModel
import prasad.vennam.moneypilot.ui.viewmodel.NotificationViewModel
import prasad.vennam.moneypilot.ui.viewmodel.RestoreState
import prasad.vennam.moneypilot.ui.viewmodel.SavingGoalViewModel
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.GoogleSheetsSyncHelper
import prasad.vennam.moneypilot.util.TrackScreen
import prasad.vennam.moneypilot.util.toMajorUnit
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    mainViewModel: MainViewModel,
    analyticsHelper: AnalyticsHelper,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToAddTransaction: (TransactionType) -> Unit,
    onNavigateToAddInvestment: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToEmergencyFund: () -> Unit,
    onNavigateToSavingGoals: () -> Unit,
    onNavigateToNews: () -> Unit,
    onNavigateToSandbox: () -> Unit,
    onNavigateToEmiCalculator: () -> Unit,
    onNavigateToLearnFinance: () -> Unit,
    onNavigateToCurrencyConverter: () -> Unit,
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.DASHBOARD)

    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val savingGoalViewModel: SavingGoalViewModel = hiltViewModel()
    val savingGoals by savingGoalViewModel.allSavingGoals.collectAsState()
    val userData by mainViewModel.userData.collectAsState()
    val isPremium by mainViewModel.isPremium.collectAsState()
    val isDevToolEnabled by mainViewModel.isDevToolEnabled.collectAsState()

    val lazyListState = rememberLazyListState()
    var isFabVisible by remember { mutableStateOf(true) }
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset) {
        val currentIndex = lazyListState.firstVisibleItemIndex
        val currentOffset = lazyListState.firstVisibleItemScrollOffset
        if (currentIndex == 0 && currentOffset == 0) {
            isFabVisible = true
        } else if (currentIndex > previousIndex || (currentIndex == previousIndex && currentOffset > previousOffset)) {
            isFabVisible = false
        } else if (currentIndex < previousIndex || (currentOffset < previousOffset)) {
            isFabVisible = true
        }
        previousIndex = currentIndex
        previousOffset = currentOffset
    }
    val isSynced by mainViewModel.isSynced.collectAsState()
    val spreadsheetId by mainViewModel.spreadsheetId.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var showLoginRequiredDialog by remember { mutableStateOf(false) }

    val restoreState by mainViewModel.restoreState.collectAsState()
    val currencyCode by mainViewModel.currency.collectAsState()

    val notificationViewModel: NotificationViewModel = hiltViewModel()
    val notifications by notificationViewModel.notifications.collectAsState()
    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    val authLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                mainViewModel.checkAndPerformRestore(context)
            }
        }

    LaunchedEffect(restoreState) {
        when (val state = restoreState) {
            is RestoreState.Success -> {
                Toast.makeText(context, context.run { getString(R.string.backup_successfully_restored) }, Toast.LENGTH_LONG).show()
                mainViewModel.resetRestoreCheck()
            }

            is RestoreState.NeedAuthorization -> {
                authLauncher.launch(state.intent)
            }

            is RestoreState.Error -> {
                Toast.makeText(context, context.run { getString(R.string.restore_failed, state.message) }, Toast.LENGTH_LONG).show()
                mainViewModel.resetRestoreCheck()
            }

            else -> {}
        }
    }

    LaunchedEffect(userData) {
        val uData = userData
        if (uData != null && uData.email != "guest@moneypilot.app" && spreadsheetId == null) {
            mainViewModel.checkAndPerformRestore(context)
        }
    }

    val triggerGoogleLogin =
        remember(context) {
            {
                scope.launch {
                    try {
                        val googleIdOption =
                            GetGoogleIdOption
                                .Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(prasad.vennam.moneypilot.BuildConfig.GOOGLE_CLIENT_ID)
                                .setAutoSelectEnabled(true)
                                .build()

                        val request =
                            GetCredentialRequest
                                .Builder()
                                .addCredentialOption(googleIdOption)
                                .build()

                        val result =
                            credentialManager.getCredential(
                                request = request,
                                context = context,
                            )

                        val credential = result.credential
                        val googleIdTokenCredential =
                            try {
                                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    GoogleIdTokenCredential.createFrom(credential.data)
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                null
                            }

                        if (googleIdTokenCredential != null) {
                            analyticsHelper.logEvent(AnalyticsConstants.Event.LOGIN, mapOf(AnalyticsConstants.Param.METHOD to "google"))
                            mainViewModel.saveUserData(
                                prasad.vennam.moneypilot.data.UserPreferences.UserData(
                                    name = googleIdTokenCredential.displayName ?: "User",
                                    email = googleIdTokenCredential.id,
                                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                                ),
                            ) {
                                mainViewModel.checkAndPerformRestore(context)
                                showLoginRequiredDialog = false
                            }
                        }
                    } catch (e: NoCredentialException) {
                        Log.e("DashboardScreen", "Login failed: No credentials available", e)
                        Toast.makeText(context, "No Google accounts found on this device. Please sign in to a Google account in Settings.", Toast.LENGTH_LONG).show()
                    } catch (e: GetCredentialCancellationException) {
                        Log.d("DashboardScreen", "Login cancelled by user")
                    } catch (e: GetCredentialException) {
                        Log.e("DashboardScreen", "Login failed: ${e.message}")
                        Toast.makeText(context, with(context) { getString(R.string.login_failed_formatted, e.message) }, Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Log.e("DashboardScreen", "Error: ${e.message}")
                        Toast.makeText(context, with(context) { getString(R.string.error_formatted, e.message) }, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    val workManager = remember { WorkManager.getInstance(context) }
    val workInfos by workManager.getWorkInfosForUniqueWorkFlow(GoogleSheetsSyncHelper.SYNC_WORK_NAME).collectAsState(initial = emptyList())

    val isGuest = remember(userData) { userData?.email == "guest@moneypilot.app" }
    val syncState =
        remember(isSynced, workInfos, isGuest, restoreState) {
            if (isGuest) {
                null
            } else if (restoreState is RestoreState.Checking) {
                SyncState.SYNCING
            } else if (!isSynced || spreadsheetId == null) {
                SyncState.PENDING_CONNECTION
            } else {
                val workInfo = workInfos.firstOrNull()
                when (workInfo?.state) {
                    WorkInfo.State.RUNNING -> SyncState.SYNCING
                    WorkInfo.State.FAILED -> SyncState.FAILED
                    else -> SyncState.SYNCED
                }
            }
        }

    var showBreakdownSheet by remember { mutableStateOf(false) }
    var showPendingReviewSheet by remember { mutableStateOf(false) }

    LaunchedEffect(dashboardState.pendingTransactions) {
        if (dashboardState.pendingTransactions.isEmpty()) {
            showPendingReviewSheet = false
        }
    }

    val unknownString = stringResource(R.string.unknown)

    // Financial Health: Track budget alerts
    LaunchedEffect(dashboardState.budgetProgresses) {
        dashboardState.budgetProgresses.forEach { progressItem ->
            if (progressItem.budget.amount > 0 && progressItem.progress >= 0.9f) {
                val categoryName = progressItem.category?.name ?: unknownString
                analyticsHelper.logEvent(
                    AnalyticsConstants.Event.BUDGET_WARNING_VIEWED,
                    mapOf(
                        AnalyticsConstants.Param.CATEGORY to categoryName,
                        AnalyticsConstants.Param.PERCENT to (progressItem.progress * 100).toInt(),
                    ),
                )
            }
        }
    }

    val pendingPulseTransition = rememberInfiniteTransition(label = "pending_pulse")
    val pendingPulseAlpha by pendingPulseTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse_alpha",
    )
    val bannerBrush =
        Brush.linearGradient(
            colors =
                listOf(
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.primary,
                ),
        )

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val outline = MaterialTheme.colorScheme.outline
    val scrim = MaterialTheme.colorScheme.scrim
    val inversePrimary = MaterialTheme.colorScheme.inversePrimary

    val chartColors =
        remember(
            primary,
            secondary,
            tertiary,
            error,
            primaryContainer,
            secondaryContainer,
            tertiaryContainer,
            outline,
            scrim,
            inversePrimary,
        ) {
            listOf(
                primary,
                secondary,
                tertiary,
                error,
                primaryContainer,
                secondaryContainer,
                tertiaryContainer,
                outline,
                scrim,
                inversePrimary,
            )
        }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                DashboardTopBar(
                    userData = userData,
                    syncState = syncState,
                    unreadCount = unreadCount,
                    onProfileClick = {
                        analyticsHelper.logEvent(AnalyticsConstants.Event.PROFILE_CLICKED)
                        onNavigateToSettings()
                    },
                    onNotificationClick = {
                        analyticsHelper.logEvent(AnalyticsConstants.Event.NOTIFICATION_ICON_CLICKED)
                        if (isGuest) {
                            showLoginRequiredDialog = true
                        } else {
                            onNavigateToNotifications()
                        }
                    },
                    showAiChat = !isFabVisible,
                    onAiChatClick = {
                        analyticsHelper.logEvent(AnalyticsConstants.Event.AI_CHAT_TOPBAR_CLICKED)
                        onNavigateToAiChat()
                    },
                )
            },
        ) { innerPadding ->
            if (showLoginRequiredDialog) {
                LoginRequiredDialog(
                    featureName = "Notifications",
                    onDismiss = { showLoginRequiredDialog = false },
                    onLoginClick = { triggerGoogleLogin() },
                )
            }
            if (syncState == SyncState.SYNCING || restoreState is RestoreState.Checking || dashboardState.isLoading) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                ) {
                    DashboardShimmer()
                }
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                ) {
                    // Sticky banner — stays pinned at top while content scrolls below
                    AdBannerView(
                        isPremium = isPremium,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        item {
                            TimeFrameSelector(
                                selectedTimeFrame = dashboardState.selectedTimeFrame,
                                onTimeFrameSelected = { dashboardViewModel.setTimeFrame(it) },
                            )
                        }

                        if (dashboardState.pendingTransactions.isNotEmpty()) {
                            item {
                                val count = dashboardState.pendingTransactions.size
                                Card(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { showPendingReviewSheet = true }
                                            .graphicsLayer {
                                                alpha = pendingPulseAlpha
                                            },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                ) {
                                    Row(
                                        modifier =
                                            Modifier
                                                .background(bannerBrush)
                                                .padding(horizontal = 20.dp, vertical = 14.dp)
                                                .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp),
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.auto_detected_transactions),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                            )
                                            Text(
                                                text =
                                                    if (count ==
                                                        1
                                                    ) {
                                                        stringResource(R.string.pending_transaction_single)
                                                    } else {
                                                        stringResource(R.string.pending_transactions_plural, count)
                                                    },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.85f),
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Rounded.ChevronRight,
                                            contentDescription = stringResource(R.string.review),
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            KPISection(
                                today = dashboardState.todayExpense,
                                periodExp = dashboardState.periodExpense,
                                periodInc = dashboardState.periodIncome,
                                savings = dashboardState.savings,
                                investment = dashboardState.totalInvestment,
                                currentInvestmentValue = dashboardState.currentInvestmentValue,
                                timeFrame = dashboardState.selectedTimeFrame,
                            )
                        }

                        item {
                            QuickActionSection(
                                onAddExpense = {
                                    analyticsHelper.logEvent(AnalyticsConstants.Event.QUICK_ACTION_CLICKED, mapOf(AnalyticsConstants.Param.ACTION to "add_expense"))
                                    onNavigateToAddTransaction(TransactionType.EXPENSE)
                                },
                                onAddIncome = {
                                    analyticsHelper.logEvent(AnalyticsConstants.Event.QUICK_ACTION_CLICKED, mapOf(AnalyticsConstants.Param.ACTION to "add_income"))
                                    onNavigateToAddTransaction(TransactionType.INCOME)
                                },
                                onAddInvestment = {
                                    analyticsHelper.logEvent(AnalyticsConstants.Event.QUICK_ACTION_CLICKED, mapOf(AnalyticsConstants.Param.ACTION to "add_investment"))
                                    onNavigateToAddInvestment()
                                },
                                onAddLoan = {
                                    analyticsHelper.logEvent(AnalyticsConstants.Event.QUICK_ACTION_CLICKED, mapOf(AnalyticsConstants.Param.ACTION to "add_loan"))
                                    onNavigateToLoans()
                                },
                                onScanReceipt = {
                                    analyticsHelper.logEvent(AnalyticsConstants.Event.QUICK_ACTION_CLICKED, mapOf(AnalyticsConstants.Param.ACTION to "scan_receipt"))
                                    onNavigateToScanner()
                                },
                                onNavigateToEmergencyFund = {
                                    analyticsHelper.logEvent(AnalyticsConstants.Event.QUICK_ACTION_CLICKED, mapOf(AnalyticsConstants.Param.ACTION to "emergency_fund"))
                                    onNavigateToEmergencyFund()
                                },
                                onNavigateToNews = {
                                    analyticsHelper.logEvent(AnalyticsConstants.Event.QUICK_ACTION_CLICKED, mapOf(AnalyticsConstants.Param.ACTION to "news"))
                                    onNavigateToNews()
                                },
                                onNavigateToSandbox = {
                                    analyticsHelper.logEvent(AnalyticsConstants.Event.QUICK_ACTION_CLICKED, mapOf(AnalyticsConstants.Param.ACTION to "sandbox"))
                                    onNavigateToSandbox()
                                },
                                onNavigateToEmiCalculator = {
                                    analyticsHelper.logEvent(AnalyticsConstants.Event.QUICK_ACTION_CLICKED, mapOf(AnalyticsConstants.Param.ACTION to "emi_calculator"))
                                    onNavigateToEmiCalculator()
                                },
                                onNavigateToCurrencyConverter = {
                                    analyticsHelper.logEvent(AnalyticsConstants.Event.QUICK_ACTION_CLICKED, mapOf(AnalyticsConstants.Param.ACTION to "currency_converter"))
                                    onNavigateToCurrencyConverter()
                                },
                                isGuest = isGuest && !prasad.vennam.moneypilot.BuildConfig.DEBUG && !isDevToolEnabled,
                            )
                        }

                        item {
                            SmartInsightsCard(onClick = {
                                analyticsHelper.logEvent(AnalyticsConstants.Event.INSIGHTS_CARD_CLICKED)
                                onNavigateToInsights()
                            })
                        }

                        // Learn Finance promotional banner — shown after Smart Insights
                        if (dashboardState.isLearnFinanceEnabled) {
                            item {
                                LearnFinancePromoCard(onClick = {
                                    analyticsHelper.logEvent(AnalyticsConstants.Event.LEARN_FINANCE_PROMO_CLICKED)
                                    onNavigateToLearnFinance()
                                })
                            }
                        }

                        item {
                            DashboardEmergencyFundCard(
                                emergencyFund = dashboardState.emergencyFund,
                                currencyCode = currencyCode,
                                onClick = {
                                    analyticsHelper.logEvent(AnalyticsConstants.Event.EMERGENCY_FUND_CARD_CLICKED)
                                    onNavigateToEmergencyFund()
                                },
                            )
                        }

                        item {
                            DashboardSavingGoalsCard(
                                savingGoals = savingGoals,
                                currencyCode = currencyCode,
                                onClick = {
                                    analyticsHelper.logEvent(AnalyticsConstants.Event.SAVING_GOALS_CARD_CLICKED)
                                    onNavigateToSavingGoals()
                                },
                            )
                        }

                        item {
                            PaymentAppsSection(currencyCode = currencyCode)
                        }

                        if (dashboardState.loans.isNotEmpty()) {
                            item {
                                LoanSection(
                                    loans = dashboardState.loans,
                                    currencyCode = currencyCode,
                                    onViewAll = onNavigateToLoans,
                                )
                            }
                        }

                        if (dashboardState.spendingByCategory.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = stringResource(R.string.expense_breakdown),
                                    onInfoClick =
                                        if (dashboardState.spendingByCategory.size > 10) {
                                            { showBreakdownSheet = true }
                                        } else {
                                            null
                                        },
                                )
                                Spacer(modifier = Modifier.size(12.dp))
                                ExpenseChartCard(dashboardState.spendingByCategory, chartColors, stringResource(R.string.other))
                            }
                        }

                        if (dashboardState.budgetProgresses.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = stringResource(R.string.budget_progress),
                                    onActionClick = {
                                        analyticsHelper.logEvent(AnalyticsConstants.Event.BUDGET_SEE_ALL_CLICKED)
                                        onNavigateToBudgets()
                                    },
                                )
                                BudgetProgressSection(dashboardState.budgetProgresses, unknownString)
                            }
                        }

                        if (dashboardState.recentTransactions.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = stringResource(R.string.recent_transactions),
                                    onActionClick = {
                                        analyticsHelper.logEvent(AnalyticsConstants.Event.HISTORY_SEE_ALL_CLICKED)
                                        onNavigateToHistory()
                                    },
                                )
                                RecentTransactionsCard(dashboardState.recentTransactions, dashboardState.categories)
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    } // end LazyColumn
                } // end Column
            } // end else
        }

        // Floating AI Bot Icon with Animation
        AnimatedVisibility(
            visible = isFabVisible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 32.dp, end = 24.dp),
        ) {
            FloatingAiBot(
                modifier = Modifier,
                onClick = {
                    analyticsHelper.logEvent(AnalyticsConstants.Event.FLOATING_AI_BOT_CLICKED)
                    onNavigateToAiChat()
                },
            )
        }
    }

    if (showBreakdownSheet) {
        CategoryBreakdownBottomSheet(
            spendingByCategory = dashboardState.spendingByCategory,
            colors = chartColors,
            unknownString = stringResource(R.string.other),
            onDismiss = { showBreakdownSheet = false },
        )
    }

    if (showPendingReviewSheet) {
        PendingReviewBottomSheet(
            pendingTransactions = dashboardState.pendingTransactions,
            categories = dashboardState.categories,
            currencyCode = currencyCode,
            onApprove = { pending, categoryId, note ->
                dashboardViewModel.approveTransaction(pending, categoryId, note)
            },
            onDismiss = { pending ->
                dashboardViewModel.dismissTransaction(pending)
            },
            onDismissRequest = { showPendingReviewSheet = false },
        )
    }
}

@Composable
fun SmartInsightsCard(onClick: () -> Unit) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                            ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.smart_ai_insights),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.ai_insights_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Rounded.AutoAwesome, // Use a ChevronRight if you want, but sticking to theme
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun FloatingAiBot(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "scale",
    )

    Surface(
        modifier =
            modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.clickable { onClick() },
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 12.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.secondary,
                            ),
                        ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = stringResource(R.string.ai_assistant),
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
fun DashboardEmergencyFundCard(
    emergencyFund: prasad.vennam.moneypilot.data.entity.EmergencyFund?,
    currencyCode: String,
    onClick: () -> Unit,
) {
    val isConfigured = emergencyFund != null && emergencyFund.monthlyExpenses > 0.0

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        if (!isConfigured) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.build_safety_net_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.safety_net_setup_prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            val monthly = emergencyFund.monthlyExpenses
            val months = emergencyFund.targetMonths
            val saved = emergencyFund.currentSaved

            val targetGoal = remember(monthly, months) { monthly * months }
            val progress =
                remember(saved, targetGoal) {
                    if (targetGoal > 0.0) (saved / targetGoal).toFloat().coerceIn(0f, 1f) else 0f
                }
            val percent = remember(progress) { (progress * 100).toInt() }
            val savedFormatted =
                remember(saved, currencyCode) {
                    CurrencyFormatter.format(saved, currencyCode)
                }
            val targetGoalFormatted =
                remember(targetGoal, currencyCode) {
                    CurrencyFormatter.format(targetGoal, currencyCode)
                }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.emergency_safety_net),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = stringResource(R.string.percentage_format, percent),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.saved_with_value, savedFormatted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.goal_with_value, targetGoalFormatted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardSavingGoalsCard(
    savingGoals: List<prasad.vennam.moneypilot.data.entity.SavingGoal>,
    currencyCode: String,
    onClick: () -> Unit,
) {
    val totalGoals = savingGoals.size
    val completedGoals = savingGoals.count { it.isCompleted }

    val totalSaved = savingGoals.sumOf { it.currentSavedAmount }
    val totalTarget = savingGoals.sumOf { it.targetAmount }

    val progress =
        remember(totalSaved, totalTarget) {
            if (totalTarget > 0L) (totalSaved.toDouble() / totalTarget.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
        }
    val percent = remember(progress) { (progress * 100).toInt() }

    val savedFormatted =
        remember(totalSaved, currencyCode) {
            CurrencyFormatter.format(totalSaved.toMajorUnit, currencyCode)
        }
    val targetFormatted =
        remember(totalTarget, currencyCode) {
            CurrencyFormatter.format(totalTarget.toMajorUnit, currencyCode)
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        if (totalGoals == 0) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrackChanges,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.create_savings_goals),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.savings_goals_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.TrackChanges,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.savings_goals_count, completedGoals, totalGoals),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = stringResource(R.string.percentage_format, percent),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.saved_amount_format, savedFormatted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.target_amount_format, targetFormatted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun TimeFrameSelector(
    selectedTimeFrame: TimeFrame,
    onTimeFrameSelected: (TimeFrame) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = CircleShape)
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TimeFrame.entries.forEach { timeFrame ->
            val isSelected = timeFrame == selectedTimeFrame
            val label =
                when (timeFrame) {
                    TimeFrame.MONTHLY -> stringResource(R.string.monthly)
                    TimeFrame.QUARTERLY -> stringResource(R.string.quarterly)
                    TimeFrame.YEARLY -> stringResource(R.string.yearly)
                }

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "chip_bg",
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "chip_text",
            )

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .clickable { onTimeFrameSelected(timeFrame) }
                        .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingReviewBottomSheet(
    pendingTransactions: List<PendingTransaction>,
    categories: List<Category>,
    currencyCode: String,
    onApprove: (PendingTransaction, Long?, String) -> Unit,
    onDismiss: (PendingTransaction) -> Unit,
    onDismissRequest: () -> Unit,
) {


    val selectedCategoryMap = remember { mutableStateMapOf<Long, Category?>() }
    val noteTextMap = remember { mutableStateMapOf<Long, String>() }
    val expandedRawMessageMap = remember { mutableStateMapOf<Long, Boolean>() }

    BaseBottomSheet(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.review_auto_detected_transactions),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.verify_pending_transactions_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                items(pendingTransactions, key = { it.id }) { pending ->
                    LaunchedEffect(pending.id) {
                        if (!selectedCategoryMap.containsKey(pending.id)) {
                            selectedCategoryMap[pending.id] = autoMatchCategory(pending.merchant, pending.type, categories)
                        }
                        if (!noteTextMap.containsKey(pending.id)) {
                            noteTextMap[pending.id] = pending.merchant
                        }
                    }

                    val chosenCategory = selectedCategoryMap[pending.id]
                    val currentNote = noteTextMap[pending.id] ?: ""
                    val isRawExpanded = expandedRawMessageMap[pending.id] ?: false

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            ),
                        border =
                            androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                            ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val dateStr =
                                    remember(pending.timestamp) {
                                        val cal = Calendar.getInstance().apply { timeInMillis = pending.timestamp }
                                        android.text.format.DateFormat
                                            .format("dd MMM, hh:mm a", cal)
                                            .toString()
                                    }
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                val amountFormatted =
                                    remember(pending.amount, currencyCode) {
                                        CurrencyFormatter.format(pending.amount, currencyCode)
                                    }
                                val amountColor =
                                    if (pending.type == "EXPENSE") {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                Text(
                                    text = amountFormatted,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = amountColor,
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = currentNote,
                                onValueChange = { noteTextMap[pending.id] = it },
                                label = { Text(stringResource(R.string.merchant_note_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyMedium,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            CategorySelectorDropdown(
                                selectedCategory = chosenCategory,
                                categories = categories.filter { it.isExpense == (pending.type == "EXPENSE") },
                                onCategorySelected = { selectedCategoryMap[pending.id] = it },
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (pending.bankAccount.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.source_formatted, pending.bankAccount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedRawMessageMap[pending.id] = !isRawExpanded }
                                        .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.show_raw_notification_text),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isRawExpanded) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }

                            if (isRawExpanded) {
                                Text(
                                    text = pending.rawMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier =
                                        Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(8.dp),
                                            ).padding(8.dp)
                                            .fillMaxWidth(),
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onDismiss(pending) },
                                    modifier = Modifier.weight(1f),
                                    colors =
                                        ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error,
                                        ),
                                    border =
                                        androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.dismiss))
                                }

                                Button(
                                    onClick = { onApprove(pending, chosenCategory?.id, currentNote) },
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.approve))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectorDropdown(
    selectedCategory: Category?,
    categories: List<Category>,
    onCategorySelected: (Category) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedCategory?.name ?: stringResource(R.string.select_category),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.category)) },
            leadingIcon = {
                Icon(
                    imageVector =
                        if (selectedCategory != null) {
                            getCategoryIcon(selectedCategory.iconName)
                        } else {
                            Icons.Rounded.Category
                        },
                    contentDescription = null,
                    tint =
                        if (selectedCategory != null) {
                            Color(selectedCategory.color)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    modifier = Modifier.size(24.dp),
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .clickable { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = getCategoryIcon(category.iconName),
                            contentDescription = null,
                            tint = Color(category.color),
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    },
                )
            }
        }
    }
}

fun autoMatchCategory(
    merchant: String,
    type: String,
    categories: List<Category>,
): Category? {
    val nameLower = merchant.lowercase()
    val isExpense = type == "EXPENSE"

    val targetCategoryName =
        if (isExpense) {
            when {
                nameLower.contains("uber") ||
                    nameLower.contains("ola") ||
                    nameLower.contains("lyft") ||
                    nameLower.contains("taxi") ||
                    nameLower.contains("metro") ||
                    nameLower.contains("rail") ||
                    nameLower.contains("train") ||
                    nameLower.contains("fuel") ||
                    nameLower.contains("petrol") ||
                    nameLower.contains("diesel") ||
                    nameLower.contains("gas station") -> "Transport"

                nameLower.contains("starbucks") ||
                    nameLower.contains("swiggy") ||
                    nameLower.contains("zomato") ||
                    nameLower.contains("ubereats") ||
                    nameLower.contains("food") ||
                    nameLower.contains("restaurant") ||
                    nameLower.contains("cafe") ||
                    nameLower.contains("dining") ||
                    nameLower.contains("pizza") ||
                    nameLower.contains("mcdonald") ||
                    nameLower.contains("burger") -> "Food"

                nameLower.contains("netflix") ||
                    nameLower.contains("spotify") ||
                    nameLower.contains("youtube") ||
                    nameLower.contains("disney") ||
                    nameLower.contains("prime video") ||
                    nameLower.contains("movie") ||
                    nameLower.contains("cinema") ||
                    nameLower.contains("game") ||
                    nameLower.contains("steam") ||
                    nameLower.contains("epic") ||
                    nameLower.contains("entertainment") -> "Entertainment"

                nameLower.contains("amazon") ||
                    nameLower.contains("flipkart") ||
                    nameLower.contains("myntra") ||
                    nameLower.contains("shopping") ||
                    nameLower.contains("store") ||
                    nameLower.contains("supermarket") ||
                    nameLower.contains("grocery") ||
                    nameLower.contains("walmart") ||
                    nameLower.contains("target") -> "Shopping"

                nameLower.contains("hospital") ||
                    nameLower.contains("clinic") ||
                    nameLower.contains("medical") ||
                    nameLower.contains("pharmacy") ||
                    nameLower.contains("doctor") ||
                    nameLower.contains("dentist") ||
                    nameLower.contains("health") ||
                    nameLower.contains("medicine") -> "Health"

                nameLower.contains("electricity") || nameLower.contains("water bill") || nameLower.contains("power") -> "Utilities"

                nameLower.contains("airtel") ||
                    nameLower.contains("jio") ||
                    nameLower.contains("bill") ||
                    nameLower.contains("recharge") ||
                    nameLower.contains("internet") ||
                    nameLower.contains("wifi") ||
                    nameLower.contains("mobile") -> "Bills"

                nameLower.contains("rent") || nameLower.contains("housing") || nameLower.contains("apartment") -> "Housing"

                nameLower.contains("school") ||
                    nameLower.contains("college") ||
                    nameLower.contains("university") ||
                    nameLower.contains("course") ||
                    nameLower.contains("udemy") ||
                    nameLower.contains("education") -> "Education"

                nameLower.contains("flight") ||
                    nameLower.contains("hotel") ||
                    nameLower.contains("travel") ||
                    nameLower.contains("trip") ||
                    nameLower.contains("booking") ||
                    nameLower.contains("airbnb") -> "Travel"

                nameLower.contains("insurance") || nameLower.contains("lic") -> "Insurance"

                else -> "Food"
            }
        } else {
            when {
                nameLower.contains("salary") || nameLower.contains("payroll") || nameLower.contains("wage") -> "Salary"
                nameLower.contains("freelance") || nameLower.contains("gigs") || nameLower.contains("consulting") -> "Freelance"
                nameLower.contains("interest") ||
                    nameLower.contains("dividend") ||
                    nameLower.contains("mutual fund") ||
                    nameLower.contains("stock") ||
                    nameLower.contains("crypto") ||
                    nameLower.contains("investment") -> "Investments"

                nameLower.contains("rental") || nameLower.contains("tenant") -> "Rental"
                nameLower.contains("gift") || nameLower.contains("present") -> "Gifts"
                nameLower.contains("refund") || nameLower.contains("cashback") || nameLower.contains("reward") -> "Refund"
                else -> "Salary"
            }
        }

    return categories.find { it.name.equals(targetCategoryName, ignoreCase = true) && it.isExpense == isExpense }
        ?: categories.find { it.isExpense == isExpense }
}
