package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionByIdUseCase
    @Inject
    constructor(
        private val repository: TransactionRepository,
    ) {
        suspend operator fun invoke(id: Long): Transaction? = repository.getTransactionById(id)
    }
