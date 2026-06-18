package prasad.vennam.moneypilot.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import prasad.vennam.moneypilot.domain.model.EmiResult
import prasad.vennam.moneypilot.domain.model.LoanComparisonResult
import prasad.vennam.moneypilot.domain.usecase.CalculateEmiUseCase
import prasad.vennam.moneypilot.domain.usecase.CompareLoansUseCase
import javax.inject.Inject

@Immutable
data class CompareLoansUiState(
    val amountA: String = "5000000",
    val rateA: String = "8.5",
    val tenureA: String = "20",
    val amountB: String = "5000000",
    val rateB: String = "9.0",
    val tenureB: String = "15",
    val comparisonResult: LoanComparisonResult? = null,
    val emiResultA: EmiResult? = null,
    val emiResultB: EmiResult? = null,
)

@HiltViewModel
class CompareLoansViewModel
    @Inject
    constructor(
        private val calculateEmiUseCase: CalculateEmiUseCase,
        private val compareLoansUseCase: CompareLoansUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CompareLoansUiState())

        val uiState: StateFlow<CompareLoansUiState> =
            _uiState
                .map { state ->
                    val resA =
                        calculateEmiUseCase(
                            state.amountA.toDoubleOrNull() ?: 0.0,
                            state.rateA.toDoubleOrNull() ?: 0.0,
                            state.tenureA.toIntOrNull() ?: 0,
                            true,
                        )
                    val resB =
                        calculateEmiUseCase(
                            state.amountB.toDoubleOrNull() ?: 0.0,
                            state.rateB.toDoubleOrNull() ?: 0.0,
                            state.tenureB.toIntOrNull() ?: 0,
                            true,
                        )

                    val comp = compareLoansUseCase(resA, resB)

                    state.copy(
                        comparisonResult = comp,
                        emiResultA = resA,
                        emiResultB = resB,
                    )
                }.flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompareLoansUiState())

        fun updateAmountA(v: String) {
            _uiState.update { it.copy(amountA = v) }
        }

        fun updateRateA(v: String) {
            _uiState.update { it.copy(rateA = v) }
        }

        fun updateTenureA(v: String) {
            _uiState.update { it.copy(tenureA = v) }
        }

        fun updateAmountB(v: String) {
            _uiState.update { it.copy(amountB = v) }
        }

        fun updateRateB(v: String) {
            _uiState.update { it.copy(rateB = v) }
        }

        fun updateTenureB(v: String) {
            _uiState.update { it.copy(tenureB = v) }
        }
    }
