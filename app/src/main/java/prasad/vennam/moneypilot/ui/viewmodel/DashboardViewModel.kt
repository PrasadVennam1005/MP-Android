package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.EmergencyFund
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.data.entity.LoanPayment
import prasad.vennam.moneypilot.data.entity.PendingTransaction
import prasad.vennam.moneypilot.data.entity.SavingGoal
import prasad.vennam.moneypilot.data.entity.Subscription
import prasad.vennam.moneypilot.data.entity.TimeFrame
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.data.repository.ExchangeRateRepository
import prasad.vennam.moneypilot.data.repository.LoanRepository
import prasad.vennam.moneypilot.domain.usecase.*
import prasad.vennam.moneypilot.util.LoanIntelligenceUtil
import prasad.vennam.moneypilot.util.RemoteConfigHelper
import prasad.vennam.moneypilot.util.toMajorUnit
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
    val periodExpense: Double = 0.0,
    val periodIncome: Double = 0.0,
    val savings: Double = 0.0,
    val totalInvestment: Double = 0.0,
    val currentInvestmentValue: Double = 0.0,
    val spendingByCategory: Map<Category?, Double> = emptyMap(),
    val recentTransactions: List<Transaction> = emptyList(),
    val budgetProgresses: List<BudgetProgress> = emptyList(),
    val categories: List<Category> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val savingGoals: List<SavingGoal> = emptyList(),
    val emergencyFund: EmergencyFund? = null,
    val savingsRate: Double = 0.0,
    val totalDebt: Double = 0.0,
    val selectedTimeFrame: TimeFrame = TimeFrame.MONTHLY,
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val pendingTransactions: List<PendingTransaction> = emptyList(),
    val isLearnFinanceEnabled: Boolean = false,
    val errorMessage: String? = null,
)

private data class DashboardData(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val budgets: List<Budget>,
    val investments: List<Investment>,
    val loans: List<Loan>,
    val subscriptions: List<Subscription>,
    val savingGoals: List<SavingGoal>,
    val emergencyFund: EmergencyFund?,
    val pendingTransactions: List<PendingTransaction>,
)

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        private val exchangeRateRepo: ExchangeRateRepository,
        private val userPreferences: UserPreferences,
        private val loanRepository: LoanRepository,
        private val addLoanUseCase: AddLoanUseCase,
        private val updateLoanUseCase: UpdateLoanUseCase,
        private val deleteLoanUseCase: DeleteLoanUseCase,
        private val getTransactionsUseCase: GetTransactionsUseCase,
        private val getCategoriesUseCase: GetCategoriesUseCase,
        private val getBudgetsUseCase: GetBudgetsUseCase,
        private val getInvestmentsUseCase: GetInvestmentsUseCase,
        private val getLoansUseCase: GetLoansUseCase,
        private val getSubscriptionsUseCase: GetSubscriptionsUseCase,
        private val getSavingGoalsUseCase: GetSavingGoalsUseCase,
        private val getEmergencyFundUseCase: GetEmergencyFundUseCase,
        private val getPendingTransactionsUseCase: GetPendingTransactionsUseCase,
        private val approvePendingTransactionUseCase: ApprovePendingTransactionUseCase,
        private val dismissPendingTransactionUseCase: DismissPendingTransactionUseCase,
        private val remoteConfigHelper: RemoteConfigHelper,
    ) : ViewModel() {
        private val _selectedTimeFrame = MutableStateFlow(TimeFrame.MONTHLY)
        val selectedTimeFrame: StateFlow<TimeFrame> = _selectedTimeFrame.asStateFlow()

        private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
        val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

        private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
        val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

        data class TimeFilter(val timeFrame: TimeFrame, val month: Int, val year: Int)

        private val filterFlow = combine(_selectedTimeFrame, _selectedMonth, _selectedYear) { tf, m, y ->
            TimeFilter(tf, m, y)
        }

        fun setTimeFrame(timeFrame: TimeFrame) {
            _selectedTimeFrame.value = timeFrame
            // Reset to current date when changing timeframe
            val now = Calendar.getInstance()
            _selectedMonth.value = now.get(Calendar.MONTH)
            _selectedYear.value = now.get(Calendar.YEAR)
        }

        fun setMonth(month: Int) {
            _selectedMonth.value = month
        }

        fun setYear(year: Int) {
            _selectedYear.value = year
        }

        fun navigatePrevious() {
            when (_selectedTimeFrame.value) {
                TimeFrame.MONTHLY -> {
                    if (_selectedMonth.value == 0) {
                        _selectedMonth.value = 11
                        _selectedYear.value -= 1
                    } else {
                        _selectedMonth.value -= 1
                    }
                }
                TimeFrame.QUARTERLY -> {
                    val currentQuarterMonthStart = (_selectedMonth.value / 3) * 3
                    if (currentQuarterMonthStart == 0) {
                        _selectedMonth.value = 9 // Q4 start (Oct)
                        _selectedYear.value -= 1
                    } else {
                        _selectedMonth.value = currentQuarterMonthStart - 3
                    }
                }
                TimeFrame.YEARLY -> {
                    _selectedYear.value -= 1
                }
            }
        }

        fun navigateNext() {
            val now = Calendar.getInstance()
            val maxYear = now.get(Calendar.YEAR)
            val maxMonth = now.get(Calendar.MONTH)

            when (_selectedTimeFrame.value) {
                TimeFrame.MONTHLY -> {
                    if (_selectedYear.value < maxYear || (_selectedYear.value == maxYear && _selectedMonth.value < maxMonth)) {
                        if (_selectedMonth.value == 11) {
                            _selectedMonth.value = 0
                            _selectedYear.value += 1
                        } else {
                            _selectedMonth.value += 1
                        }
                    }
                }
                TimeFrame.QUARTERLY -> {
                    val currentQuarterMonthStart = (_selectedMonth.value / 3) * 3
                    val maxQuarterMonthStart = (maxMonth / 3) * 3
                    if (_selectedYear.value < maxYear || (_selectedYear.value == maxYear && currentQuarterMonthStart < maxQuarterMonthStart)) {
                        if (currentQuarterMonthStart == 9) {
                            _selectedMonth.value = 0 // Q1 start (Jan)
                            _selectedYear.value += 1
                        } else {
                            _selectedMonth.value = currentQuarterMonthStart + 3
                        }
                    }
                }
                TimeFrame.YEARLY -> {
                    if (_selectedYear.value < maxYear) {
                        _selectedYear.value += 1
                    }
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        private val dataFlow =
            combine(
                getTransactionsUseCase(),
                getCategoriesUseCase(),
                getBudgetsUseCase(),
                getInvestmentsUseCase(),
                getLoansUseCase(),
                getSubscriptionsUseCase(),
                getSavingGoalsUseCase(),
                getEmergencyFundUseCase(),
                getPendingTransactionsUseCase(),
            ) { array ->
                DashboardData(
                    transactions = array[0] as List<Transaction>,
                    categories = array[1] as List<Category>,
                    budgets = array[2] as List<Budget>,
                    investments = array[3] as List<Investment>,
                    loans = array[4] as List<Loan>,
                    subscriptions = array[5] as List<Subscription>,
                    savingGoals = array[6] as List<SavingGoal>,
                    emergencyFund = array[7] as EmergencyFund?,
                    pendingTransactions = array[8] as List<PendingTransaction>,
                )
            }

        val uiState: StateFlow<DashboardState> =
            combine(
                dataFlow,
                exchangeRateRepo.allRates,
                userPreferences.currency,
                filterFlow,
            ) { data, allRates, currentCurrencyCode, filter ->
                val timeFrame = filter.timeFrame
                val selMonth = filter.month
                val selYear = filter.year

                fun convertAmount(
                    amountInMinor: Long,
                    fromCurrency: String,
                ): Double {
                    if (fromCurrency == currentCurrencyCode) return amountInMinor.toMajorUnit
                    val rateFrom = allRates[fromCurrency] ?: 1.0
                    val rateTo = allRates[currentCurrencyCode] ?: 1.0
                    val amountInUSD = amountInMinor.toMajorUnit / rateFrom
                    return amountInUSD * rateTo
                }

                val transactions = data.transactions
                val categories = data.categories
                val budgets = data.budgets
                val investments = data.investments

                val calendar = Calendar.getInstance()
                val today = calendar.get(Calendar.DAY_OF_YEAR)
                val currentYear = calendar.get(Calendar.YEAR)

                // Filter transactions based on selected TimeFrame, Month, and Year
                val filteredTransactions =
                    transactions.filter {
                        val transCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        val transYear = transCal.get(Calendar.YEAR)
                        val transMonth = transCal.get(Calendar.MONTH)

                        if (transYear != selYear) {
                            false
                        } else {
                            when (timeFrame) {
                                TimeFrame.MONTHLY -> transMonth == selMonth
                                TimeFrame.QUARTERLY -> {
                                    val selectedQuarter = selMonth / 3
                                    val transQuarter = transMonth / 3
                                    transQuarter == selectedQuarter
                                }
                                TimeFrame.YEARLY -> true
                            }
                        }
                    }

                val todayExpense =
                    transactions
                        .filter {
                            val transCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                            it.type == TransactionType.EXPENSE &&
                                transCal.get(Calendar.DAY_OF_YEAR) == today &&
                                transCal.get(Calendar.YEAR) == currentYear
                        }.sumOf { convertAmount(it.amount, it.currencyCode) }

                val periodExpense =
                    filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf {
                        convertAmount(
                            it.amount,
                            it.currencyCode,
                        )
                    }
                val periodIncome =
                    filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf {
                        convertAmount(
                            it.amount,
                            it.currencyCode,
                        )
                    }
                val savings = periodIncome - periodExpense
                val savingsRate = if (periodIncome > 0) (savings / periodIncome) * 100 else 0.0

                val totalInvestment = investments.sumOf { convertAmount(it.investedAmount, it.currencyCode) }
                val currentInvestmentValue = investments.sumOf { convertAmount(it.currentValue, it.currencyCode) }
                val totalDebt = data.loans.sumOf { convertAmount(it.outstandingAmount, it.currencyCode) }

                val categoriesMap = categories.associateBy { it.id }

                val spendingByCategory =
                    filteredTransactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .groupBy { it.categoryId }
                        .mapKeys { (catId, _) -> categoriesMap[catId] }
                        .mapValues { (_, trans) -> trans.sumOf { convertAmount(it.amount, it.currencyCode) } }

                val currentMonthExpenses =
                    transactions.filter {
                        val transCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        it.type == TransactionType.EXPENSE &&
                            transCal.get(Calendar.MONTH) == selMonth &&
                            transCal.get(Calendar.YEAR) == selYear
                    }
                val expensesByCategoryId = currentMonthExpenses.groupBy { it.categoryId }

                val budgetProgresses =
                    budgets.map { budget ->
                        val category = categoriesMap[budget.categoryId]
                        val spent =
                            expensesByCategoryId[budget.categoryId]
                                ?.sumOf { convertAmount(it.amount, it.currencyCode) } ?: 0.0

                        val budgetConverted = convertAmount(budget.amount, budget.currencyCode)
                        val progress = if (budgetConverted > 0) (spent / budgetConverted).toFloat().coerceIn(0f, 1f) else 0f
                        BudgetProgress(budget, category, spent, budgetConverted, progress)
                    }

                DashboardState(
                    isLoading = false,
                    todayExpense = todayExpense,
                    periodExpense = periodExpense,
                    periodIncome = periodIncome,
                    savings = savings,
                    totalInvestment = totalInvestment,
                    currentInvestmentValue = currentInvestmentValue,
                    spendingByCategory = spendingByCategory,
                    recentTransactions = transactions.take(5),
                    budgetProgresses = budgetProgresses,
                    categories = categories,
                    loans = data.loans,
                    subscriptions = data.subscriptions,
                    savingGoals = data.savingGoals,
                    emergencyFund = data.emergencyFund,
                    savingsRate = savingsRate,
                    totalDebt = totalDebt,
                    selectedTimeFrame = timeFrame,
                    selectedMonth = selMonth,
                    selectedYear = selYear,
                    pendingTransactions = data.pendingTransactions,
                    isLearnFinanceEnabled = remoteConfigHelper.isLearnFinanceEnabled(),
                )
            }.flowOn(Dispatchers.Default)
                .catch { e ->
                    emit(DashboardState(isLoading = false, errorMessage = e.message))
                }
                .stateIn(
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
                addLoanUseCase(
                    Loan(
                        name = name,
                        totalAmount = total,
                        outstandingAmount = outstanding,
                        emiAmount = emi,
                        nextEmiDate =
                            Calendar
                                .getInstance()
                                .apply {
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
                updateLoanUseCase(loan)
            }
        }

        fun deleteLoan(loan: Loan) {
            viewModelScope.launch {
                deleteLoanUseCase(loan)
            }
        }

        fun recordLoanPayment(
            loanId: Long,
            amount: Long,
            isExtra: Boolean = false,
            note: String = "",
        ) {
            viewModelScope.launch {
                val payment =
                    LoanPayment(
                        loanId = loanId,
                        amount = amount,
                        date = System.currentTimeMillis(),
                        isExtraPayment = isExtra,
                        note = note,
                    )
                loanRepository.insertLoanPayment(payment)
            }
        }

        fun getLoanPayments(loanId: Long): kotlinx.coroutines.flow.Flow<List<LoanPayment>> = loanRepository.getPaymentsForLoan(loanId)

        fun estimatePayoff(loan: Loan): Long =
            LoanIntelligenceUtil.predictPayoffDate(
                loan.outstandingAmount,
                loan.interestRate,
                loan.emiAmount,
            )

        fun approveTransaction(
            pending: PendingTransaction,
            categoryId: Long?,
            note: String = "",
        ) {
            viewModelScope.launch {
                approvePendingTransactionUseCase(pending, categoryId, note)
            }
        }

        fun dismissTransaction(pending: PendingTransaction) {
            viewModelScope.launch {
                dismissPendingTransactionUseCase(pending)
            }
        }
    }
