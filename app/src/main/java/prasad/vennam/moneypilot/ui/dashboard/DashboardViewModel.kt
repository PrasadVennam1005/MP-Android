package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import java.util.Calendar
import javax.inject.Inject

data class BudgetProgress(
    val budget: Budget,
    val category: Category?,
    val spent: Double,
    val progress: Float
)

data class DashboardState(
    val isLoading: Boolean = true,
    val todayExpense: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val savings: Double = 0.0,
    val totalInvestment: Double = 0.0,
    val currentInvestmentValue: Double = 0.0,
    val spendingByCategory: Map<Category?, Double> = emptyMap(),
    val recentTransactions: List<Transaction> = emptyList(),
    val budgetProgresses: List<BudgetProgress> = emptyList(),
    val categories: List<Category> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: MoneyPilotRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardState> = combine(
        repository.allTransactions,
        repository.allCategories,
        repository.allBudgets,
        repository.allInvestments
    ) { transactions, categories, budgets, investments ->
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val today = calendar.get(Calendar.DAY_OF_YEAR)

        val monthlyTransactions = transactions.filter {
            val transCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            transCal.get(Calendar.MONTH) == currentMonth && transCal.get(Calendar.YEAR) == currentYear
        }

        val todayExpense = transactions.filter {
            val transCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            it.type == TransactionType.EXPENSE && transCal.get(Calendar.DAY_OF_YEAR) == today && transCal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }

        val monthlyExpense = monthlyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val monthlyIncome = monthlyTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val savings = monthlyIncome - monthlyExpense

        val totalInvestment = investments.sumOf { it.investedAmount }
        val currentInvestmentValue = investments.sumOf { it.currentValue }

        val spendingByCategory = monthlyTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapKeys { (catId, _) -> categories.find { it.id == catId } }
            .mapValues { (_, trans) -> trans.sumOf { it.amount } }

        val budgetProgresses = budgets.map { budget ->
            val category = categories.find { it.id == budget.categoryId }
            val spent = transactions.filter { 
                it.categoryId == budget.categoryId && it.type == TransactionType.EXPENSE 
            }.sumOf { it.amount }
            val progress = if (budget.amount > 0) (spent / budget.amount).toFloat().coerceIn(0f, 1f) else 0f
            BudgetProgress(budget, category, spent, progress)
        }

        DashboardState(
            isLoading = false,
            todayExpense = todayExpense,
            monthlyExpense = monthlyExpense,
            monthlyIncome = monthlyIncome,
            savings = savings,
            totalInvestment = totalInvestment,
            currentInvestmentValue = currentInvestmentValue,
            spendingByCategory = spendingByCategory,
            recentTransactions = transactions.take(5),
            budgetProgresses = budgetProgresses,
            categories = categories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState(isLoading = true)
    )
}
