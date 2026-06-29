package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.repository.BudgetRepository
import javax.inject.Inject

class SaveBudgetUseCase
    @Inject
    constructor(
        private val repository: BudgetRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(budget: Budget) {
            userPreferences.setSynced(false)
            if (budget.id == 0L) {
                repository.insertBudget(budget)
            } else {
                repository.updateBudget(budget)
            }
        }
    }
