package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class SaveTransactionUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(transaction: Transaction) {
            userPreferences.setSynced(false)
            if (transaction.id == 0L) {
                repository.insertTransaction(transaction)
            } else {
                repository.updateTransaction(transaction)
            }
        }
    }
