package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class GetTransactionByIdUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
    ) {
        suspend operator fun invoke(id: Long): Transaction? = repository.getTransactionById(id)
    }
