package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.repository.InvestmentRepository
import javax.inject.Inject

class UpdateInvestmentUseCase
    @Inject
    constructor(
        private val repository: InvestmentRepository,
    ) {
        suspend operator fun invoke(investment: Investment) {
            repository.updateInvestment(investment)
        }
    }
