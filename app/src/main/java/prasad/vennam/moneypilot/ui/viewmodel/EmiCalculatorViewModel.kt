package prasad.vennam.moneypilot.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import prasad.vennam.moneypilot.domain.model.AffordabilityResult
import prasad.vennam.moneypilot.domain.model.AmortizationInstallment
import prasad.vennam.moneypilot.domain.model.EmiResult
import prasad.vennam.moneypilot.domain.model.PrepaymentResult
import prasad.vennam.moneypilot.domain.usecase.CalculateAffordabilityUseCase
import prasad.vennam.moneypilot.domain.usecase.CalculateEmiUseCase
import prasad.vennam.moneypilot.domain.usecase.CalculatePrepaymentUseCase
import prasad.vennam.moneypilot.domain.usecase.CompareLoansUseCase
import prasad.vennam.moneypilot.domain.usecase.GenerateAmortizationScheduleUseCase
import prasad.vennam.moneypilot.util.CurrencyFormatter
import javax.inject.Inject

@Immutable
data class EmiCalculatorUiState(
    val selectedTab: Int = 0,
    val amountInput: String = "5000000",
    val rateInput: String = "8.5",
    val tenureInput: String = "20",
    val isTenureInYears: Boolean = true,
    val processingFeeInput: String = "0",
    val showDetailedReport: Boolean = false,
    val isMonthlyView: Boolean = true,
    val searchQuery: String = "",
    val pageIndex: Int = 0,
    val emiResult: EmiResult = EmiResult(0.0, 0.0, 0.0, "", 0.0),
    val monthlySchedule: List<AmortizationInstallment> = emptyList(),
    val yearlySchedule: List<AmortizationInstallment> = emptyList(),
    val formattedMonthlyEmi: String = "",
    val formattedTotalInterest: String = "",
    val formattedTotalPayable: String = "",
    val formattedProcessingFee: String = "",
    val prepaymentAmount: String = "",
    val isMonthlyPrepayment: Boolean = false,
    val prepaymentResult: PrepaymentResult? = null,
    val monthlyIncomeInput: String = "",
    val affordabilityResult: AffordabilityResult? = null,
)

@HiltViewModel
class EmiCalculatorViewModel
    @Inject
    constructor(
        private val calculateEmiUseCase: CalculateEmiUseCase,
        private val generateScheduleUseCase: GenerateAmortizationScheduleUseCase,
        private val calculatePrepaymentUseCase: CalculatePrepaymentUseCase,
        private val calculateAffordabilityUseCase: CalculateAffordabilityUseCase,
        private val compareLoansUseCase: CompareLoansUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(EmiCalculatorUiState())
        private val currencyCodeFlow = MutableStateFlow("INR")

        val uiState: StateFlow<EmiCalculatorUiState> =
            combine(
                flow = _uiState,
                flow2 = currencyCodeFlow,
            ) { state, currency ->
                val principal = state.amountInput.toDoubleOrNull() ?: 0.0
                val rate = state.rateInput.toDoubleOrNull() ?: 0.0
                val tenure = state.tenureInput.toIntOrNull() ?: 0
                val feePercent = state.processingFeeInput.toDoubleOrNull() ?: 0.0

                val result = calculateEmiUseCase(principal, rate, tenure, state.isTenureInYears, feePercent)

                val tenureMonths = if (state.isTenureInYears) tenure * 12 else tenure
                val monthlySchedule = generateScheduleUseCase(principal, rate, tenureMonths, result.monthlyEmi)
                val yearlySchedule = generateScheduleUseCase.generateYearlySchedule(monthlySchedule)

                val prepaymentResult =
                    if (state.prepaymentAmount.isNotEmpty()) {
                        calculatePrepaymentUseCase(
                            principal,
                            rate,
                            tenureMonths,
                            result.monthlyEmi,
                            state.prepaymentAmount.toDoubleOrNull() ?: 0.0,
                            state.isMonthlyPrepayment,
                        )
                    } else {
                        null
                    }

                val affordabilityResult =
                    if (state.monthlyIncomeInput.isNotEmpty()) {
                        calculateAffordabilityUseCase(
                            state.monthlyIncomeInput.toDoubleOrNull() ?: 0.0,
                            rate,
                            if (state.isTenureInYears) tenure else tenure / 12,
                        )
                    } else {
                        null
                    }

                state.copy(
                    emiResult = result,
                    monthlySchedule = monthlySchedule,
                    yearlySchedule = yearlySchedule,
                    formattedMonthlyEmi = CurrencyFormatter.format(result.monthlyEmi, currency),
                    formattedTotalInterest = CurrencyFormatter.format(result.totalInterest, currency),
                    formattedTotalPayable = CurrencyFormatter.format(result.totalPayable, currency),
                    formattedProcessingFee = CurrencyFormatter.format(result.processingFeeAmount, currency),
                    prepaymentResult = prepaymentResult,
                    affordabilityResult = affordabilityResult,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EmiCalculatorUiState())

        fun initializeDefaults(currencyCode: String) {
            currencyCodeFlow.value = currencyCode
            val tabIndex = _uiState.value.selectedTab
            val defaultPrincipal =
                when (tabIndex) {
                    0 -> if (currencyCode == "INR") 5_000_000.0 else 250_000.0
                    1 -> if (currencyCode == "INR") 1_000_000.0 else 40_000.0
                    2 -> if (currencyCode == "INR") 300_000.0 else 15_000.0
                    else -> if (currencyCode == "INR") 500_000.0 else 25_000.0
                }
            _uiState.update { it.copy(amountInput = defaultPrincipal.toLong().toString()) }
        }

        fun updateTab(
            tabIndex: Int,
            currencyCode: String,
        ) {
            currencyCodeFlow.value = currencyCode
            val defaultPrincipal =
                when (tabIndex) {
                    0 -> if (currencyCode == "INR") 5_000_000.0 else 250_000.0
                    1 -> if (currencyCode == "INR") 1_000_000.0 else 40_000.0
                    2 -> if (currencyCode == "INR") 300_000.0 else 15_000.0
                    else -> if (currencyCode == "INR") 500_000.0 else 25_000.0
                }
            val defaultRate =
                when (tabIndex) {
                    0 -> 8.5
                    1 -> 9.0
                    2 -> 12.0
                    else -> 10.0
                }
            val defaultTenure =
                when (tabIndex) {
                    0 -> 20
                    1 -> 5
                    2 -> 3
                    else -> 5
                }

            _uiState.update {
                it.copy(
                    selectedTab = tabIndex,
                    amountInput = defaultPrincipal.toLong().toString(),
                    rateInput = defaultRate.toString(),
                    tenureInput = defaultTenure.toString(),
                    showDetailedReport = false,
                    isMonthlyView = true,
                    searchQuery = "",
                    pageIndex = 0,
                )
            }
        }

        fun updateAmount(amount: String) {
            _uiState.update { it.copy(amountInput = amount, pageIndex = 0) }
        }

        fun updateRate(rate: String) {
            _uiState.update { it.copy(rateInput = rate, pageIndex = 0) }
        }

        fun updateTenure(tenure: String) {
            _uiState.update { it.copy(tenureInput = tenure, pageIndex = 0) }
        }

        fun updateTenureUnit(isYears: Boolean) {
            _uiState.update { it.copy(isTenureInYears = isYears, pageIndex = 0) }
        }

        fun updateProcessingFee(fee: String) {
            _uiState.update { it.copy(processingFeeInput = fee) }
        }

        fun updateShowDetailedReport(show: Boolean) {
            _uiState.update { it.copy(showDetailedReport = show) }
        }

        fun updateIsMonthlyView(isMonthly: Boolean) {
            _uiState.update { it.copy(isMonthlyView = isMonthly, pageIndex = 0) }
        }

        fun updateSearchQuery(query: String) {
            _uiState.update { it.copy(searchQuery = query, pageIndex = 0) }
        }

        fun updatePageIndex(index: Int) {
            _uiState.update { it.copy(pageIndex = index) }
        }

        fun updatePrepaymentAmount(amount: String) {
            _uiState.update { it.copy(prepaymentAmount = amount) }
        }

        fun updateIsMonthlyPrepayment(isMonthly: Boolean) {
            _uiState.update { it.copy(isMonthlyPrepayment = isMonthly) }
        }

        fun updateMonthlyIncome(income: String) {
            _uiState.update { it.copy(monthlyIncomeInput = income) }
        }
    }
