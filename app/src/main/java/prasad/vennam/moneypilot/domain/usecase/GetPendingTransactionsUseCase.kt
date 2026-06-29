package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.PendingTransaction
import prasad.vennam.moneypilot.data.repository.TransactionRepository
import javax.inject.Inject

class GetPendingTransactionsUseCase
    @Inject
    constructor(
        private val repository: TransactionRepository,
    ) {
        operator fun invoke(): Flow<List<PendingTransaction>> = repository.allPendingTransactions
    }
