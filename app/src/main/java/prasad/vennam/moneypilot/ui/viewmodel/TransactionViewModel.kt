package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.domain.usecase.DeleteCategoryUseCase
import prasad.vennam.moneypilot.domain.usecase.DeleteTransactionUseCase
import prasad.vennam.moneypilot.domain.usecase.GetCategoriesUseCase
import prasad.vennam.moneypilot.domain.usecase.GetTransactionByIdUseCase
import prasad.vennam.moneypilot.domain.usecase.GetTransactionsUseCase
import prasad.vennam.moneypilot.domain.usecase.RestoreBackupUseCase
import prasad.vennam.moneypilot.domain.usecase.SaveCategoryUseCase
import prasad.vennam.moneypilot.domain.usecase.SaveTransactionUseCase
import javax.inject.Inject

data class TransactionFormState(
    val amount: String = "",
    val note: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: Long? = null,
    val subCategory: String = "",
    val paymentMode: String = "Cash",
    val timestamp: Long = System.currentTimeMillis(),
)

@HiltViewModel
class TransactionViewModel
    @Inject
    constructor(
        private val userPreferences: UserPreferences,
        private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
        private val saveTransactionUseCase: SaveTransactionUseCase,
        private val deleteTransactionUseCase: DeleteTransactionUseCase,
        private val getTransactionsUseCase: GetTransactionsUseCase,
        private val getCategoriesUseCase: GetCategoriesUseCase,
        private val saveCategoryUseCase: SaveCategoryUseCase,
        private val deleteCategoryUseCase: DeleteCategoryUseCase,
        private val restoreBackupUseCase: RestoreBackupUseCase,
    ) : ViewModel() {
        val allTransactions: StateFlow<List<Transaction>> =
            getTransactionsUseCase()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val allCategories: StateFlow<List<Category>> =
            getCategoriesUseCase()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private val _formState = MutableStateFlow(TransactionFormState())
        val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

        fun updateAmount(value: String) {
            _formState.update { it.copy(amount = value) }
        }

        fun updateNote(value: String) {
            _formState.update { it.copy(note = value) }
        }

        fun updateType(value: TransactionType) {
            _formState.update { it.copy(type = value, categoryId = null) }
        }

        fun updateCategory(id: Long?) {
            _formState.update { it.copy(categoryId = id) }
        }

        fun updateSubCategory(value: String) {
            _formState.update { it.copy(subCategory = value) }
        }

        fun updatePaymentMode(value: String) {
            _formState.update { it.copy(paymentMode = value) }
        }

        fun updateTimestamp(value: Long) {
            _formState.update { it.copy(timestamp = value) }
        }

        fun resetFormState() {
            _formState.value = TransactionFormState()
        }

        fun loadTransactionForEdit(id: Long?) {
            if (id == null || id == 0L) {
                resetFormState()
                return
            }
            viewModelScope.launch {
                try {
                    val transaction = getTransactionByIdUseCase(id)
                    transaction?.let {
                        _formState.value =
                            TransactionFormState(
                                amount =
                                    (it.amount / 100.0).let { value ->
                                        if (value % 1 ==
                                            0.0
                                        ) {
                                            value.toLong().toString()
                                        } else {
                                            value.toString()
                                        }
                                    },
                                note = it.note,
                                type = it.type,
                                categoryId = it.categoryId,
                                subCategory = it.subCategory,
                                paymentMode = it.paymentMode,
                                timestamp = it.timestamp,
                            )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TransactionViewModel", "Error loading transaction for edit", e)
                }
            }
        }

        fun saveTransaction(transaction: Transaction) {
            viewModelScope.launch {
                try {
                    saveTransactionUseCase(transaction)
                } catch (e: Exception) {
                    android.util.Log.e("TransactionViewModel", "Error saving transaction", e)
                }
            }
        }

        fun deleteTransaction(transaction: Transaction) {
            viewModelScope.launch {
                try {
                    deleteTransactionUseCase(transaction)
                } catch (e: Exception) {
                    android.util.Log.e("TransactionViewModel", "Error deleting transaction", e)
                }
            }
        }

        fun saveCategory(category: Category) {
            viewModelScope.launch {
                try {
                    saveCategoryUseCase(category)
                } catch (e: Exception) {
                    android.util.Log.e("TransactionViewModel", "Error saving category", e)
                }
            }
        }

        fun deleteCategory(category: Category) {
            viewModelScope.launch {
                try {
                    deleteCategoryUseCase(category)
                } catch (e: Exception) {
                    android.util.Log.e("TransactionViewModel", "Error deleting category", e)
                }
            }
        }

        suspend fun getTransactionById(id: Long): Transaction? = getTransactionByIdUseCase(id)

        suspend fun restoreBackup(
            categories: List<Category>,
            transactions: List<Transaction>,
            budgets: List<Budget>,
            investments: List<Investment>,
        ) {
            restoreBackupUseCase(categories, transactions, budgets, investments)
        }

        suspend fun clearLocalDatabase() {
            restoreBackupUseCase(Category.DEFAULT_CATEGORIES, emptyList(), emptyList(), emptyList())
        }
    }
