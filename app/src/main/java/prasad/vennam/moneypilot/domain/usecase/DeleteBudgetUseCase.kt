package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class DeleteBudgetUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(budget: Budget) {
            userPreferences.setSynced(false)
            repository.deleteBudget(budget)
        }
    }
