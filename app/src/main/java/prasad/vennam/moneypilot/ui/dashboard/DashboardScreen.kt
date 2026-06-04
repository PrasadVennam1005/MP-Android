package prasad.vennam.moneypilot.ui.dashboard

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil.compose.AsyncImage
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.ui.components.SpendingDonutChart
import prasad.vennam.moneypilot.ui.viewmodel.BudgetViewModel
import prasad.vennam.moneypilot.ui.viewmodel.InvestmentViewModel
import prasad.vennam.moneypilot.ui.viewmodel.MainViewModel
import prasad.vennam.moneypilot.ui.viewmodel.RestoreState
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.GoogleSheetsSyncHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    transactionViewModel: TransactionViewModel,
    investmentViewModel: InvestmentViewModel,
    budgetViewModel: BudgetViewModel,
    mainViewModel: MainViewModel,
    analyticsHelper: AnalyticsHelper,
    onNavigateToAddTransaction: (TransactionType) -> Unit,
    onNavigateToAddInvestment: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToScanner: () -> Unit,
) {
    val transactions by transactionViewModel.allTransactions.collectAsState()
    val categories by transactionViewModel.allCategories.collectAsState()
    val budgets by budgetViewModel.allBudgets.collectAsState()
    val userData by mainViewModel.userData.collectAsState()
    val investments by investmentViewModel.allInvestments.collectAsState()
    val isSynced by mainViewModel.isSynced.collectAsState()
    val spreadsheetId by mainViewModel.spreadsheetId.collectAsState()

    val context = LocalContext.current
    val restoreState by mainViewModel.restoreState.collectAsState()

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            mainViewModel.checkAndPerformRestore(context)
        }
    }

    LaunchedEffect(restoreState) {
        when (val state = restoreState) {
            is RestoreState.Success -> {
                Toast.makeText(context, "Backup successfully restored!", Toast.LENGTH_LONG).show()
                mainViewModel.resetRestoreCheck()
            }

            is RestoreState.NeedAuthorization -> {
                authLauncher.launch(state.intent)
            }

            is RestoreState.Error -> {
                Toast.makeText(context, "Restore failed: ${state.message}", Toast.LENGTH_LONG)
                    .show()
                mainViewModel.resetRestoreCheck()
            }

            else -> {}
        }
    }

    val workManager = WorkManager.getInstance(context)
    val workInfos by workManager.getWorkInfosForUniqueWorkFlow(GoogleSheetsSyncHelper.SYNC_WORK_NAME).collectAsState(initial = emptyList())

    val isGuest = remember(userData) { userData?.email == "guest@moneypilot.app" }
    val syncState = remember(isSynced, workInfos, isGuest) {
        if (isGuest) null
        else if (!isSynced || spreadsheetId == null) SyncState.PENDING_CONNECTION
        else {
            val workInfo = workInfos.firstOrNull()
            when (workInfo?.state) {
                WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> SyncState.SYNCING
                WorkInfo.State.FAILED -> SyncState.FAILED
                else -> SyncState.SYNCED
            }
        }
    }

    var showBreakdownSheet by remember { mutableStateOf(false) }

    // 4. Financial Health: Track budget alerts
    LaunchedEffect(budgets, transactions) {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        budgets.forEach { budget ->
            val spent = transactions.filter {
                val transCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                it.categoryId == budget.categoryId &&
                        it.type == TransactionType.EXPENSE &&
                        transCal.get(Calendar.MONTH) == currentMonth &&
                        transCal.get(Calendar.YEAR) == currentYear
            }.sumOf { it.amount }

            if (budget.amount > 0 && spent / budget.amount >= 0.9) {
                val categoryName = categories.find { it.id == budget.categoryId }?.name ?: "Unknown"
                analyticsHelper.logEvent(
                    "budget_warning_viewed", mapOf(
                        "category" to categoryName,
                        "percent" to (spent / budget.amount * 100).toInt()
                    )
                )
            }
        }
    }

    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)
    val today = calendar.get(Calendar.DAY_OF_YEAR)

    val monthlyTransactions = remember(transactions, currentMonth, currentYear) {
        transactions.filter {
            val transCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            transCal.get(Calendar.MONTH) == currentMonth && transCal.get(Calendar.YEAR) == currentYear
        }
    }

    val todayExpense = remember(transactions, today, currentYear) {
        transactions.filter {
            val transCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            it.type == TransactionType.EXPENSE && transCal.get(Calendar.DAY_OF_YEAR) == today && transCal.get(
                Calendar.YEAR
            ) == currentYear
        }.sumOf { it.amount }
    }

    val monthlyExpense =
        monthlyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val monthlyIncome =
        monthlyTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val savings = monthlyIncome - monthlyExpense

    val totalInvestment = remember(investments) { investments.sumOf { it.investedAmount } }
    val currentInvestmentValue = remember(investments) { investments.sumOf { it.currentValue } }

    val spendingByCategory = remember(monthlyTransactions, categories) {
        monthlyTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapKeys { (catId, _) -> categories.find { it.id == catId }?.name ?: "Other" }
            .mapValues { (_, trans) -> trans.sumOf { it.amount } }
    }

    val chartColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.outline,
        MaterialTheme.colorScheme.scrim,
        MaterialTheme.colorScheme.inversePrimary
    )

    Scaffold(
        topBar = {
            DashboardTopBar(userData, syncState, onNavigateToSettings)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // KPI Cards
            item {
                KPISection(
                    todayExpense,
                    monthlyExpense,
                    monthlyIncome,
                    savings,
                    totalInvestment,
                    currentInvestmentValue
                )
            }

            // Quick Actions
            item {
                QuickActionSection(
                    onAddExpense = { onNavigateToAddTransaction(TransactionType.EXPENSE) },
                    onAddIncome = { onNavigateToAddTransaction(TransactionType.INCOME) },
                    onAddInvestment = onNavigateToAddInvestment,
                    onScanReceipt = onNavigateToScanner,
                    isGuest = isGuest
                )
            }

            // Charts Section
            if (spendingByCategory.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Expense Breakdown",
                        onInfoClick = if (spendingByCategory.size > 10) {
                            { showBreakdownSheet = true }
                        } else null
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    ExpenseChartCard(spendingByCategory, chartColors)
                }
            }

            // Budget Progress
            if (budgets.isNotEmpty()) {
                item {
                    SectionHeader("Budget Progress", onActionClick = onNavigateToBudgets)
                    BudgetProgressSection(budgets, monthlyTransactions, categories)
                }
            }

            // Recent Transactions
            item {
                SectionHeader("Recent Transactions", onActionClick = onNavigateToHistory)
                RecentTransactionsCard(transactions.take(5), categories)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showBreakdownSheet) {
        CategoryBreakdownBottomSheet(
            spendingByCategory = spendingByCategory,
            colors = chartColors,
            onDismiss = { showBreakdownSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(
    userData: UserPreferences.UserData?,
    syncState: SyncState?,
    onProfileClick: () -> Unit,
) {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = "$greeting,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = userData?.name ?: "User",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            if (syncState != null) {
                SyncStatusIndicator(syncState)
            }
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                if (userData?.photoUrl != null) {
                    AsyncImage(
                        model = userData.photoUrl,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun KPISection(
    today: Double,
    monthlyExp: Double,
    monthlyInc: Double,
    savings: Double,
    investment: Double,
    currentInvestmentValue: Double,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KPICard(
                title = "Today's Expense",
                amount = today,
                icon = Icons.Rounded.Today,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            KPICard(
                title = "Savings",
                amount = savings,
                icon = Icons.Rounded.AccountBalance,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KPICard(
                title = "Monthly Income",
                amount = monthlyInc,
                icon = Icons.Rounded.ArrowUpward,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            KPICard(
                title = "Monthly Expense",
                amount = monthlyExp,
                icon = Icons.Rounded.ArrowDownward,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KPICard(
                title = "Investments",
                amount = investment,
                icon = Icons.Rounded.Savings,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            KPICard(
                title = "Profit / Loss",
                amount = currentInvestmentValue - investment,
                icon = Icons.Rounded.AddChart,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun KPICard(
    title: String,
    amount: Double,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.7f)
            )
            Text(
                buildString {
                    append("₹")
                    append(String.format("%,.0f", amount))
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

@Composable
fun QuickActionSection(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddInvestment: () -> Unit,
    onScanReceipt: () -> Unit,
    isGuest: Boolean,
) {
    Column {
        SectionHeader("Quick Actions")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                "Expense",
                Icons.Rounded.RemoveCircleOutline,
                MaterialTheme.colorScheme.error,
                onAddExpense,
                Modifier.weight(1f)
            )
            QuickActionButton(
                "Income",
                Icons.Rounded.AddCircleOutline,
                MaterialTheme.colorScheme.secondary,
                onAddIncome,
                Modifier.weight(1f)
            )
            QuickActionButton(
                "Investment",
                Icons.Rounded.AccountBalanceWallet,
                MaterialTheme.colorScheme.primary,
                onAddInvestment,
                Modifier.weight(1f)
            )
            QuickActionButton(
                "Scan",
                Icons.Rounded.Camera,
                MaterialTheme.colorScheme.tertiary,
                onScanReceipt,
                Modifier.weight(1f),
                isGuest
            )
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
) {
    OutlinedCard(
        onClick = if (disabled) ({}) else onClick,
        modifier = modifier
            .height(90.dp)
            .graphicsLayer { alpha = if (disabled) 0.5f else 1f },
        shape = MaterialTheme.shapes.large,
        border = CardDefaults.outlinedCardBorder()
            .copy(brush = Brush.linearGradient(listOf(color.copy(alpha = 0.5f), color)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    onActionClick: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            if (onInfoClick != null) {
                IconButton(onClick = onInfoClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Info,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        if (onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text("See All", style = MaterialTheme.typography.labelLarge)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ExpenseChartCard(spendingByCategory: Map<String, Double>, colors: List<Color>) {
    val totalExpense = spendingByCategory.values.sum()
    val sortedList = spendingByCategory.toList().sortedByDescending { it.second }
    val displayList = sortedList.take(10)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                SpendingDonutChart(
                    sortedSpending = sortedList,
                    colors = colors,
                    modifier = Modifier.size(200.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        buildString {
                            append("₹")
                            append(String.format("%,.0f", totalExpense))
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                displayList.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowItems.forEach { pair ->
                            val colorIndex = sortedList.indexOf(pair)
                            LegendItem(
                                name = pair.first,
                                color = colors.getOrElse(colorIndex) { Color.Gray },
                                amount = pair.second,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBreakdownBottomSheet(
    spendingByCategory: Map<String, Double>,
    colors: List<Color>,
    onDismiss: () -> Unit,
) {
    val sortedList = spendingByCategory.toList().sortedByDescending { it.second }
    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
        ) {
            Text(
                "Full Expense Breakdown",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 16.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(sortedList) { pair ->
                    val colorIndex = sortedList.indexOf(pair)
                    LegendItem(
                        name = pair.first,
                        color = colors.getOrElse(colorIndex) { Color.Gray },
                        amount = pair.second,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(name: String, color: Color, amount: Double, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(8.dp)
    ) {
        Box(modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(buildString {
                append("₹")
                append(String.format("%,.0f", amount))
            }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BudgetProgressSection(
    budgets: List<Budget>,
    transactions: List<Transaction>,
    categories: List<Category>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        budgets.take(3).forEach { budget ->
            val category = categories.find { it.id == budget.categoryId }
            val spent =
                transactions.filter { it.categoryId == budget.categoryId && it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
            val progress = (spent / budget.amount).toFloat().coerceIn(0f, 1f)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            category?.name ?: "Unknown",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(buildString {
                            append("₹")
                            append(String.format(buildString {
                                append("%.0f")
                            }, spent))
                            append(" / ₹")
                            append(String.format("%.0f", budget.amount))
                        }, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RecentTransactionsCard(transactions: List<Transaction>, categories: List<Category>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            transactions.forEach {
                TransactionItem(
                    it,
                    categories.find { c -> c.id == it.categoryId })
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, category: Category?) {
    val dateFormatter = SimpleDateFormat("dd MMM, hh:mm a", LocalLocale.current.platformLocale)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = if (transaction.type == TransactionType.INCOME) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (transaction.type == TransactionType.INCOME) Icons.Rounded.Add else Icons.Rounded.Remove,
                    contentDescription = null,
                    tint = if (transaction.type == TransactionType.INCOME) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.note.ifBlank { category?.name ?: "Transaction" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dateFormatter.format(Date(transaction.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = buildString {
                append(if (transaction.type == TransactionType.INCOME) "+" else "-")
                append("₹")
                append(String.format("%,.0f", transaction.amount))
            },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (transaction.type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

enum class SyncState { SYNCED, SYNCING, PENDING_CONNECTION, FAILED }

@Composable
fun SyncStatusIndicator(state: SyncState) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse)
    )
    val (icon, color, label) = when (state) {
        SyncState.SYNCED -> Triple(Icons.Rounded.CloudDone, Color(0xFF4CAF50), "Cloud Synced")
        SyncState.SYNCING -> Triple(
            Icons.Rounded.CloudSync,
            MaterialTheme.colorScheme.primary,
            "Syncing..."
        )

        SyncState.PENDING_CONNECTION -> Triple(
            Icons.Rounded.CloudOff,
            MaterialTheme.colorScheme.outline,
            "Not Connected"
        )

        SyncState.FAILED -> Triple(
            Icons.Rounded.Warning,
            MaterialTheme.colorScheme.error,
            "Sync Failed"
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 8.dp)
            .graphicsLayer { if (state == SyncState.SYNCING) this.alpha = alpha }) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
