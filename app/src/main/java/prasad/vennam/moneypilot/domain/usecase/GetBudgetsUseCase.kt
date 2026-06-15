package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class GetBudgetsUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
    ) {
        operator fun invoke(): Flow<List<Budget>> = repository.allBudgets
    }
