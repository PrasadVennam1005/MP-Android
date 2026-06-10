package prasad.vennam.moneypilot.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface AnalyticsHelper {
    fun logScreenView(screenName: String)

    fun logEvent(
        event: String,
        params: Map<String, Any> = emptyMap(),
    )

    fun setUserProperty(
        name: String,
        value: String,
    )
}

@Singleton
class FirebaseAnalyticsHelper
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : AnalyticsHelper {
        private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        override fun logScreenView(screenName: String) {
            val bundle =
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
                }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        }

        override fun logEvent(
            event: String,
            params: Map<String, Any>,
        ) {
            val bundle =
                Bundle().apply {
                    params.forEach { (key, value) ->
                        when (value) {
                            is String -> putString(key, value)
                            is Int -> putInt(key, value)
                            is Long -> putLong(key, value)
                            is Double -> putDouble(key, value)
                            is Boolean -> putBoolean(key, value)
                        }
                    }
                }
            firebaseAnalytics.logEvent(event, bundle)
        }

        override fun setUserProperty(
            name: String,
            value: String,
        ) {
            firebaseAnalytics.setUserProperty(name, value)
        }
    }
