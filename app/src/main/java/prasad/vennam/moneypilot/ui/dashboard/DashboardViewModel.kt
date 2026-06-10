package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.data.repository.ExchangeRateRepository
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import prasad.vennam.moneypilot.util.inRupees
import java.util.Calendar
import javax.inject.Inject

data class BudgetProgress(
    val budget: Budget,
    val category: Category?,
    val spent: Double,
    val limit: Double,
    val progress: Float,
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
    val categories: List<Category> = emptyList(),
    val loans: List<Loan> = emptyList(),
)

private data class DashboardData(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val budgets: List<Budget>,
    val investments: List<Investment>,
    val loans: List<Loan>,
)

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
        private val exchangeRateRepo: ExchangeRateRepository,
        private val userPreferences: UserPreferences,
    ) : ViewModel() {
        private val dataFlow =
            combine(
                repository.allTransactions,
                repository.allCategories,
                repository.allBudgets,
                repository.allInvestments,
                repository.allLoans,
            ) { t, c, b, i, l -> DashboardData(t, c, b, i, l) }

        val uiState: StateFlow<DashboardState> =
            combine(
                dataFlow,
                exchangeRateRepo.allRates,
                userPreferences.currency,
            ) { data, allRates, currentCurrencyCode ->

                fun convertAmount(
                    amountInMinor: Long,
                    fromCurrency: String,
                ): Double {
                    if (fromCurrency == currentCurrencyCode) return amountInMinor.inRupees
                    val rateFrom = allRates[fromCurrency] ?: 1.0
                    val rateTo = allRates[currentCurrencyCode] ?: 1.0
                    val amountInUSD = amountInMinor.inRupees / rateFrom
                    return amountInUSD * rateTo
                }

                val transactions = data.transactions
                val categories = data.categories
                val budgets = data.budgets
                val investments = data.investments

                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                val today = calendar.get(Calendar.DAY_OF_YEAR)

                val monthlyTransactions =
                    transactions.filter {
                        val transCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        transCal.get(Calendar.MONTH) == currentMonth && transCal.get(Calendar.YEAR) == currentYear
                    }

                val todayExpense =
                    transactions
                        .filter {
                            val transCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                            it.type == TransactionType.EXPENSE &&
                                transCal.get(Calendar.DAY_OF_YEAR) == today &&
                                transCal.get(Calendar.YEAR) == currentYear
                        }.sumOf { convertAmount(it.amount, it.currencyCode) }

                val monthlyExpense =
                    monthlyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf {
                        convertAmount(
                            it.amount,
                            it.currencyCode,
                        )
                    }
                val monthlyIncome =
                    monthlyTransactions.filter { it.type == TransactionType.INCOME }.sumOf {
                        convertAmount(
                            it.amount,
                            it.currencyCode,
                        )
                    }
                val savings = monthlyIncome - monthlyExpense

                val totalInvestment = investments.sumOf { convertAmount(it.investedAmount, it.currencyCode) }
                val currentInvestmentValue = investments.sumOf { convertAmount(it.currentValue, it.currencyCode) }

                val spendingByCategory =
                    monthlyTransactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .groupBy { it.categoryId }
                        .mapKeys { (catId, _) -> categories.find { it.id == catId } }
                        .mapValues { (_, trans) -> trans.sumOf { convertAmount(it.amount, it.currencyCode) } }

                val budgetProgresses =
                    budgets.map { budget ->
                        val category = categories.find { it.id == budget.categoryId }
                        val spent =
                            transactions
                                .filter {
                                    val transCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                                    it.categoryId == budget.categoryId &&
                                            it.type == TransactionType.EXPENSE &&
                                            transCal.get(Calendar.MONTH) == currentMonth &&
                                            transCal.get(Calendar.YEAR) == currentYear
                                }.sumOf { convertAmount(it.amount, it.currencyCode) }

                        val budgetConverted = convertAmount(budget.amount, budget.currencyCode)
                        val progress = if (budgetConverted > 0) (spent / budgetConverted).toFloat().coerceIn(0f, 1f) else 0f
                        BudgetProgress(budget, category, spent, budgetConverted, progress)
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
                    categories = categories,
                    loans = data.loans,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = DashboardState(isLoading = true),
            )

        fun addLoan(
            name: String,
            total: Long,
            outstanding: Long,
            emi: Long,
            currencyCode: String,
            lenderName: String = "",
            interestRate: Double = 0.0,
            tenureMonths: Int = 12,
            dueDayOfMonth: Int = 1,
            isNotificationEnabled: Boolean = true,
        ) {
            viewModelScope.launch {
                repository.insertLoan(
                    Loan(
                        name = name,
                        totalAmount = total,
                        outstandingAmount = outstanding,
                        emiAmount = emi,
                        nextEmiDate = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_MONTH, dueDayOfMonth.coerceIn(1, 28))
                            if (get(Calendar.DAY_OF_MONTH) >= dueDayOfMonth) {
                                add(Calendar.MONTH, 1)
                            }
                        }.timeInMillis,
                        currencyCode = currencyCode,
                        lenderName = lenderName,
                        interestRate = interestRate,
                        tenureMonths = tenureMonths,
                        dueDayOfMonth = dueDayOfMonth,
                        isNotificationEnabled = isNotificationEnabled,
                    ),
                )
            }
        }

        fun updateLoan(loan: Loan) {
            viewModelScope.launch {
                repository.updateLoan(loan)
            }
        }

        fun deleteLoan(loan: Loan) {
            viewModelScope.launch {
                repository.deleteLoan(loan)
            }
        }
    }
