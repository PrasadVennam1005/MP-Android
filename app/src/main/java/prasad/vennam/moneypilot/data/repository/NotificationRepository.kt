package prasad.vennam.moneypilot.data.repository

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.dao.NotificationDao
import prasad.vennam.moneypilot.data.entity.Notification
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository
    @Inject
    constructor(
        private val notificationDao: NotificationDao,
    ) {
        val allNotifications: Flow<List<Notification>> = notificationDao.getAllNotifications()

        suspend fun insertNotification(notification: Notification) = notificationDao.insertNotification(notification)

        suspend fun insertNotifications(notifications: List<Notification>) = notificationDao.insertNotifications(notifications)

        suspend fun deleteNotification(id: Long) = notificationDao.deleteNotification(id)

        suspend fun clearAllNotifications() = notificationDao.clearAllNotifications()
    }
