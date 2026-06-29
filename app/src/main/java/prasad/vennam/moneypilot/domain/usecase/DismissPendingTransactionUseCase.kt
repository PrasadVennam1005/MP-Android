package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.entity.PendingTransaction
import prasad.vennam.moneypilot.data.repository.TransactionRepository
import javax.inject.Inject

class DismissPendingTransactionUseCase
    @Inject
    constructor(
        private val repository: TransactionRepository,
    ) {
        suspend operator fun invoke(pending: PendingTransaction) {
            repository.deletePendingTransaction(pending)
        }
    }
