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
            FEATURE_LEARN_FINANCE_ENABLED to false,
            FINANCE_ARTICLES_JSON to "",
            MODEL_EMULATOR_FILE to "",
            MODEL_EMULATOR_URL to "",
            MODEL_DEVICE_FILE to "",
            MODEL_DEVICE_URL to ""
        ))
        fetchAndActivate()
    }

    fun fetchAndActivate() {
        remoteConfig.fetchAndActivate()
    }

    fun isLearnFinanceEnabled(): Boolean {
        return remoteConfig.getBoolean(FEATURE_LEARN_FINANCE_ENABLED)
    }

    fun getFinanceArticlesJson(): String {
        return remoteConfig.getString(FINANCE_ARTICLES_JSON)
    }

    fun getEmulatorModelFile(): String {
        return remoteConfig.getString(MODEL_EMULATOR_FILE)
    }

    fun getEmulatorModelUrl(): String {
        return remoteConfig.getString(MODEL_EMULATOR_URL)
    }

    fun getDeviceModelFile(): String {
        return remoteConfig.getString(MODEL_DEVICE_FILE)
    }

    fun getDeviceModelUrl(): String {
        return remoteConfig.getString(MODEL_DEVICE_URL)
    }

    companion object {
        private const val FEATURE_LEARN_FINANCE_ENABLED = "feature_learn_finance_enabled"
        private const val FINANCE_ARTICLES_JSON = "finance_articles_json"
        private const val MODEL_EMULATOR_FILE = "model_emulator_file"
        private const val MODEL_EMULATOR_URL = "model_emulator_url"
        private const val MODEL_DEVICE_FILE = "model_device_file"
        private const val MODEL_DEVICE_URL = "model_device_url"
    }
}
