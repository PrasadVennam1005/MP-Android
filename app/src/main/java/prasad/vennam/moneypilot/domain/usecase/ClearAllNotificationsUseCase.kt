package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.repository.NotificationRepository
import javax.inject.Inject

class ClearAllNotificationsUseCase
    @Inject
    constructor(
        private val repository: NotificationRepository,
    ) {
        suspend operator fun invoke() {
            repository.clearAllNotifications()
        }
    }
