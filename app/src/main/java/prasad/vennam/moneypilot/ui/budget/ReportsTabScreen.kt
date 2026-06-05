package prasad.vennam.moneypilot.ui.budget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.ui.viewmodel.AnalyticsViewModel
import prasad.vennam.moneypilot.ui.viewmodel.BudgetViewModel
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.ui.budget.components.BudgetFormBottomSheet
import prasad.vennam.moneypilot.ui.components.ProfileIconButton
import prasad.vennam.moneypilot.ui.dashboard.SyncState
import prasad.vennam.moneypilot.ui.dashboard.SyncStatusIndicator
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showFormSheet by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<Budget?>(null) }
    
    val currencyCode = prasad.vennam.moneypilot.util.LocalCurrencyCode.current
    val monthlyString = stringResource(R.string.monthly)
    val categories by budgetViewModel.allCategories.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 0) stringResource(R.string.budget_planner) else stringResource(R.string.analytics),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    if (syncState != null) {
                        SyncStatusIndicator(syncState)
                    }
                    ProfileIconButton(userData = userData, onClick = onProfileClick)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        budgetToEdit = null
                        showFormSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_budget),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.budgets), fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.analytics), fontWeight = FontWeight.Bold) }
                )
            }

            when (selectedTab) {
                0 -> {
                    BudgetContent(
                        budgetViewModel = budgetViewModel,
                        transactionViewModel = transactionViewModel,
                        onEditBudget = { budget ->
                            budgetToEdit = budget
                            showFormSheet = true
                        }
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
                                currencyCode = currencyCode
                            )
                        )
                    } else {
                        budgetViewModel.saveBudget(
                            budgetToEdit!!.copy(
                                categoryId = catId,
                                amount = amount.inPaisa
                            )
                        )
                    }
                    showFormSheet = false
                    budgetToEdit = null
                }
            )
        }
    }
}
