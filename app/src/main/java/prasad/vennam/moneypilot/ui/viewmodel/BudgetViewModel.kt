package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.data.repository.ExchangeRateRepository
import prasad.vennam.moneypilot.domain.usecase.DeleteBudgetUseCase
import prasad.vennam.moneypilot.domain.usecase.GetBudgetsUseCase
import prasad.vennam.moneypilot.domain.usecase.GetCategoriesUseCase
import prasad.vennam.moneypilot.domain.usecase.GetTransactionsUseCase
import prasad.vennam.moneypilot.domain.usecase.SaveBudgetUseCase
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel
    @Inject
    constructor(
        private val getBudgetsUseCase: GetBudgetsUseCase,
        private val getCategoriesUseCase: GetCategoriesUseCase,
        private val getTransactionsUseCase: GetTransactionsUseCase,
        private val exchangeRateRepo: ExchangeRateRepository,
        private val userPreferences: UserPreferences,
        private val saveBudgetUseCase: SaveBudgetUseCase,
        private val deleteBudgetUseCase: DeleteBudgetUseCase,
    ) : ViewModel() {
        val allBudgets: StateFlow<List<Budget>> =
            getBudgetsUseCase()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val allCategories =
            getCategoriesUseCase()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val budgetProgresses: StateFlow<List<BudgetProgress>> =
            combine(
                getBudgetsUseCase(),
                getCategoriesUseCase(),
                getTransactionsUseCase(),
                exchangeRateRepo.allRates,
                userPreferences.currency,
            ) {
                budgets: List<prasad.vennam.moneypilot.data.entity.Budget>,
                categories: List<prasad.vennam.moneypilot.data.entity.Category>,
                transactions: List<prasad.vennam.moneypilot.data.entity.Transaction>,
                rates: Map<String, Double>,
                currentCurrency: String,
                ->

                fun convertAmount(
                    amountInPaisa: Long,
                    fromCurrency: String,
                ): Double {
                    if (fromCurrency == currentCurrency) return amountInPaisa / 100.0
                    val rateFrom = rates[fromCurrency] ?: 1.0
                    val rateTo = rates[currentCurrency] ?: 1.0
                    val amountInUSD = (amountInPaisa / 100.0) / rateFrom
                    return amountInUSD * rateTo
                }

                val calendar = java.util.Calendar.getInstance()
                val currentMonth = calendar.get(java.util.Calendar.MONTH)
                val currentYear = calendar.get(java.util.Calendar.YEAR)

                val categoriesMap = categories.associateBy { it.id }
                val currentMonthExpenses = transactions.filter {
                    val transCal = java.util.Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    it.type == TransactionType.EXPENSE &&
                        transCal.get(java.util.Calendar.MONTH) == currentMonth &&
                        transCal.get(java.util.Calendar.YEAR) == currentYear
                }
                val expensesByCategoryId = currentMonthExpenses.groupBy { it.categoryId }

                budgets.map { budget ->
                    val category = categoriesMap[budget.categoryId]
                    val spent = expensesByCategoryId[budget.categoryId]
                        ?.sumOf { convertAmount(it.amount, it.currencyCode) } ?: 0.0

                    val budgetConverted = convertAmount(budget.amount, budget.currencyCode)
                    val progress = if (budgetConverted > 0) (spent / budgetConverted).toFloat().coerceIn(0f, 1f) else 0f
                    prasad.vennam.moneypilot.ui.viewmodel
                        .BudgetProgress(budget, category, spent, budgetConverted, progress)
                }
            }.flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun saveBudget(budget: Budget) {
            viewModelScope.launch {
                try {
                    saveBudgetUseCase(budget)
                } catch (e: Exception) {
                    android.util.Log.e("BudgetViewModel", "Error saving budget", e)
                }
            }
        }

        fun deleteBudget(budget: Budget) {
            viewModelScope.launch {
                try {
                    deleteBudgetUseCase(budget)
                } catch (e: Exception) {
                    android.util.Log.e("BudgetViewModel", "Error deleting budget", e)
                }
            }
        }
    }
