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
            val budgetToSave = budget.copy(lastUpdated = System.currentTimeMillis())
            if (budget.id == 0L || repository.getBudgetById(budget.id) == null) {
                repository.insertBudget(budgetToSave)
            } else {
                repository.updateBudget(budgetToSave)
            }
        }
    }
