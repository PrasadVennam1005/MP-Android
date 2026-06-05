package prasad.vennam.moneypilot.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.ui.budget.components.BudgetFormBottomSheet
import prasad.vennam.moneypilot.ui.budget.components.BudgetHeaderSection
import prasad.vennam.moneypilot.ui.budget.components.EmptyBudgetState
import prasad.vennam.moneypilot.ui.budget.components.PremiumBudgetCard
import prasad.vennam.moneypilot.ui.budget.utils.BudgetItemState
import prasad.vennam.moneypilot.ui.viewmodel.BudgetProgress
import prasad.vennam.moneypilot.ui.viewmodel.BudgetViewModel
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.util.inPaisa
import prasad.vennam.moneypilot.util.inRupees
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    budgetViewModel: BudgetViewModel,
    transactionViewModel: TransactionViewModel,
) {
    val monthlyString = stringResource(R.string.monthly)
    val categories by budgetViewModel.allCategories.collectAsState()

    var showFormSheet by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<Budget?>(null) }
    
    val currencyCode = prasad.vennam.moneypilot.util.LocalCurrencyCode.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.budget_planner),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
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
    ) { innerPadding ->
        BudgetContent(
            budgetViewModel = budgetViewModel,
            transactionViewModel = transactionViewModel,
            onEditBudget = {
                budgetToEdit = it
                showFormSheet = true
            },
            modifier = Modifier.padding(innerPadding)
        )
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

@Composable
fun BudgetContent(
    budgetViewModel: BudgetViewModel,
    transactionViewModel: TransactionViewModel,
    onEditBudget: (Budget) -> Unit,
    modifier: Modifier = Modifier
) {
    val budgetProgresses by budgetViewModel.budgetProgresses.collectAsState()

    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            BudgetHeaderSection(budgetProgresses, currentMonth, currentYear)
        }

        item {
            Text(
                stringResource(R.string.monthly_budgets),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
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
                    onDelete = { budgetViewModel.deleteBudget(itemState.budget) }
                )
            }
        }
    }
}

