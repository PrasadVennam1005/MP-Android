package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.Notification
import prasad.vennam.moneypilot.data.repository.NotificationRepository
import javax.inject.Inject

class GetNotificationsUseCase
    @Inject
    constructor(
        private val repository: NotificationRepository,
    ) {
        operator fun invoke(): Flow<List<Notification>> = repository.allNotifications
    }
