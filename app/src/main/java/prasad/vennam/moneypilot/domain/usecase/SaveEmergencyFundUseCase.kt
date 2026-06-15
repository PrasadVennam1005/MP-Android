package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.EmergencyFund
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class SaveEmergencyFundUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(emergencyFund: EmergencyFund) {
            userPreferences.setSynced(false)
            repository.insertEmergencyFund(emergencyFund)
        }

        suspend operator fun invoke(saved: Double) {
            userPreferences.setSynced(false)
            val current = repository.getEmergencyFundSync()
            if (current != null) {
                repository.insertEmergencyFund(current.copy(currentSaved = saved))
            } else {
                repository.insertEmergencyFund(EmergencyFund(id = 1, monthlyExpenses = 0.0, targetMonths = 6, currentSaved = saved))
            }
        }
    }
