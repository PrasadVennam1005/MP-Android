package prasad.vennam.moneypilot.ui.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.ui.budget.components.BudgetFormBottomSheet
import prasad.vennam.moneypilot.ui.budget.components.BudgetHeaderSection
import prasad.vennam.moneypilot.ui.budget.components.EmptyBudgetState
import prasad.vennam.moneypilot.ui.budget.components.PremiumBudgetCard
import prasad.vennam.moneypilot.ui.components.ProfileIconButton
import prasad.vennam.moneypilot.ui.dashboard.SyncState
import prasad.vennam.moneypilot.ui.dashboard.SyncStatusIndicator
import prasad.vennam.moneypilot.ui.viewmodel.AnalyticsViewModel
import prasad.vennam.moneypilot.ui.viewmodel.BudgetProgress
import prasad.vennam.moneypilot.ui.viewmodel.BudgetViewModel
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.util.inPaisa
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsTabScreen(
    budgetViewModel: BudgetViewModel,
    transactionViewModel: TransactionViewModel,
    analyticsViewModel: AnalyticsViewModel,
    userData: UserPreferences.UserData?,
    syncState: SyncState?,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showFormSheet by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<Budget?>(null) }

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
        } else if (currentIndex < previousIndex || (currentIndex == previousIndex && currentOffset < previousOffset)) {
            isFabVisible = true
        }
        previousIndex = currentIndex
        previousOffset = currentOffset
    }

    val currencyCode = prasad.vennam.moneypilot.util.LocalCurrencyCode.current
    val monthlyString = stringResource(R.string.monthly)
    val categories by budgetViewModel.allCategories.collectAsState()
    val budgetProgresses by budgetViewModel.budgetProgresses.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 0) stringResource(R.string.budget_planner) else stringResource(R.string.analytics),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                actions = {
                    if (syncState != null) {
                        SyncStatusIndicator(syncState)
                    }
                    ProfileIconButton(userData = userData, onClick = onProfileClick)
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = Color.Unspecified,
                        navigationIconContentColor = Color.Unspecified,
                        titleContentColor = Color.Unspecified,
                        actionIconContentColor = Color.Unspecified,
                    ),
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedTab == 0 && isFabVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                FloatingActionButton(
                    onClick = {
                        budgetToEdit = null
                        showFormSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_budget),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.budgets), fontWeight = FontWeight.Bold) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.analytics), fontWeight = FontWeight.Bold) },
                )
            }

            when (selectedTab) {
                0 -> {
                    val deletedMessage = stringResource(R.string.budget_deleted)
                    val undoLabel = stringResource(R.string.undo)
                    BudgetContent(
                        budgetProgresses = budgetProgresses,
                        lazyListState = lazyListState,
                        onEditBudget = { budget ->
                            budgetToEdit = budget
                            showFormSheet = true
                        },
                        onDeleteBudget = { budget ->
                            val budgetCopy = budget
                            budgetViewModel.deleteBudget(budget)
                            scope.launch {
                                val result =
                                    snackbarHostState.showSnackbar(
                                        message = deletedMessage,
                                        actionLabel = undoLabel,
                                        duration = SnackbarDuration.Short,
                                    )
                                if (result == SnackbarResult.ActionPerformed) {
                                    budgetViewModel.saveBudget(budgetCopy)
                                }
                            }
                        },
                    )
                }
                1 -> {
                    AnalyticsScreen(viewModel = analyticsViewModel)
                }
            }
        }

        if (showFormSheet) {
            BudgetFormBottomSheet(
                initialBudget = budgetToEdit,
                categories = categories.filter { it.isExpense },
                onDismiss = {
                    showFormSheet = false
                    budgetToEdit = null
                },
                onSave = { catId, amount ->
                    if (budgetToEdit == null) {
                        budgetViewModel.saveBudget(
                            Budget(
                                categoryId = catId,
                                amount = amount.inPaisa,
                                period = monthlyString,
                                currencyCode = currencyCode,
                            ),
                        )
                    } else {
                        budgetViewModel.saveBudget(
                            budgetToEdit!!.copy(
                                categoryId = catId,
                                amount = amount.inPaisa,
                            ),
                        )
                    }
                    showFormSheet = false
                    budgetToEdit = null
                },
            )
        }
    }
}

@Composable
fun BudgetContent(
    budgetProgresses: List<BudgetProgress>,
    lazyListState: LazyListState,
    onEditBudget: (Budget) -> Unit,
    onDeleteBudget: (Budget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            BudgetHeaderSection(budgetProgresses, currentMonth, currentYear)
        }

        item {
            Text(
                stringResource(R.string.monthly_budgets),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (budgetProgresses.isEmpty()) {
            item {
                EmptyBudgetState()
            }
        } else {
            items(budgetProgresses, key = { it.budget.id }) { itemState ->
                PremiumBudgetCard(
                    budgetProgress = itemState,
                    onEdit = { onEditBudget(itemState.budget) },
                    onDelete = { onDeleteBudget(itemState.budget) },
                )
            }
        }
    }
}
