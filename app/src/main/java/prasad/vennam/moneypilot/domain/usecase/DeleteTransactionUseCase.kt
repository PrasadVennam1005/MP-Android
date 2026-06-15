package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class DeleteTransactionUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(transaction: Transaction) {
            userPreferences.setSynced(false)
            repository.deleteTransaction(transaction)
        }
    }
