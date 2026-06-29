package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.entity.SavingGoal
import prasad.vennam.moneypilot.data.repository.GoalRepository
import javax.inject.Inject

@HiltViewModel
class SavingGoalViewModel
    @Inject
    constructor(
        private val repository: GoalRepository,
    ) : ViewModel() {
        val allSavingGoals: StateFlow<List<SavingGoal>> =
            repository.allSavingGoals
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun saveSavingGoal(savingGoal: SavingGoal) {
            viewModelScope.launch {
                try {
                    val isCompleted = savingGoal.currentSavedAmount >= savingGoal.targetAmount
                    val updatedGoal = savingGoal.copy(isCompleted = isCompleted)
                    if (updatedGoal.id == 0L) {
                        repository.insertSavingGoal(updatedGoal)
                    } else {
                        repository.updateSavingGoal(updatedGoal)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SavingGoalViewModel", "Error saving saving goal", e)
                }
            }
        }

        fun deleteSavingGoal(savingGoal: SavingGoal) {
            viewModelScope.launch {
                try {
                    repository.deleteSavingGoal(savingGoal)
                } catch (e: Exception) {
                    android.util.Log.e("SavingGoalViewModel", "Error deleting saving goal", e)
                }
            }
        }

        fun depositToGoal(
            goal: SavingGoal,
            amount: Long,
        ) {
            viewModelScope.launch {
                try {
                    val newSavedAmount = goal.currentSavedAmount + amount
                    val isCompleted = newSavedAmount >= goal.targetAmount
                    repository.updateSavingGoal(
                        goal.copy(
                            currentSavedAmount = newSavedAmount,
                            isCompleted = isCompleted,
                            lastUpdated = System.currentTimeMillis(),
                        ),
                    )
                } catch (e: Exception) {
                    android.util.Log.e("SavingGoalViewModel", "Error depositing to goal", e)
                }
            }
        }

        fun withdrawFromGoal(
            goal: SavingGoal,
            amount: Long,
        ) {
            viewModelScope.launch {
                try {
                    val newSavedAmount = (goal.currentSavedAmount - amount).coerceAtLeast(0)
                    val isCompleted = newSavedAmount >= goal.targetAmount
                    repository.updateSavingGoal(
                        goal.copy(
                            currentSavedAmount = newSavedAmount,
                            isCompleted = isCompleted,
                            lastUpdated = System.currentTimeMillis(),
                        ),
                    )
                } catch (e: Exception) {
                    android.util.Log.e("SavingGoalViewModel", "Error withdrawing from goal", e)
                }
            }
        }
    }
