package prasad.vennam.moneypilot.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import prasad.vennam.moneypilot.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigHelper @Inject constructor() {
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(mapOf(
            FEATURE_LEARN_FINANCE_ENABLED to false
        ))
        fetchAndActivate()
    }

    fun fetchAndActivate() {
        remoteConfig.fetchAndActivate()
    }

    fun isLearnFinanceEnabled(): Boolean {
        return remoteConfig.getBoolean(FEATURE_LEARN_FINANCE_ENABLED)
    }

    companion object {
        private const val FEATURE_LEARN_FINANCE_ENABLED = "feature_learn_finance_enabled"
    }
}
