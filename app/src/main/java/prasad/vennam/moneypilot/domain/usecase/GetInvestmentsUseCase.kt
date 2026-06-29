package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.repository.InvestmentRepository
import javax.inject.Inject

class GetInvestmentsUseCase
    @Inject
    constructor(
        private val repository: InvestmentRepository,
    ) {
        operator fun invoke(): Flow<List<Investment>> = repository.allInvestments
    }
