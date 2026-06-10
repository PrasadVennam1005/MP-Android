package prasad.vennam.moneypilot.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.first
import prasad.vennam.moneypilot.MainActivity
import prasad.vennam.moneypilot.data.dao.LoanDao
import prasad.vennam.moneypilot.data.dao.NotificationDao
import prasad.vennam.moneypilot.data.entity.Notification
import prasad.vennam.moneypilot.util.inRupees
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object LoanNotificationScheduler {
    private const val TAG = "LoanNotification"
    private const val CHANNEL_ID = "loan_reminders_channel"
    private const val NOTIFICATION_ID_OFFSET = 2000

    suspend fun checkAndTriggerLoanReminders(
        context: Context,
        loanDao: LoanDao,
        notificationDao: NotificationDao,
    ) {
        try {
            Log.d(TAG, "Running loan notification reminder check...")
            val loans = loanDao.getAllLoans().first()
            val existingNotifications = notificationDao.getAllNotifications().first()
            val now = System.currentTimeMillis()
            val twoDaysFromNow = now + TimeUnit.DAYS.toMillis(2)
            val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            loans.forEach { loan ->
                if (!loan.isNotificationEnabled) return@forEach

                // Check if next EMI date is approaching (within the next 2 days)
                if (loan.nextEmiDate in (now - TimeUnit.DAYS.toMillis(1))..twoDaysFromNow) {
                    val nextEmiDateStr = dateFormatter.format(Date(loan.nextEmiDate))
                    val lenderText = if (loan.lenderName.isNotBlank()) " (${loan.lenderName})" else ""
                    val title = "EMI Reminder: ${loan.name}"
                    val message = "Your monthly EMI of ${loan.emiAmount.inRupees} ${loan.currencyCode} for ${loan.name}$lenderText is due on $nextEmiDateStr."

                    // Check if the user has already been notified for this specific EMI due date
                    val alreadyNotified = existingNotifications.any { it.title == title && it.message == message }
                    if (!alreadyNotified) {
                        Log.d(TAG, "EMI reminder triggered for loan: ${loan.name}")

                        // 1. Save to database notifications
                        val dbNotification = Notification(
                            title = title,
                            message = message,
                            category = "Alerts",
                            timestamp = System.currentTimeMillis(),
                            isRead = false,
                            url = null
                        )
                        notificationDao.insertNotification(dbNotification)

                        // 2. Post Android system notification
                        sendSystemNotification(context, loan.id.toInt() + NOTIFICATION_ID_OFFSET, title, message)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking loan reminders", e)
        }
    }

    private fun sendSystemNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Loan EMI Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming loan monthly installments."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to_notifications", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(prasad.vennam.moneypilot.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}
