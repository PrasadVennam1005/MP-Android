package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: MoneyPilotRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTransaction(transaction: Transaction) {
        viewModelScope.launch {
            userPreferences.setSynced(false)
            if (transaction.id == 0L) {
                repository.insertTransaction(transaction)
            } else {
                repository.updateTransaction(transaction)
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            userPreferences.setSynced(false)
            repository.deleteTransaction(transaction)
        }
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        return repository.getTransactionById(id)
    }

    suspend fun restoreBackup(
        categories: List<Category>,
        transactions: List<Transaction>,
        budgets: List<Budget>,
        investments: List<Investment>
    ) {
        repository.restoreBackup(categories, transactions, budgets, investments)
        userPreferences.setSynced(true)
    }

    suspend fun clearLocalDatabase() {
        repository.restoreBackup(Category.DEFAULT_CATEGORIES, emptyList(), emptyList(), emptyList())
        userPreferences.setSynced(true)
    }
}
