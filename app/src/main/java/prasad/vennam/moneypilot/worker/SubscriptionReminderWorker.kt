package prasad.vennam.moneypilot.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import prasad.vennam.moneypilot.MainActivity
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.dao.NotificationDao
import prasad.vennam.moneypilot.data.entity.Notification
import prasad.vennam.moneypilot.data.repository.SubscriptionRepository
import prasad.vennam.moneypilot.receiver.SubscriptionActionReceiver
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.toMajorUnit
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class SubscriptionReminderWorker
    @AssistedInject
    constructor(
        @Assisted private val context: Context,
        @Assisted params: WorkerParameters,
        private val repository: SubscriptionRepository,
        private val notificationDao: NotificationDao,
        private val userPreferences: UserPreferences,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result =
            withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "SubscriptionReminderWorker started...")
                    val subscriptions = repository.allSubscriptions.first()
                    val existingNotifications = notificationDao.getAllNotifications().first()
                    val currencyCode = userPreferences.currency.first()
                    val now = System.currentTimeMillis()
                    val oneDayFromNow = now + TimeUnit.DAYS.toMillis(1)

                    val todayStart =
                        Calendar
                            .getInstance()
                            .apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis

                    subscriptions.forEach { subscription ->
                        if (!subscription.isNotificationEnabled) return@forEach

                        // Check if next payment date is approaching (within next 24 hours or overdue)
                        if (subscription.nextPaymentDate <= oneDayFromNow) {
                            val title = "Subscription Due: ${subscription.name}"
                            val amountFormatted = CurrencyFormatter.format(subscription.amount.toMajorUnit, currencyCode)
                            val message = "Your subscription ${subscription.name} of $amountFormatted is due. Tap Approve & Log to confirm."

                            // Check if already notified today for this subscription
                            val alreadyNotified =
                                existingNotifications.any {
                                    it.title == title && it.timestamp >= todayStart
                                }

                            if (!alreadyNotified) {
                                Log.d(TAG, "Triggering notification for subscription: ${subscription.name}")

                                // 1. Save to Database
                                val dbNotification =
                                    Notification(
                                        title = title,
                                        message = message,
                                        category = "Alerts",
                                        timestamp = System.currentTimeMillis(),
                                        isRead = false,
                                        url = "moneypilot://subscription/log?id=${subscription.id}",
                                    )
                                notificationDao.insertNotification(dbNotification)

                                // 2. Post notification with Approve & Log action
                                sendSubscriptionNotification(subscription.id, title, message)
                            }
                        }
                    }

                    schedule(context)
                    Result.success()
                } catch (e: Exception) {
                    Log.e(TAG, "Error running subscription reminder check", e)
                    Result.retry()
                }
            }

        private fun sendSubscriptionNotification(
            subscriptionId: Long,
            title: String,
            message: String,
        ) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = subscriptionId.toInt() + NOTIFICATION_ID_OFFSET

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel =
                    NotificationChannel(
                        CHANNEL_ID,
                        "Subscription Alerts",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = "Reminders for recurring subscriptions and billing."
                    }
                notificationManager.createNotificationChannel(channel)
            }

            // Tap content intent (opens app dashboard)
            val contentIntent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("navigate_to_notifications", true)
                }
            val contentPendingIntent =
                PendingIntent.getActivity(
                    context,
                    notificationId,
                    contentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            // Action button intent (Approve & Log)
            val logIntent =
                Intent(context, SubscriptionActionReceiver::class.java).apply {
                    action = ACTION_APPROVE_AND_LOG
                    putExtra(EXTRA_SUBSCRIPTION_ID, subscriptionId)
                    putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                }
            val logPendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    subscriptionId.toInt(),
                    logIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )

            val builder =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(prasad.vennam.moneypilot.R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(contentPendingIntent)
                    .setAutoCancel(true)
                    .addAction(
                        android.R.drawable.ic_menu_save,
                        "Approve & Log",
                        logPendingIntent,
                    )

            notificationManager.notify(notificationId, builder.build())
        }

        companion object {
            private const val TAG = "SubscriptionReminder"
            private const val CHANNEL_ID = "subscription_alerts_channel"
            private const val NOTIFICATION_ID_OFFSET = 3000

            const val ACTION_APPROVE_AND_LOG = "prasad.vennam.moneypilot.action.APPROVE_AND_LOG"
            const val EXTRA_SUBSCRIPTION_ID = "subscription_id"
            const val EXTRA_NOTIFICATION_ID = "notification_id"

            fun schedule(context: Context) {
                val calendar = Calendar.getInstance()
                val now = calendar.timeInMillis

                // Run daily at 9:00 AM
                val targetCal =
                    Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                if (targetCal.timeInMillis <= now) {
                    targetCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                val initialDelay = targetCal.timeInMillis - now
                Log.d(TAG, "Scheduling next subscription check in ${initialDelay / 1000 / 60} minutes")

                val workRequest =
                    OneTimeWorkRequestBuilder<SubscriptionReminderWorker>()
                        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                        .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "subscription_reminder_work",
                    ExistingWorkPolicy.REPLACE,
                    workRequest,
                )
            }
        }
    }
