package prasad.vennam.moneypilot.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import prasad.vennam.moneypilot.util.WorkManagerSyncScheduler
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences
    @Inject
    constructor(
        private val context: Context,
    ) {
        data class UserData(
            val name: String,
            val email: String,
            val photoUrl: String? = null,
        )

        /** Named constants for the theme mode integer stored in DataStore. */
        object ThemeMode {
            const val SYSTEM = 0
            const val LIGHT = 1
            const val DARK = 2
        }

        private val isLoggedInKey = booleanPreferencesKey("is_logged_in")
        private val userNameKey =
            androidx.datastore.preferences.core
                .stringPreferencesKey("user_name")
        private val userEmailKey =
            androidx.datastore.preferences.core
                .stringPreferencesKey("user_email")
        private val userPhotoUrlKey =
            androidx.datastore.preferences.core
                .stringPreferencesKey("user_photo_url")
        private val isSyncedKey = booleanPreferencesKey("is_synced")
        private val spreadsheetIdKey =
            androidx.datastore.preferences.core
                .stringPreferencesKey("spreadsheet_id")
        private val currencyKey =
            androidx.datastore.preferences.core
                .stringPreferencesKey("selected_currency")
        private val financialGoalKey =
            androidx.datastore.preferences.core
                .stringPreferencesKey("financial_goal")
        private val monthlySavingsTargetKey =
            androidx.datastore.preferences.core
                .longPreferencesKey("monthly_savings_target")
        private val isOnboardingCompletedKey = booleanPreferencesKey("is_onboarding_completed")
        private val themeModeKey =
            androidx.datastore.preferences.core
                .intPreferencesKey("theme_mode")
        private val hasSeededNotificationsKey = booleanPreferencesKey("has_seeded_notifications")


        val isLoggedIn: Flow<Boolean> =
            context.dataStore.data
                .map { preferences ->
                    preferences[isLoggedInKey] ?: false
                }

        val currency: Flow<String> =
            context.dataStore.data
                .map { preferences ->
                    preferences[currencyKey] ?: "INR"
                }

        val financialGoal: Flow<String> =
            context.dataStore.data
                .map { preferences ->
                    preferences[financialGoalKey] ?: "Track Expenses"
                }

        val monthlySavingsTarget: Flow<Long> =
            context.dataStore.data
                .map { preferences ->
                    preferences[monthlySavingsTargetKey] ?: 20L
                }

        val isOnboardingCompleted: Flow<Boolean> =
            context.dataStore.data
                .map { preferences ->
                    preferences[isOnboardingCompletedKey] ?: false
                }

        val themeMode: Flow<Int> =
            context.dataStore.data
                .map { preferences ->
                    preferences[themeModeKey] ?: 0 // Default to SYSTEM (0)
                }

        val hasSeededNotifications: Flow<Boolean> =
            context.dataStore.data
                .map { preferences ->
                    preferences[hasSeededNotificationsKey] ?: false
                }

        val isSynced: Flow<Boolean> =
            context.dataStore.data
                .map { preferences ->
                    preferences[isSyncedKey] ?: false
                }

        val spreadsheetId: Flow<String?> =
            context.dataStore.data
                .map { preferences ->
                    preferences[spreadsheetIdKey]
                }

        val userData: Flow<UserData?> =
            context.dataStore.data
                .map { preferences ->
                    val name = preferences[userNameKey]
                    if (name != null) {
                        UserData(
                            name = name,
                            email = preferences[userEmailKey] ?: "",
                            photoUrl = preferences[userPhotoUrlKey],
                        )
                    } else {
                        null
                    }
                }

        suspend fun saveUserData(userData: UserData) {
            context.dataStore.edit { preferences ->
                preferences[isLoggedInKey] = true
                preferences[userNameKey] = userData.name
                if (preferences[userEmailKey] != userData.email) {
                    preferences[isSyncedKey] = false
                }
                preferences[userEmailKey] = userData.email
                userData.photoUrl?.let { preferences[userPhotoUrlKey] = it }
            }
        }

        suspend fun clearUserData() {
            context.dataStore.edit { preferences ->
                preferences[isLoggedInKey] = false
                preferences.remove(userNameKey)
                preferences.remove(userEmailKey)
                preferences.remove(userPhotoUrlKey)
                preferences.remove(isSyncedKey)
                preferences.remove(spreadsheetIdKey)
            }
        }

        suspend fun setSynced(synced: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[isSyncedKey] = synced
            }
            if (!synced) {
                WorkManagerSyncScheduler.scheduleSync(context)
            }
        }

        suspend fun setLoggedIn(loggedIn: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[isLoggedInKey] = loggedIn
            }
        }

        suspend fun saveSpreadsheetId(id: String) {
            context.dataStore.edit { preferences ->
                preferences[spreadsheetIdKey] = id
            }
        }

        suspend fun clearSpreadsheetId() {
            context.dataStore.edit { preferences ->
                preferences.remove(spreadsheetIdKey)
            }
        }

        suspend fun setCurrency(currencyCode: String) {
            context.dataStore.edit { preferences ->
                preferences[currencyKey] = currencyCode
            }
        }

        suspend fun savePreferences(
            goal: String,
            target: Long,
            currencyCode: String,
        ) {
            context.dataStore.edit { preferences ->
                preferences[financialGoalKey] = goal
                preferences[monthlySavingsTargetKey] = target
                preferences[currencyKey] = currencyCode
                preferences[isOnboardingCompletedKey] = true
            }
        }

        suspend fun setOnboardingCompleted(completed: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[isOnboardingCompletedKey] = completed
            }
        }

        suspend fun setThemeMode(mode: Int) {
            context.dataStore.edit { preferences ->
                preferences[themeModeKey] = mode
            }
        }

        suspend fun setNotificationsSeeded(seeded: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[hasSeededNotificationsKey] = seeded
            }
        }
    }
