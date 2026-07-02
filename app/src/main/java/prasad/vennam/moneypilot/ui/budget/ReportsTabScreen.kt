package prasad.vennam.moneypilot.ui.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.ui.budget.components.BudgetFormBottomSheet
import prasad.vennam.moneypilot.ui.budget.components.BudgetHeaderSection
import prasad.vennam.moneypilot.ui.budget.components.EmptyBudgetState
import prasad.vennam.moneypilot.ui.budget.components.PremiumBudgetCard
import prasad.vennam.moneypilot.ui.components.AdBannerView
import prasad.vennam.moneypilot.ui.components.ProfileIconButton
import prasad.vennam.moneypilot.ui.dashboard.SyncState
import prasad.vennam.moneypilot.ui.dashboard.SyncStatusIndicator
import prasad.vennam.moneypilot.ui.viewmodel.AnalyticsViewModel
import prasad.vennam.moneypilot.ui.viewmodel.BudgetProgress
import prasad.vennam.moneypilot.ui.viewmodel.BudgetViewModel
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.TrackScreen
import prasad.vennam.moneypilot.util.toMinorUnit
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsTabScreen(
    budgetViewModel: BudgetViewModel,
    transactionViewModel: TransactionViewModel,
    analyticsViewModel: AnalyticsViewModel,
    userData: UserPreferences.UserData?,
    syncState: SyncState?,
    isPremium: Boolean,
    analyticsHelper: AnalyticsHelper,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.REPORTS)
    var selectedTab by remember { mutableStateOf(0) }
    var showFormSheet by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<Budget?>(null) }

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    var isFabVisible by remember { mutableStateOf(true) }
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset, lazyGridState.firstVisibleItemIndex, lazyGridState.firstVisibleItemScrollOffset) {
        val currentIndex = if (isExpanded) lazyGridState.firstVisibleItemIndex else lazyListState.firstVisibleItemIndex
        val currentOffset = if (isExpanded) lazyGridState.firstVisibleItemScrollOffset else lazyListState.firstVisibleItemScrollOffset
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

    val currencyCode = prasad.vennam.moneypilot.util.LocalCurrencyCode.current
    val monthlyString = stringResource(R.string.monthly)
    val categories by budgetViewModel.allCategories.collectAsState()
    val budgetProgresses by budgetViewModel.budgetProgresses.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
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
                    AnimatedVisibility(
                        visible = !isFabVisible,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        IconButton(onClick = {
                            budgetToEdit = null
                            showFormSheet = true
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = stringResource(R.string.add_budget),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
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
                    onClick = {
                        analyticsHelper.logEvent(
                            AnalyticsConstants.Event.REPORTS_TAB_SWITCHED,
                            mapOf(AnalyticsConstants.Param.TAB to "budgets"),
                        )
                        selectedTab = 0
                    },
                    text = { Text(stringResource(R.string.budgets), fontWeight = FontWeight.Bold) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        analyticsHelper.logEvent(
                            AnalyticsConstants.Event.REPORTS_TAB_SWITCHED,
                            mapOf(AnalyticsConstants.Param.TAB to "analytics"),
                        )
                        selectedTab = 1
                    },
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
                        lazyGridState = lazyGridState,
                        isExpanded = isExpanded,
                        isPremium = isPremium,
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
                    AnalyticsScreen(
                        viewModel = analyticsViewModel,
                        analyticsHelper = analyticsHelper,
                        isPremium = isPremium,
                    )
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
                                amount = amount.toMinorUnit,
                                period = monthlyString,
                                currencyCode = currencyCode,
                            ),
                        )
                    } else {
                        budgetViewModel.saveBudget(
                            budgetToEdit!!.copy(
                                categoryId = catId,
                                amount = amount.toMinorUnit,
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
    lazyGridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    isExpanded: Boolean,
    onEditBudget: (Budget) -> Unit,
    onDeleteBudget: (Budget) -> Unit,
    isPremium: Boolean,
    modifier: Modifier = Modifier,
) {
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    if (isExpanded) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = lazyGridState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(span = { GridItemSpan(2) }) {
                BudgetHeaderSection(budgetProgresses, currentMonth, currentYear)
            }

            item(span = { GridItemSpan(2) }) {
                Text(
                    stringResource(R.string.monthly_budgets),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (budgetProgresses.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    EmptyBudgetState()
                }
            } else {
                items(budgetProgresses, key = { "${it.budget.id}_${it.budget.lastUpdated}" }) { itemState ->
                    PremiumBudgetCard(
                        budgetProgress = itemState,
                        onEdit = { onEditBudget(itemState.budget) },
                        onDelete = { onDeleteBudget(itemState.budget) },
                    )
                }
            }

            if (!isPremium) {
                item(span = { GridItemSpan(2) }) {
                    AdBannerView(
                        isPremium = isPremium,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                    )
                }
            }
        }
    } else {
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
                items(budgetProgresses, key = { "${it.budget.id}_${it.budget.lastUpdated}" }) { itemState ->
                    PremiumBudgetCard(
                        budgetProgress = itemState,
                        onEdit = { onEditBudget(itemState.budget) },
                        onDelete = { onDeleteBudget(itemState.budget) },
                    )
                }
            }

            if (!isPremium) {
                item {
                    AdBannerView(
                        isPremium = isPremium,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                    )
                }
            }
        }
    }
}
