package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.entity.PendingTransaction
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class DismissPendingTransactionUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
    ) {
        suspend operator fun invoke(pending: PendingTransaction) {
            repository.deletePendingTransaction(pending)
        }
    }
