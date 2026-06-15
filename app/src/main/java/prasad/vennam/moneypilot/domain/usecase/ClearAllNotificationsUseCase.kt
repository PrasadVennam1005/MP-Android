package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class ClearAllNotificationsUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
    ) {
        suspend operator fun invoke() {
            repository.clearAllNotifications()
        }
    }
