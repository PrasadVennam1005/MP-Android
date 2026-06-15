package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.entity.Notification
import prasad.vennam.moneypilot.domain.usecase.AddBookmarkUseCase
import prasad.vennam.moneypilot.domain.usecase.ClearAllNotificationsUseCase
import prasad.vennam.moneypilot.domain.usecase.DeleteNotificationUseCase
import prasad.vennam.moneypilot.domain.usecase.GetNotificationsUseCase
import prasad.vennam.moneypilot.domain.usecase.InsertNotificationsUseCase
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel
    @Inject
    constructor(
        private val userPreferences: prasad.vennam.moneypilot.data.UserPreferences,
        private val getNotificationsUseCase: GetNotificationsUseCase,
        private val insertNotificationsUseCase: InsertNotificationsUseCase,
        private val deleteNotificationUseCase: DeleteNotificationUseCase,
        private val clearAllNotificationsUseCase: ClearAllNotificationsUseCase,
        private val addBookmarkUseCase: AddBookmarkUseCase,
    ) : ViewModel() {
        val notifications: StateFlow<List<Notification>> =
            getNotificationsUseCase()
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
                    if (getNotificationsUseCase().first().isEmpty()) {
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
                        insertNotificationsUseCase(defaultNotifications)
                    }
                    userPreferences.setNotificationsSeeded(true)
                }
            }
        }

        fun deleteNotification(id: Long) {
            viewModelScope.launch {
                deleteNotificationUseCase(id)
            }
        }

        fun clearAll() {
            viewModelScope.launch {
                clearAllNotificationsUseCase()
            }
        }

        fun bookmarkNotificationUrl(
            title: String,
            url: String,
        ) {
            viewModelScope.launch {
                val currency = userPreferences.currency.first()
                addBookmarkUseCase(title, url, currency)
            }
        }
    }
