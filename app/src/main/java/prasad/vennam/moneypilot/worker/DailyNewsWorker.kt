package prasad.vennam.moneypilot.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.util.Xml
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import prasad.vennam.moneypilot.MainActivity
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.dao.NotificationDao
import prasad.vennam.moneypilot.data.entity.Notification
import java.io.StringReader
import java.net.URL
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class DailyNewsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun notificationDao(): NotificationDao
        fun userPreferences(): UserPreferences
    }

    data class FeedItem(val title: String, val message: String, val url: String)

    override suspend fun doWork(): Result {
        Log.d("DailyNewsWorker", "doWork: Daily news worker started")
        val appContext = applicationContext

        val entryPoint = EntryPointAccessors.fromApplication(appContext, WorkerEntryPoint::class.java)
        val notificationDao = entryPoint.notificationDao()
        val userPreferences = entryPoint.userPreferences()

        val currencyCode = userPreferences.currency.first()
        Log.d("DailyNewsWorker", "doWork: User preferred currency is $currencyCode")

        val rssUrl = when (currencyCode) {
            "INR" -> "https://economictimes.indiatimes.com/wealth/rssfeeds/8375556.cms" // India Wealth / Personal Finance (Economic Times)
            "GBP" -> "https://uk.finance.yahoo.com/news/rssindex" // UK Personal Finance (Yahoo Finance)
            else -> "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=21324812" // Global/US CNBC Personal Finance
        }

        val xmlContent = try {
            withContext(Dispatchers.IO) {
                URL(rssUrl).readText()
            }
        } catch (e: Exception) {
            Log.e("DailyNewsWorker", "Failed to fetch RSS feed from $rssUrl", e)
            schedule(appContext)
            return Result.retry()
        }

        val feedItems = parseRssFeed(xmlContent)
        if (feedItems.isEmpty()) {
            Log.d("DailyNewsWorker", "No news items parsed. Rescheduling.")
            schedule(appContext)
            return Result.success()
        }

        val existing = notificationDao.getAllNotifications().first()
        val existingTitles = existing.map { it.title }.toSet()
        val newItems = feedItems.filter { it.title !in existingTitles }

        val selectedItem = if (newItems.isNotEmpty()) {
            newItems.first()
        } else {
            Log.d("DailyNewsWorker", "No new articles found. Skipping notification.")
            schedule(appContext)
            return Result.success()
        }

        try {
            val newNotification = Notification(
                title = selectedItem.title,
                message = selectedItem.message,
                category = "Alerts",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                url = selectedItem.url
            )
            notificationDao.insertNotification(newNotification)
            sendSystemNotification(appContext, selectedItem.title, selectedItem.message)
        } catch (e: Exception) {
            Log.e("DailyNewsWorker", "Failed to save or post notification", e)
        }

        schedule(appContext)
        return Result.success()
    }

    private fun parseRssFeed(xmlContent: String): List<FeedItem> {
        val items = mutableListOf<FeedItem>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xmlContent))
            var eventType = parser.eventType
            var currentTitle = ""
            var currentLink = ""
            var currentDesc = ""
            var insideItem = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.equals("item", ignoreCase = true)) {
                            insideItem = true
                        } else if (insideItem) {
                            when (name.lowercase(Locale.ROOT)) {
                                "title" -> currentTitle = parser.nextText()
                                "link" -> currentLink = parser.nextText()
                                "description" -> {
                                    val desc = parser.nextText()
                                    currentDesc = desc.replace(Regex("<[^>]*>"), "").trim()
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name.equals("item", ignoreCase = true)) {
                            if (currentTitle.isNotBlank()) {
                                items.add(FeedItem(currentTitle.trim(), currentDesc.trim(), currentLink.trim()))
                            }
                            currentTitle = ""
                            currentLink = ""
                            currentDesc = ""
                            insideItem = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("DailyNewsWorker", "XML parsing error", e)
        }
        return items
    }

    private fun sendSystemNotification(context: Context, title: String, message: String) {
        val channelId = "financial_news_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Financial News & Insights",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily personal finance news and money management tips."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to_notifications", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(prasad.vennam.moneypilot.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1005, builder.build())
    }

    companion object {
        fun schedule(context: Context) {
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis

            val targets = listOf(9, 18)
            var nextTargetTime: Long = 0

            for (hour in targets) {
                val targetCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (targetCal.timeInMillis > now) {
                    nextTargetTime = targetCal.timeInMillis
                    break
                }
            }

            if (nextTargetTime == 0L) {
                val tomorrowCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                nextTargetTime = tomorrowCal.timeInMillis
            }

            val initialDelay = nextTargetTime - now
            Log.d("DailyNewsWorker", "Scheduling next news fetch in ${initialDelay / 1000 / 60} minutes")

            val newsWorkRequest = OneTimeWorkRequestBuilder<DailyNewsWorker>()
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "daily_news_notification_work",
                ExistingWorkPolicy.REPLACE,
                newsWorkRequest
            )
        }
    }
}
