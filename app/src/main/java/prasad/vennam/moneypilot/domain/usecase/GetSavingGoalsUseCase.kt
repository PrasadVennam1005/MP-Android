package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.SavingGoal
import prasad.vennam.moneypilot.data.repository.GoalRepository
import javax.inject.Inject

class GetSavingGoalsUseCase @Inject constructor(
    private val repository: GoalRepository
) {
    operator fun invoke(): Flow<List<SavingGoal>> = repository.allSavingGoals
}
