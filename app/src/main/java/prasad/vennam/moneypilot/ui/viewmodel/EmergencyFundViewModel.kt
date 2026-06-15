package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.entity.EmergencyFund
import prasad.vennam.moneypilot.domain.usecase.GetEmergencyFundUseCase
import prasad.vennam.moneypilot.domain.usecase.SaveEmergencyFundUseCase
import javax.inject.Inject

@HiltViewModel
class EmergencyFundViewModel
    @Inject
    constructor(
        private val getEmergencyFundUseCase: GetEmergencyFundUseCase,
        private val saveEmergencyFundUseCase: SaveEmergencyFundUseCase,
    ) : ViewModel() {
        val emergencyFund: StateFlow<EmergencyFund?> =
            getEmergencyFundUseCase()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        fun saveEmergencyFund(
            monthly: Double,
            months: Int,
            saved: Double,
        ) {
            viewModelScope.launch {
                try {
                    val config =
                        EmergencyFund(
                            id = 1,
                            monthlyExpenses = monthly,
                            targetMonths = months,
                            currentSaved = saved,
                        )
                    saveEmergencyFundUseCase(config)
                } catch (e: Exception) {
                    android.util.Log.e("EmergencyFundViewModel", "Error saving emergency fund", e)
                }
            }
        }

        fun updateEmergencySaved(saved: Double) {
            viewModelScope.launch {
                try {
                    saveEmergencyFundUseCase(saved)
                } catch (e: Exception) {
                    android.util.Log.e("EmergencyFundViewModel", "Error updating emergency saved", e)
                }
            }
        }
    }
