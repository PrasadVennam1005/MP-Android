package prasad.vennam.moneypilot.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import prasad.vennam.moneypilot.util.GoogleSheetsSyncHelper
import prasad.vennam.moneypilot.util.SyncResult
import javax.inject.Inject

sealed interface RestoreState {
    object Idle : RestoreState

    object Checking : RestoreState

    data class NeedAuthorization(
        val intent: Intent,
    ) : RestoreState

    object Success : RestoreState

    object NoBackup : RestoreState

    data class Error(
        val message: String,
    ) : RestoreState
}

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
        private val userPreferences: UserPreferences,
        private val exchangeRateRepo: prasad.vennam.moneypilot.data.repository.ExchangeRateRepository,
    ) : ViewModel() {
        init {
            viewModelScope.launch {
                exchangeRateRepo.syncRates()
            }
        }

        val userData: StateFlow<UserPreferences.UserData?> =
            userPreferences.userData
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        val isLoggedIn: StateFlow<Boolean> =
            userPreferences.isLoggedIn
                .stateIn(viewModelScope, SharingStarted.Eagerly, false)

        val spreadsheetId: StateFlow<String?> =
            userPreferences.spreadsheetId
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        val isSynced: StateFlow<Boolean> =
            userPreferences.isSynced
                .stateIn(viewModelScope, SharingStarted.Eagerly, false)

        val currency: StateFlow<String> =
            userPreferences.currency
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "INR")

        val isOnboardingCompleted: StateFlow<Boolean> =
            userPreferences.isOnboardingCompleted
                .stateIn(viewModelScope, SharingStarted.Eagerly, false)

        val financialGoal: StateFlow<String> =
            userPreferences.financialGoal
                .stateIn(viewModelScope, SharingStarted.Eagerly, "Track Expenses")

        val monthlySavingsTarget: StateFlow<Long> =
            userPreferences.monthlySavingsTarget
                .stateIn(viewModelScope, SharingStarted.Eagerly, 20L)

        val exchangeRates: StateFlow<Map<String, Double>> =
            exchangeRateRepo.allRates
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        val themeMode: StateFlow<Int> =
            userPreferences.themeMode
                .stateIn(viewModelScope, SharingStarted.Eagerly, 0)



        private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Idle)
        val restoreState: StateFlow<RestoreState> = _restoreState.asStateFlow()

        private var lastCheckedEmail: String? = null

        fun saveUserData(
            userData: UserPreferences.UserData,
            onComplete: () -> Unit,
        ) {
            viewModelScope.launch {
                userPreferences.saveUserData(userData)
                onComplete()
            }
        }

        fun setSynced(synced: Boolean) {
            viewModelScope.launch {
                userPreferences.setSynced(synced)
            }
        }

        fun saveSpreadsheetId(id: String) {
            viewModelScope.launch {
                userPreferences.saveSpreadsheetId(id)
            }
        }

        fun clearSpreadsheetId() {
            viewModelScope.launch {
                userPreferences.clearSpreadsheetId()
            }
        }

        fun checkAndPerformRestore(context: Context) {
            val email = userData.value?.email
            val currentSpreadsheetId = spreadsheetId.value
            val isGuest = email == "guest@moneypilot.app"

            Log.d(
                "MoneyPilotRestore",
                "checkAndPerformRestore: email=$email, isGuest=$isGuest, currentSpreadsheetId=$currentSpreadsheetId, lastCheckedEmail=$lastCheckedEmail, restoreState=${_restoreState.value}",
            )

            if (email != null &&
                !isGuest &&
                currentSpreadsheetId == null &&
                lastCheckedEmail != email &&
                _restoreState.value !is RestoreState.Checking
            ) {
                Log.d("MoneyPilotRestore", "Triggering backup restore sequence for $email")
                lastCheckedEmail = email
                _restoreState.value = RestoreState.Checking

                viewModelScope.launch {
                    Log.d("MoneyPilotRestore", "Launching restore scope coroutine...")
                    val result =
                        GoogleSheetsSyncHelper.performTwoWaySync(
                            context = context,
                            email = email,
                            repository = repository,
                            spreadsheetId = currentSpreadsheetId,
                            isRestore = true,
                            onSpreadsheetIdFound = { id ->
                                Log.d("MoneyPilotRestore", "onSpreadsheetIdFound: id=$id")
                                userPreferences.saveSpreadsheetId(id)
                                userPreferences.setSynced(true)
                            },
                        )

                    Log.d("MoneyPilotRestore", "Restore check finished with result: $result")
                    when (result) {
                        is SyncResult.Success -> {
                            Log.d("MoneyPilotRestore", "Restore success! Updating state to Success.")
                            _restoreState.value = RestoreState.Success
                        }
                        is SyncResult.NeedAuthorization -> {
                            Log.d("MoneyPilotRestore", "Restore requires user authorization. Redirecting to intent.")
                            _restoreState.value = RestoreState.NeedAuthorization(result.intent)
                        }
                        is SyncResult.NoBackupFound -> {
                            Log.d("MoneyPilotRestore", "No backup spreadsheet was found on Google Drive.")
                            _restoreState.value = RestoreState.NoBackup
                        }
                        is SyncResult.Error -> {
                            Log.e("MoneyPilotRestore", "Restore failed with error: ${result.exception.message}", result.exception)
                            _restoreState.value = RestoreState.Error(result.exception.message ?: "Unknown error")
                        }
                    }
                }
            } else {
                Log.d("MoneyPilotRestore", "Restore check conditions not met. Skipping trigger.")
            }
        }

        fun resetRestoreCheck() {
            lastCheckedEmail = null
            _restoreState.value = RestoreState.Idle
        }

        fun logout(onLogoutSuccess: () -> Unit) {
            viewModelScope.launch {
                userPreferences.clearUserData()
                repository.restoreBackup(Category.DEFAULT_CATEGORIES, emptyList(), emptyList(), emptyList())
                userPreferences.setSynced(true)
                resetRestoreCheck()
                onLogoutSuccess()
            }
        }

        fun setCurrency(currencyCode: String) {
            viewModelScope.launch {
                userPreferences.setCurrency(currencyCode)
            }
        }

        fun savePreferences(
            goal: String,
            target: Long,
            currencyCode: String,
        ) {
            viewModelScope.launch {
                userPreferences.savePreferences(goal, target, currencyCode)
            }
        }

        fun resetOnboarding() {
            viewModelScope.launch {
                userPreferences.setOnboardingCompleted(false)
            }
        }

        fun setThemeMode(mode: Int) {
            viewModelScope.launch {
                userPreferences.setThemeMode(mode)
            }
        }

        fun deleteAccount(
            context: Context,
            onComplete: (Boolean) -> Unit,
        ) {
            viewModelScope.launch {
                val email = userData.value?.email
                val currentSpreadsheetId = spreadsheetId.value
                val isGuest = email == "guest@moneypilot.app"

                var deleteSheetSuccess = true

                // 1. Cancel active sync WorkManager
                try {
                    prasad.vennam.moneypilot.util.WorkManagerSyncScheduler.cancelSync(context)
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to cancel sync", e)
                }

                // 2. Delete or clear Google Sheet backup spreadsheet
                if (!isGuest && email != null && currentSpreadsheetId != null) {
                    deleteSheetSuccess = GoogleSheetsSyncHelper.deleteSpreadsheetFile(
                        context = context,
                        email = email,
                        spreadsheetId = currentSpreadsheetId
                    )
                }

                // 3. Clear all local database tables
                try {
                    repository.clearAllData()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to clear database", e)
                }

                // 4. Clear all user preferences in DataStore
                try {
                    userPreferences.clearAll()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to clear user preferences", e)
                }

                resetRestoreCheck()
                onComplete(deleteSheetSuccess)
            }
        }
    }


