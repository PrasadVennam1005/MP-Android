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
import prasad.vennam.moneypilot.util.RestoreResult
import javax.inject.Inject

sealed interface RestoreState {
    object Idle : RestoreState
    object Checking : RestoreState
    data class NeedAuthorization(val intent: Intent) : RestoreState
    object Success : RestoreState
    object NoBackup : RestoreState
    data class Error(val message: String) : RestoreState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MoneyPilotRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val userData: StateFlow<UserPreferences.UserData?> = userPreferences.userData
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isLoggedIn: StateFlow<Boolean> = userPreferences.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val spreadsheetId: StateFlow<String?> = userPreferences.spreadsheetId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isSynced: StateFlow<Boolean> = userPreferences.isSynced
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val currency: StateFlow<String> = userPreferences.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "INR")

    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val restoreState: StateFlow<RestoreState> = _restoreState.asStateFlow()

    private var lastCheckedEmail: String? = null

    fun saveUserData(userData: UserPreferences.UserData, onComplete: () -> Unit) {
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

        Log.d("MoneyPilotRestore", "checkAndPerformRestore: email=$email, isGuest=$isGuest, currentSpreadsheetId=$currentSpreadsheetId, lastCheckedEmail=$lastCheckedEmail, restoreState=${_restoreState.value}")

        if (email != null && !isGuest && currentSpreadsheetId == null && lastCheckedEmail != email && _restoreState.value !is RestoreState.Checking) {
            Log.d("MoneyPilotRestore", "Triggering backup restore sequence for $email")
            lastCheckedEmail = email
            _restoreState.value = RestoreState.Checking

            viewModelScope.launch {
                Log.d("MoneyPilotRestore", "Launching restore scope coroutine...")
                val result = GoogleSheetsSyncHelper.checkForRestoreAndExecute(
                    context = context,
                    email = email,
                    onSpreadsheetIdFound = { id ->
                        Log.d("MoneyPilotRestore", "onSpreadsheetIdFound: id=$id")
                        userPreferences.saveSpreadsheetId(id)
                    },
                    onRestoreData = { categories, transactions, budgets, investments ->
                        Log.d("MoneyPilotRestore", "onRestoreData invoked: categories=${categories.size}, transactions=${transactions.size}, budgets=${budgets.size}, investments=${investments.size}")
                        repository.restoreBackup(categories, transactions, budgets, investments)
                        userPreferences.setSynced(true)
                    }
                )

                Log.d("MoneyPilotRestore", "Restore check finished with result: $result")
                when (result) {
                    is RestoreResult.Success -> {
                        Log.d("MoneyPilotRestore", "Restore success! Updating state to Success.")
                        _restoreState.value = RestoreState.Success
                    }
                    is RestoreResult.NeedAuthorization -> {
                        Log.d("MoneyPilotRestore", "Restore requires user authorization. Redirecting to intent.")
                        _restoreState.value = RestoreState.NeedAuthorization(result.intent)
                    }
                    is RestoreResult.NoBackupFound -> {
                        Log.d("MoneyPilotRestore", "No backup spreadsheet was found on Google Drive.")
                        _restoreState.value = RestoreState.NoBackup
                    }
                    is RestoreResult.Error -> {
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
}
