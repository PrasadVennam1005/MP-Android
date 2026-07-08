package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.repository.BudgetRepository
import javax.inject.Inject

class GetBudgetsUseCase
    @Inject
    constructor(
        private val repository: BudgetRepository,
    ) {
        operator fun invoke(): Flow<List<Budget>> = repository.allBudgets
    }
