package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.dao.NotificationDao
import prasad.vennam.moneypilot.data.entity.Notification
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel
    @Inject
    constructor(
        private val notificationDao: NotificationDao,
        private val userPreferences: prasad.vennam.moneypilot.data.UserPreferences,
    ) : ViewModel() {
        val notifications: StateFlow<List<Notification>> =
            notificationDao
                .getAllNotifications()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        init {
            seedInitialNotifications()
        }

        private fun seedInitialNotifications() {
            viewModelScope.launch {
                val hasSeeded = userPreferences.hasSeededNotifications.first()
                if (!hasSeeded) {
                    if (notificationDao.getAllNotifications().first().isEmpty()) {
                        val defaultNotifications =
                            listOf(
                                Notification(
                                    title = "Welcome to MoneyPilot! 🚀",
                                    message = "Start tracking your financial journey by adding your first transaction on the dashboard.",
                                    category = "System",
                                    timestamp = System.currentTimeMillis() - 60000 * 5, // 5 mins ago
                                ),
                                Notification(
                                    title = "Smart Budget Tracking 💡",
                                    message = "Set up category-specific spending targets in the Reports tab to build healthy savings habits.",
                                    category = "Budget",
                                    timestamp = System.currentTimeMillis() - 3600000 * 2, // 2 hours ago
                                ),
                                Notification(
                                    title = "Sync with Google Sheets ☁️",
                                    message = "Back up and access your data anytime by signing in with Google. Live updates automatically sync directly to your spreadsheet.",
                                    category = "Sync",
                                    timestamp = System.currentTimeMillis() - 3600000 * 24, // 24 hours ago
                                ),
                                Notification(
                                    title = "Live Exchange Rates 📈",
                                    message = "Your preferred currency rates are now updated dynamically relative to USD.",
                                    category = "Alerts",
                                    timestamp = System.currentTimeMillis() - 3600000 * 48, // 48 hours ago
                                ),
                            )
                        notificationDao.insertNotifications(defaultNotifications)
                    }
                    userPreferences.setNotificationsSeeded(true)
                }
            }
        }

        fun deleteNotification(id: Long) {
            viewModelScope.launch {
                notificationDao.deleteNotification(id)
            }
        }

        fun clearAll() {
            viewModelScope.launch {
                notificationDao.clearAllNotifications()
            }
        }
    }
