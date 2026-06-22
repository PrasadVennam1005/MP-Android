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
import prasad.vennam.moneypilot.data.model.RateAlert
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
        private val isPremiumKey = booleanPreferencesKey("is_premium")
        private val isBiometricEnabledKey = booleanPreferencesKey("is_biometric_enabled")
        private val remainingAiScansKey =
            androidx.datastore.preferences.core
                .intPreferencesKey("remaining_ai_scans")
        private val lastAiScanResetDateKey =
            androidx.datastore.preferences.core
                .stringPreferencesKey("last_ai_scan_reset_date")
        private val isDevToolEnabledKey = booleanPreferencesKey("is_dev_tool_enabled")
        private val recentCurrencyPairsKey =
            androidx.datastore.preferences.core
                .stringPreferencesKey("recent_currency_pairs")
        private val favoriteCurrencyPairsKey =
            androidx.datastore.preferences.core
                .stringPreferencesKey("favorite_currency_pairs")
        private val currencyBasketKey =
            androidx.datastore.preferences.core
                .stringPreferencesKey("currency_basket")
        private val rateAlertsKey =
            androidx.datastore.preferences.core
                .stringPreferencesKey("currency_rate_alerts")



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

        val isPremium: Flow<Boolean> =
            context.dataStore.data
                .map { preferences ->
                    preferences[isPremiumKey] ?: false
                }

        val remainingAiScans: Flow<Int> =
            context.dataStore.data
                .map { preferences ->
                    preferences[remainingAiScansKey] ?: 3
                }

        val isBiometricEnabled: Flow<Boolean> =
            context.dataStore.data
                .map { preferences ->
                    preferences[isBiometricEnabledKey] ?: false
                }

        val isDevToolEnabled: Flow<Boolean> =
            context.dataStore.data
                .map { preferences ->
                    preferences[isDevToolEnabledKey] ?: false
                }

        val recentCurrencyPairs: Flow<List<Pair<String, String>>> =
            context.dataStore.data
                .map { preferences ->
                    val raw = preferences[recentCurrencyPairsKey] ?: ""
                    parseCurrencyPairs(raw)
                }

        val favoriteCurrencyPairs: Flow<List<Pair<String, String>>> =
            context.dataStore.data
                .map { preferences ->
                    val raw = preferences[favoriteCurrencyPairsKey] ?: ""
                    parseCurrencyPairs(raw)
                }

        val currencyBasket: Flow<List<String>> =
            context.dataStore.data
                .map { preferences ->
                    val raw = preferences[currencyBasketKey] ?: "EUR,GBP,INR,JPY"
                    if (raw.isEmpty()) emptyList() else raw.split(",")
                }

        val rateAlerts: Flow<List<RateAlert>> =
            context.dataStore.data
                .map { preferences ->
                    val raw = preferences[rateAlertsKey] ?: ""
                    parseRateAlerts(raw)
                }

        private fun parseRateAlerts(raw: String): List<RateAlert> {
            if (raw.isEmpty()) return emptyList()
            return raw.split(";").mapNotNull {
                val parts = it.split(",")
                if (parts.size == 4) {
                    val from = parts[0]
                    val to = parts[1]
                    val rate = parts[2].toDoubleOrNull() ?: return@mapNotNull null
                    val isAbove = parts[3] == "above"
                    RateAlert(from, to, rate, isAbove)
                } else {
                    null
                }
            }
        }


        private fun parseCurrencyPairs(raw: String): List<Pair<String, String>> {
            if (raw.isEmpty()) return emptyList()
            return raw.split(";").mapNotNull {
                val parts = it.split(",")
                if (parts.size == 2) {
                    Pair(parts[0], parts[1])
                } else {
                    null
                }
            }
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

        suspend fun setPremium(premium: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[isPremiumKey] = premium
            }
        }

        suspend fun clear() {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
        }

        suspend fun setIsBiometricEnabled(enabled: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[isBiometricEnabledKey] = enabled
            }
        }

        suspend fun setDevToolEnabled(enabled: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[isDevToolEnabledKey] = enabled
            }
        }

        suspend fun checkAndResetDailyQuota() {
            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            context.dataStore.edit { preferences ->
                val lastResetDate = preferences[lastAiScanResetDateKey]
                if (lastResetDate != currentDate) {
                    val currentScans = preferences[remainingAiScansKey] ?: 3
                    if (currentScans < 3) {
                        preferences[remainingAiScansKey] = 3
                    }
                    preferences[lastAiScanResetDateKey] = currentDate
                }
            }
        }

        suspend fun decrementAiScans() {
            context.dataStore.edit { preferences ->
                val current = preferences[remainingAiScansKey] ?: 3
                preferences[remainingAiScansKey] = (current - 1).coerceAtLeast(0)
            }
        }

        suspend fun incrementAiScans(amount: Int) {
            context.dataStore.edit { preferences ->
                val current = preferences[remainingAiScansKey] ?: 3
                preferences[remainingAiScansKey] = current + amount
            }
        }

        suspend fun saveRecentCurrencyPair(from: String, to: String) {
            context.dataStore.edit { preferences ->
                val raw = preferences[recentCurrencyPairsKey] ?: ""
                val current = parseCurrencyPairs(raw).toMutableList()
                val pair = Pair(from, to)
                current.remove(pair)
                current.add(0, pair)
                val limited = current.take(8)
                preferences[recentCurrencyPairsKey] = limited.joinToString(";") { "${it.first},${it.second}" }
            }
        }

        suspend fun toggleFavoriteCurrencyPair(from: String, to: String) {
            context.dataStore.edit { preferences ->
                val raw = preferences[favoriteCurrencyPairsKey] ?: ""
                val current = parseCurrencyPairs(raw).toMutableList()
                val pair = Pair(from, to)
                if (current.contains(pair)) {
                    current.remove(pair)
                } else {
                    current.add(pair)
                }
                preferences[favoriteCurrencyPairsKey] = current.joinToString(";") { "${it.first},${it.second}" }
            }
        }

        suspend fun addRateAlert(alert: RateAlert) {
            context.dataStore.edit { preferences ->
                val raw = preferences[rateAlertsKey] ?: ""
                val current = parseRateAlerts(raw).toMutableList()
                if (!current.contains(alert)) {
                    current.add(alert)
                }
                preferences[rateAlertsKey] = current.joinToString(";") {
                    "${it.from},${it.to},${it.targetRate},${if (it.isAbove) "above" else "below"}"
                }
            }
        }

        suspend fun removeRateAlert(alert: RateAlert) {
            context.dataStore.edit { preferences ->
                val raw = preferences[rateAlertsKey] ?: ""
                val current = parseRateAlerts(raw).toMutableList()
                current.remove(alert)
                preferences[rateAlertsKey] = current.joinToString(";") {
                    "${it.from},${it.to},${it.targetRate},${if (it.isAbove) "above" else "below"}"
                }
            }
        }

        suspend fun saveCurrencyBasket(basket: List<String>) {
            context.dataStore.edit { preferences ->
                preferences[currencyBasketKey] = basket.joinToString(",")
            }
        }
    }


