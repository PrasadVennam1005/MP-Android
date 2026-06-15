package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.entity.Notification
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class InsertNotificationsUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
    ) {
        suspend operator fun invoke(notifications: List<Notification>) {
            repository.insertNotifications(notifications)
        }
    }
