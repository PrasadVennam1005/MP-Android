package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.EmergencyFund
import prasad.vennam.moneypilot.data.repository.GoalRepository
import javax.inject.Inject

class GetEmergencyFundUseCase
    @Inject
    constructor(
        private val repository: GoalRepository,
    ) {
        operator fun invoke(): Flow<EmergencyFund?> = repository.emergencyFund
    }
