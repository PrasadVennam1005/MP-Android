package prasad.vennam.moneypilot.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigHelper
    @Inject
    constructor() {
        private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        init {
            val fetchInterval = if (prasad.vennam.moneypilot.BuildConfig.DEBUG) 0L else 3600L
            val configSettings =
                FirebaseRemoteConfigSettings
                    .Builder()
                    .setMinimumFetchIntervalInSeconds(fetchInterval)
                    .build()
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.setDefaultsAsync(
                mapOf(
                    FEATURE_LEARN_FINANCE_ENABLED to false,
                    FINANCE_ARTICLES_JSON to "",
                    AI_MODEL_CONFIG to "",
                ),
            )
            fetchAndActivate()
        }

        fun fetchAndActivate() {
            remoteConfig.fetchAndActivate()
        }

        fun isLearnFinanceEnabled(): Boolean = remoteConfig.getBoolean(FEATURE_LEARN_FINANCE_ENABLED)

        fun getFinanceArticlesJson(): String = remoteConfig.getString(FINANCE_ARTICLES_JSON)

        private fun getAiModelConfig(): JSONObject? {
            val json = remoteConfig.getString(AI_MODEL_CONFIG)
            return if (json.isNotBlank()) {
                try {
                    JSONObject(json)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }

        fun getEmulatorModelFile(): String = getAiModelConfig()?.optString("model_emulator_file") ?: ""

        fun getEmulatorModelUrl(): String = getAiModelConfig()?.optString("model_emulator_url") ?: ""

        fun getDeviceModelFile(): String = getAiModelConfig()?.optString("model_device_file") ?: ""

        fun getDeviceModelUrl(): String = getAiModelConfig()?.optString("model_device_url") ?: ""

        companion object {
            private const val FEATURE_LEARN_FINANCE_ENABLED = "feature_learn_finance_enabled"
            private const val FINANCE_ARTICLES_JSON = "finance_articles_json"
            private const val AI_MODEL_CONFIG = "ai_model_config"
        }
    }
