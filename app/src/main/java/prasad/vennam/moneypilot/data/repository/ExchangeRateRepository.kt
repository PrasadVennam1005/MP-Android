package prasad.vennam.moneypilot.data.repository

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.dao.ExchangeRateDao
import prasad.vennam.moneypilot.data.dao.NotificationDao
import prasad.vennam.moneypilot.data.entity.ExchangeRate
import prasad.vennam.moneypilot.data.entity.Notification
import prasad.vennam.moneypilot.data.model.FrankfurterCurrencyItem
import prasad.vennam.moneypilot.data.model.FrankfurterRateResponseItem
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class ExchangeRateRepository
    @Inject
    constructor(
        private val exchangeRateDao: ExchangeRateDao,
        private val notificationDao: NotificationDao,
        private val userPreferences: UserPreferences,
        @ApplicationContext private val context: Context,
        private val client: OkHttpClient,
        private val moshi: Moshi,
    ) {
        val allRates: Flow<Map<String, Double>> =
            exchangeRateDao.getAllRates().map { list ->
                list.associate { it.currencyCode to it.rateAgainstUSD }
            }

        private suspend fun <T> executeWithRetry(
            times: Int = 3,
            initialDelayMs: Long = 1000L,
            block: suspend () -> T?,
        ): T? {
            var lastThrowable: Throwable? = null
            for (attempt in 1..times) {
                try {
                    return block()
                } catch (e: IOException) {
                    lastThrowable = e
                    if (attempt < times) {
                        delay((initialDelayMs * attempt).milliseconds)
                    }
                }
            }
            lastThrowable?.let { throw it }
            return null
        }

        suspend fun syncRates() {
            withContext(Dispatchers.IO) {
                runCatching {
                    executeWithRetry {
                        val req =
                            Request
                                .Builder()
                                .url("https://api.frankfurter.dev/v2/rates?base=USD")
                                .build()
                        client.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) return@executeWithRetry null
                            val type = Types.newParameterizedType(List::class.java, FrankfurterRateResponseItem::class.java)
                            val adapter = moshi.adapter<List<FrankfurterRateResponseItem>>(type)
                            adapter.fromJson(resp.body.string())
                        }
                    }?.let { parsed ->
                        val timeLastUpdateUnix = System.currentTimeMillis() / 1000L
                        val ratesList =
                            parsed.map { item ->
                                ExchangeRate(item.quote, item.rate, timeLastUpdateUnix)
                            }
                        val usdRate = ExchangeRate("USD", 1.0, timeLastUpdateUnix)
                        val fullRates = ratesList + usdRate
                        exchangeRateDao.insertRates(fullRates)

                        // Evaluate alerts
                        try {
                            val alerts = userPreferences.rateAlerts.first()
                            val ratesMap = fullRates.associate { it.currencyCode to it.rateAgainstUSD }
                            
                            alerts.forEach { alert ->
                                val rateFrom = ratesMap[alert.from] ?: 0.0
                                val rateTo = ratesMap[alert.to] ?: 0.0
                                if (rateFrom > 0.0) {
                                    val currentRate = rateTo / rateFrom
                                    val triggered = if (alert.isAbove) {
                                        currentRate >= alert.targetRate
                                    } else {
                                        currentRate <= alert.targetRate
                                    }
                                    if (triggered) {
                                        val title = "Currency Alert Triggered! 🚨"
                                        val message = "1 ${alert.from} has crossed your target of ${alert.targetRate} ${alert.to} (Current rate: ${String.format(java.util.Locale.US, "%.4f", currentRate)})"
                                        
                                        val dbNotification = Notification(
                                            title = title,
                                            message = message,
                                            category = "Alerts",
                                            timestamp = System.currentTimeMillis(),
                                            isRead = false,
                                            url = null
                                        )
                                        notificationDao.insertNotification(dbNotification)
                                        sendSystemNotification(title, message)
                                        userPreferences.removeRateAlert(alert)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }

        private fun sendSystemNotification(
            title: String,
            message: String
        ) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel =
                    android.app.NotificationChannel(
                        "currency_alerts_channel",
                        "Currency Rate Alerts",
                        android.app.NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = "Triggers alerts when specified currency exchange thresholds are crossed."
                    }
                notificationManager.createNotificationChannel(channel)
            }

            val intent =
                android.content.Intent(context, prasad.vennam.moneypilot.MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("navigate_to_notifications", true)
                }

            val pendingIntent =
                android.app.PendingIntent.getActivity(
                    context,
                    3000 + (System.currentTimeMillis() % 1000).toInt(),
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                )

            val builder =
                androidx.core.app.NotificationCompat
                    .Builder(context, "currency_alerts_channel")
                    .setSmallIcon(prasad.vennam.moneypilot.R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
        }

        suspend fun fetchHistoricalRates(
            from: String,
            to: String,
            base: String,
            quote: String
        ): List<FrankfurterRateResponseItem> {
            return withContext(Dispatchers.IO) {
                try {
                    val req =
                        Request
                            .Builder()
                            .url("https://api.frankfurter.dev/v2/rates?from=$from&to=$to&base=$base&quotes=$quote")
                            .build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@withContext emptyList()
                        val type = Types.newParameterizedType(List::class.java, FrankfurterRateResponseItem::class.java)
                        val adapter = moshi.adapter<List<FrankfurterRateResponseItem>>(type)
                        adapter.fromJson(resp.body.string()) ?: emptyList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
        }

        suspend fun fetchCurrencies(): List<FrankfurterCurrencyItem> {
            return withContext(Dispatchers.IO) {
                try {
                    val req =
                        Request
                            .Builder()
                            .url("https://api.frankfurter.dev/v2/currencies")
                            .build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) return@withContext emptyList()
                        val type = Types.newParameterizedType(List::class.java, FrankfurterCurrencyItem::class.java)
                        val adapter = moshi.adapter<List<FrankfurterCurrencyItem>>(type)
                        adapter.fromJson(resp.body.string()) ?: emptyList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
        }

        suspend fun getRateForCurrency(code: String): Double = exchangeRateDao.getRate(code) ?: 1.0
    }
