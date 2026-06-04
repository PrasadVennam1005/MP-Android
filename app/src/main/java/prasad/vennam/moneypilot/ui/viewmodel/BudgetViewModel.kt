package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import prasad.vennam.moneypilot.data.UserPreferences
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repository: MoneyPilotRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val allBudgets: StateFlow<List<Budget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveBudget(budget: Budget) {
        viewModelScope.launch {
            userPreferences.setSynced(false)
            if (budget.id == 0L) {
                repository.insertBudget(budget)
            } else {
                repository.updateBudget(budget)
            }
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            userPreferences.setSynced(false)
            repository.deleteBudget(budget)
        }
    }
}
