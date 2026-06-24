package prasad.vennam.moneypilot.util

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.DisposableEffect
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface AnalyticsHelper {
    fun logScreenView(screenName: String)

    fun logScreenEngagement(
        screenName: String,
        engagementTimeSeconds: Long,
    )

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

        override fun logScreenEngagement(
            screenName: String,
            engagementTimeSeconds: Long,
        ) {
            logEvent(
                AnalyticsConstants.Event.SCREEN_ENGAGEMENT_TIME,
                mapOf(
                    AnalyticsConstants.Param.SCREEN_NAME to screenName,
                    AnalyticsConstants.Param.ENGAGEMENT_TIME_SEC to engagementTimeSeconds,
                ),
            )
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

@androidx.compose.runtime.Composable
fun TrackScreen(
    analyticsHelper: AnalyticsHelper,
    screenName: String,
) {
    DisposableEffect(screenName) {
        val startTime = System.currentTimeMillis()
        analyticsHelper.logScreenView(screenName)

        onDispose {
            val endTime = System.currentTimeMillis()
            val engagementTimeSeconds = (endTime - startTime) / 1000
            if (engagementTimeSeconds > 0) {
                analyticsHelper.logScreenEngagement(screenName, engagementTimeSeconds)
            }
        }
    }
}
