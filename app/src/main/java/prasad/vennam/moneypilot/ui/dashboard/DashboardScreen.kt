package prasad.vennam.moneypilot.ui.dashboard

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.ui.dashboard.components.*
import prasad.vennam.moneypilot.ui.settings.LoginRequiredDialog
import prasad.vennam.moneypilot.ui.viewmodel.*
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.GoogleSheetsSyncHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    transactionViewModel: TransactionViewModel,
    investmentViewModel: InvestmentViewModel,
    budgetViewModel: BudgetViewModel,
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
) {
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val userData by mainViewModel.userData.collectAsState()
    val isSynced by mainViewModel.isSynced.collectAsState()
    val spreadsheetId by mainViewModel.spreadsheetId.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var showLoginRequiredDialog by remember { mutableStateOf(false) }

    val restoreState by mainViewModel.restoreState.collectAsState()
    val isOnboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsState()
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

    val triggerGoogleLogin = {
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
                    analyticsHelper.logEvent("login", mapOf("method" to "google"))
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
            } catch (e: GetCredentialException) {
                Log.e("DashboardScreen", "Login failed: ${e.message}")
                Toast.makeText(context, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("DashboardScreen", "Error: ${e.message}")
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
    val unknownString = stringResource(R.string.unknown)

    // Financial Health: Track budget alerts
    LaunchedEffect(dashboardState.budgetProgresses) {
        dashboardState.budgetProgresses.forEach { progressItem ->
            if (progressItem.budget.amount > 0 && progressItem.progress >= 0.9f) {
                val categoryName = progressItem.category?.name ?: unknownString
                analyticsHelper.logEvent(
                    "budget_warning_viewed",
                    mapOf(
                        "category" to categoryName,
                        "percent" to (progressItem.progress * 100).toInt(),
                    ),
                )
            }
        }
    }

    val chartColors =
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.outline,
            MaterialTheme.colorScheme.scrim,
            MaterialTheme.colorScheme.inversePrimary,
        )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                DashboardTopBar(
                    userData = userData,
                    syncState = syncState,
                    unreadCount = unreadCount,
                    onProfileClick = onNavigateToSettings,
                    onNotificationClick = {
                        if (isGuest) {
                            showLoginRequiredDialog = true
                        } else {
                            onNavigateToNotifications()
                        }
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
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    item {
                        KPISection(
                            today = dashboardState.todayExpense,
                            monthlyExp = dashboardState.monthlyExpense,
                            monthlyInc = dashboardState.monthlyIncome,
                            savings = dashboardState.savings,
                            investment = dashboardState.totalInvestment,
                            currentInvestmentValue = dashboardState.currentInvestmentValue,
                        )
                    }

                    item {
                        QuickActionSection(
                            onAddExpense = { onNavigateToAddTransaction(TransactionType.EXPENSE) },
                            onAddIncome = { onNavigateToAddTransaction(TransactionType.INCOME) },
                            onAddInvestment = onNavigateToAddInvestment,
                            onAddLoan = onNavigateToLoans,
                            onScanReceipt = onNavigateToScanner,
                            isGuest = isGuest,
                        )
                    }

                    item {
                        SmartInsightsCard(onClick = onNavigateToInsights)
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
                            SectionHeader(stringResource(R.string.budget_progress), onActionClick = onNavigateToBudgets)
                            BudgetProgressSection(dashboardState.budgetProgresses, unknownString)
                        }
                    }

                    if (dashboardState.recentTransactions.isNotEmpty()) {
                        item {
                            SectionHeader(stringResource(R.string.recent_transactions), onActionClick = onNavigateToHistory)
                            RecentTransactionsCard(dashboardState.recentTransactions, dashboardState.categories)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        // Floating AI Bot Icon with Animation
        FloatingAiBot(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 32.dp, end = 24.dp),
            onClick = onNavigateToAiChat,
        )
    }

    if (showBreakdownSheet) {
        CategoryBreakdownBottomSheet(
            spendingByCategory = dashboardState.spendingByCategory,
            colors = chartColors,
            unknownString = stringResource(R.string.other),
            onDismiss = { showBreakdownSheet = false },
        )
    }

    if (!isOnboardingCompleted) {
        prasad.vennam.moneypilot.ui.components.OnboardingDialog(
            initialCurrency = currencyCode,
            onSavePreferences = { goal, target, currency ->
                mainViewModel.savePreferences(goal, target, currency)
            },
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
                    "Smart AI Insights",
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
                contentDescription = "AI Assistant",
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
