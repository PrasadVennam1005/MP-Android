package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.repository.TransactionRepository
import javax.inject.Inject

class SaveTransactionUseCase
    @Inject
    constructor(
        private val repository: TransactionRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(transaction: Transaction) {
            userPreferences.removeDeletedTransactionId(transaction.id.toString())
            userPreferences.setSynced(false)
            val transactionToSave = transaction.copy(lastUpdated = System.currentTimeMillis())
            if (transaction.id == 0L || repository.getTransactionById(transaction.id) == null) {
                repository.insertTransaction(transactionToSave)
            } else {
                repository.updateTransaction(transactionToSave)
            }
        }
    }
