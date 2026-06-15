package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class DeleteInvestmentUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(investment: Investment) {
            userPreferences.setSynced(false)
            repository.deleteInvestment(investment)
        }
    }
