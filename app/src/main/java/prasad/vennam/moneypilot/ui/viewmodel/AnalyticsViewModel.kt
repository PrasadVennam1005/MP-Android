package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.data.repository.ExchangeRateRepository
import prasad.vennam.moneypilot.domain.usecase.GetBudgetsUseCase
import prasad.vennam.moneypilot.domain.usecase.GetCategoriesUseCase
import prasad.vennam.moneypilot.domain.usecase.GetInvestmentsUseCase
import prasad.vennam.moneypilot.domain.usecase.GetTransactionsUseCase
import prasad.vennam.moneypilot.feature.ai.domain.AiRepository
import prasad.vennam.moneypilot.util.toMajorUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class TimeFilter {
    THIS_MONTH,
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    ALL_TIME,
}

enum class InsightType {
    INFO,
    SUCCESS,
    WARNING,
}

enum class InsightActionType {
    ADJUST_BUDGET,
    VIEW_TRANSACTIONS,
    ANALYZE_PORTFOLIO,
}

data class FinancialInsight(
    val title: String,
    val description: String,
    val type: InsightType,
    val actionType: InsightActionType? = null,
)

sealed class AiRecommendationState {
    object Idle : AiRecommendationState()

    object Loading : AiRecommendationState()

    data class Success(
        val text: String,
    ) : AiRecommendationState()

    data class Error(
        val message: String,
    ) : AiRecommendationState()
}

data class TrendPoint(
    val label: String,
    val income: Double,
    val expense: Double,
)

data class AssetAllocation(
    val type: String,
    val investedAmount: Double,
    val currentValue: Double,
    val sharePercentage: Float,
)

data class AnalyticsState(
    val isLoading: Boolean = true,
    val timeFilter: TimeFilter = TimeFilter.THIS_MONTH,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val savingsRate: Double = 0.0,
    val trendPoints: List<TrendPoint> = emptyList(),
    val spendingByCategory: Map<Category?, Double> = emptyMap(),
    val assetAllocations: List<AssetAllocation> = emptyList(),
    val insights: List<FinancialInsight> = emptyList(),
    val incomeChangePct: Double? = null,
    val expenseChangePct: Double? = null,
    val netSavingsChangePct: Double? = null,
    val savingsRateChangePct: Double? = null,
)

private data class AnalyticsRawData(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val investments: List<Investment>,
    val budgets: List<Budget>,
)

@HiltViewModel
class AnalyticsViewModel
    @Inject
    constructor(
        private val getTransactionsUseCase: GetTransactionsUseCase,
        private val getCategoriesUseCase: GetCategoriesUseCase,
        private val getInvestmentsUseCase: GetInvestmentsUseCase,
        private val getBudgetsUseCase: GetBudgetsUseCase,
        private val exchangeRateRepo: ExchangeRateRepository,
        private val userPreferences: UserPreferences,
        private val aiRepository: AiRepository,
    ) : ViewModel() {
        private val selectedTimeFilter = MutableStateFlow(TimeFilter.THIS_MONTH)
        private val _aiRecommendation = MutableStateFlow<AiRecommendationState>(AiRecommendationState.Idle)
        val aiRecommendation: StateFlow<AiRecommendationState> = _aiRecommendation

        private val dbDataFlow =
            combine(
                getTransactionsUseCase(),
                getCategoriesUseCase(),
                getInvestmentsUseCase(),
                getBudgetsUseCase(),
            ) { t, c, i, b -> AnalyticsRawData(t, c, i, b) }

        val uiState: StateFlow<AnalyticsState> =
            combine(
                dbDataFlow,
                selectedTimeFilter,
                exchangeRateRepo.allRates,
                userPreferences.currency,
            ) { data, filter, rates, currentCurrency ->

                fun convertAmount(
                    amountInMinor: Long,
                    fromCurrency: String,
                ): Double {
                    if (fromCurrency == currentCurrency) return amountInMinor.toMajorUnit
                    val rateFrom = rates[fromCurrency] ?: 1.0
                    val rateTo = rates[currentCurrency] ?: 1.0
                    val amountInUSD = amountInMinor.toMajorUnit / rateFrom
                    return amountInUSD * rateTo
                }

                val now = Calendar.getInstance()
                val currentMonth = now.get(Calendar.MONTH)
                val currentYear = now.get(Calendar.YEAR)

                // Filter transactions by selected time range
                val filteredTransactions =
                    data.transactions.filter { transaction ->
                        val transCal = Calendar.getInstance().apply { timeInMillis = transaction.timestamp }
                        when (filter) {
                            TimeFilter.THIS_MONTH -> {
                                transCal.get(Calendar.MONTH) == currentMonth && transCal.get(Calendar.YEAR) == currentYear
                            }
                            TimeFilter.LAST_3_MONTHS -> {
                                val limit = Calendar.getInstance().apply { add(Calendar.MONTH, -3) }
                                transCal.after(limit)
                            }
                            TimeFilter.LAST_6_MONTHS -> {
                                val limit = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }
                                transCal.after(limit)
                            }
                            TimeFilter.ALL_TIME -> true
                        }
                    }

                // Filter transactions for the previous period to calculate period-over-period change
                val prevFilteredTransactions =
                    data.transactions.filter { transaction ->
                        val transCal = Calendar.getInstance().apply { timeInMillis = transaction.timestamp }
                        when (filter) {
                            TimeFilter.THIS_MONTH -> {
                                val prevMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                                transCal.get(Calendar.MONTH) == prevMonthCal.get(Calendar.MONTH) &&
                                        transCal.get(Calendar.YEAR) == prevMonthCal.get(Calendar.YEAR)
                            }
                            TimeFilter.LAST_3_MONTHS -> {
                                val limit1 = Calendar.getInstance().apply { add(Calendar.MONTH, -3) }
                                val limit2 = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }
                                transCal.after(limit2) && !transCal.after(limit1)
                            }
                            TimeFilter.LAST_6_MONTHS -> {
                                val limit1 = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }
                                val limit2 = Calendar.getInstance().apply { add(Calendar.MONTH, -12) }
                                transCal.after(limit2) && !transCal.after(limit1)
                            }
                            TimeFilter.ALL_TIME -> false
                        }
                    }

                // Calculate Totals
                val totalIncome =
                    filteredTransactions
                        .filter { it.type == TransactionType.INCOME }
                        .sumOf { convertAmount(it.amount, it.currencyCode) }
                val totalExpense =
                    filteredTransactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .sumOf { convertAmount(it.amount, it.currencyCode) }
                val netSavings = totalIncome - totalExpense
                val savingsRate = if (totalIncome > 0.0) (netSavings / totalIncome) * 100.0 else 0.0

                // Calculate Previous Totals
                val prevIncome =
                    prevFilteredTransactions
                        .filter { it.type == TransactionType.INCOME }
                        .sumOf { convertAmount(it.amount, it.currencyCode) }
                val prevExpense =
                    prevFilteredTransactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .sumOf { convertAmount(it.amount, it.currencyCode) }
                val prevNetSavings = prevIncome - prevExpense
                val prevSavingsRate = if (prevIncome > 0.0) (prevNetSavings / prevIncome) * 100.0 else 0.0

                // Calculate Changes
                val incomeChangePct = if (filter != TimeFilter.ALL_TIME && prevIncome > 0.0) {
                    ((totalIncome - prevIncome) / prevIncome) * 100.0
                } else null

                val expenseChangePct = if (filter != TimeFilter.ALL_TIME && prevExpense > 0.0) {
                    ((totalExpense - prevExpense) / prevExpense) * 100.0
                } else null

                val netSavingsChangePct = if (filter != TimeFilter.ALL_TIME && prevNetSavings != 0.0) {
                    ((netSavings - prevNetSavings) / Math.abs(prevNetSavings)) * 100.0
                } else null

                val savingsRateChangePct = if (filter != TimeFilter.ALL_TIME && prevSavingsRate > 0.0) {
                    ((savingsRate - prevSavingsRate) / prevSavingsRate) * 100.0
                } else null

                // Calculate Spending Breakdown by Category
                val categoriesMap = data.categories.associateBy { it.id }

                val spendingByCategory =
                    filteredTransactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .groupBy { it.categoryId }
                        .mapKeys { (catId, _) -> categoriesMap[catId] }
                        .mapValues { (_, transList) -> transList.sumOf { convertAmount(it.amount, it.currencyCode) } }

                // Calculate Trend Points
                val trendPoints =
                    if (filter == TimeFilter.THIS_MONTH) {
                        // Group by Day of Month
                        val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val formatDay = SimpleDateFormat("dd", Locale.getDefault())
                        val pointsMap =
                            (1..daysInMonth)
                                .associate { day ->
                                    val label = String.format("%02d", day)
                                    label to TrendPoint(label, 0.0, 0.0)
                                }.toMutableMap()

                        filteredTransactions.forEach { tx ->
                            val txDay = formatDay.format(Date(tx.timestamp))
                            val currentPoint = pointsMap[txDay]
                            if (currentPoint != null) {
                                val converted = convertAmount(tx.amount, tx.currencyCode)
                                pointsMap[txDay] =
                                    if (tx.type == TransactionType.INCOME) {
                                        currentPoint.copy(income = currentPoint.income + converted)
                                    } else {
                                        currentPoint.copy(expense = currentPoint.expense + converted)
                                    }
                            }
                        }
                        pointsMap.values.sortedBy { it.label }
                    } else {
                        // Group by MonthName
                        val formatMonth = SimpleDateFormat("MMM yy", Locale.getDefault())

                        // Collect last N months or all months
                        val activeMonths = mutableListOf<String>()
                        val limitMonths =
                            when (filter) {
                                TimeFilter.LAST_3_MONTHS -> 3
                                TimeFilter.LAST_6_MONTHS -> 6
                                else -> 12 // limit to 12 months for visual spacing in ALL_TIME
                            }

                        if (filter == TimeFilter.ALL_TIME) {
                            // Find all unique months in transaction history
                            data.transactions
                                .map { formatMonth.format(Date(it.timestamp)) }
                                .distinct()
                                .takeLast(12)
                                .forEach { activeMonths.add(it) }
                        } else {
                            for (i in (limitMonths - 1) downTo 0) {
                                val mCal = Calendar.getInstance().apply { add(Calendar.MONTH, -i) }
                                activeMonths.add(formatMonth.format(mCal.time))
                            }
                        }

                        val pointsMap = activeMonths.associateWith { TrendPoint(it, 0.0, 0.0) }.toMutableMap()

                        filteredTransactions.forEach { tx ->
                            val txMonth = formatMonth.format(Date(tx.timestamp))
                            val currentPoint = pointsMap[txMonth]
                            if (currentPoint != null) {
                                val converted = convertAmount(tx.amount, tx.currencyCode)
                                pointsMap[txMonth] =
                                    if (tx.type == TransactionType.INCOME) {
                                        currentPoint.copy(income = currentPoint.income + converted)
                                    } else {
                                        currentPoint.copy(expense = currentPoint.expense + converted)
                                    }
                            }
                        }
                        // Sort chronologically using a Date parser
                        val parser = SimpleDateFormat("MMM yy", Locale.getDefault())
                        pointsMap.values.sortedBy {
                            try {
                                parser.parse(it.label)?.time ?: 0L
                            } catch (e: Exception) {
                                0L
                            }
                        }
                    }

                // Calculate Investment Asset Allocation
                val totalInvested = data.investments.sumOf { convertAmount(it.investedAmount, it.currencyCode) }
                val assetAllocations =
                    data.investments
                        .groupBy { it.type }
                        .map { (type, list) ->
                            val invested = list.sumOf { convertAmount(it.investedAmount, it.currencyCode) }
                            val currentVal = list.sumOf { convertAmount(it.currentValue, it.currencyCode) }
                            val share = if (totalInvested > 0.0) (invested / totalInvested).toFloat() else 0f
                            AssetAllocation(type, invested, currentVal, share)
                        }.sortedByDescending { it.investedAmount }

                // Generate Automated Financial Insights
                val insights = mutableListOf<FinancialInsight>()

                // 1. Savings Rate Insight
                if (totalIncome > 0.0) {
                    when {
                        savingsRate >= 30.0 -> {
                            insights.add(
                                FinancialInsight(
                                    title = "Excellent Savings Rate",
                                    description = "You saved ${String.format(
                                        "%.1f",
                                        savingsRate,
                                    )}% of your income this period. Keep building your wealth!",
                                    type = InsightType.SUCCESS,
                                ),
                            )
                        }
                        savingsRate < 10.0 -> {
                            insights.add(
                                FinancialInsight(
                                    title = "High Spending Warning",
                                    description = "Your savings rate is ${String.format(
                                        "%.1f",
                                        savingsRate,
                                    )}% this period. Consider reviewing non-essential spending.",
                                    type = InsightType.WARNING,
                                    actionType = InsightActionType.VIEW_TRANSACTIONS,
                                ),
                            )
                        }
                        else -> {
                            insights.add(
                                FinancialInsight(
                                    title = "Healthy Savings Habits",
                                    description = "You saved ${String.format(
                                        "%.1f",
                                        savingsRate,
                                    )}% of your income. Standard target is 20%.",
                                    type = InsightType.INFO,
                                ),
                            )
                        }
                    }
                }

                // 2. Top Category Spending Insight
                if (spendingByCategory.isNotEmpty()) {
                    val topCategory = spendingByCategory.maxByOrNull { it.value }
                    if (topCategory != null) {
                        val catName = topCategory.key?.name ?: "Other"
                        val catPct = if (totalExpense > 0.0) (topCategory.value / totalExpense) * 100.0 else 0.0
                        insights.add(
                            FinancialInsight(
                                title = "Top Spend: $catName",
                                description = "Your largest expense is in $catName making up ${String.format(
                                    "%.1f",
                                    catPct,
                                )}% of total expenses.",
                                type = InsightType.INFO,
                                actionType = InsightActionType.VIEW_TRANSACTIONS,
                            ),
                        )
                    }
                }

                // 3. Investment Growth Insight
                if (totalInvested > 0.0) {
                    val totalCurrent = data.investments.sumOf { convertAmount(it.currentValue, it.currencyCode) }
                    val gain = totalCurrent - totalInvested
                    val gainPct = (gain / totalInvested) * 100.0
                    if (gain > 0.0) {
                        insights.add(
                            FinancialInsight(
                                title = "Portfolio Growth",
                                description = "Your investment portfolio has generated a profit of ${String.format(
                                    "%.1f",
                                    gainPct,
                                )}% (+${String.format("%.2f", gain)} $currentCurrency).",
                                type = InsightType.SUCCESS,
                                actionType = InsightActionType.ANALYZE_PORTFOLIO,
                            ),
                        )
                    } else if (gain < 0.0) {
                        insights.add(
                            FinancialInsight(
                                title = "Portfolio Alert",
                                description = "Your investment portfolio is currently down by ${String.format(
                                    "%.1f",
                                    Math.abs(gainPct),
                                )}% (${String.format("%.2f", gain)} $currentCurrency).",
                                type = InsightType.WARNING,
                                actionType = InsightActionType.ANALYZE_PORTFOLIO,
                            ),
                        )
                    }
                }

                // 4. Budget Overrun Insight
                val currentMonthExpenses =
                    data.transactions.filter {
                        val transCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        it.type == TransactionType.EXPENSE &&
                            transCal.get(Calendar.MONTH) == currentMonth &&
                            transCal.get(Calendar.YEAR) == currentYear
                    }
                val expensesByCategoryId = currentMonthExpenses.groupBy { it.categoryId }

                val overruns =
                    data.budgets.mapNotNull { budget ->
                        val cat = categoriesMap[budget.categoryId]
                        val spent =
                            expensesByCategoryId[budget.categoryId]
                                ?.sumOf { convertAmount(it.amount, it.currencyCode) } ?: 0.0

                        val budgetAmt = convertAmount(budget.amount, budget.currencyCode)
                        if (spent > budgetAmt) {
                            cat?.name to (spent - budgetAmt)
                        } else {
                            null
                        }
                    }

                if (overruns.isNotEmpty()) {
                    insights.add(
                        FinancialInsight(
                            title = "Budget Exceeded",
                            description = "You have exceeded monthly budgets in ${overruns.size} category/categories (e.g. ${overruns.first().first} by ${String.format(
                                "%.2f",
                                overruns.first().second,
                            )} $currentCurrency).",
                            type = InsightType.WARNING,
                            actionType = InsightActionType.ADJUST_BUDGET,
                        ),
                    )
                }

                AnalyticsState(
                    isLoading = false,
                    timeFilter = filter,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    netSavings = netSavings,
                    savingsRate = savingsRate,
                    trendPoints = trendPoints,
                    spendingByCategory = spendingByCategory,
                    assetAllocations = assetAllocations,
                    insights = insights,
                    incomeChangePct = incomeChangePct,
                    expenseChangePct = expenseChangePct,
                    netSavingsChangePct = netSavingsChangePct,
                    savingsRateChangePct = savingsRateChangePct,
                )
            }.flowOn(Dispatchers.Default)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = AnalyticsState(isLoading = true),
                )

        fun setTimeFilter(filter: TimeFilter) {
            selectedTimeFilter.update { filter }
        }

        fun generateAiRecommendation(currencyCode: String) {
            val state = uiState.value
            if (state.isLoading) return

            viewModelScope.launch {
                _aiRecommendation.value = AiRecommendationState.Loading
                try {
                    val summary =
                        buildString {
                            append("Savings: ${String.format("%.1f", state.savingsRate)}%, ")
                            if (state.insights.isNotEmpty()) {
                                append(
                                    "Key Findings: ${state.insights.take(2).joinToString("; ") { it.title + " (" + it.description + ")" }}",
                                )
                            }
                        }
                    val advice = aiRepository.generateShortAdvice(summary)
                    if (advice.isNotEmpty()) {
                        _aiRecommendation.value = AiRecommendationState.Success(advice)
                    } else {
                        _aiRecommendation.value =
                            AiRecommendationState.Error(
                                "AI Model is not ready. Go to the AI Chat tab to download and initialize the model.",
                            )
                    }
                } catch (e: Exception) {
                    _aiRecommendation.value = AiRecommendationState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }
