package prasad.vennam.moneypilot.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.entity.PendingTransaction
import prasad.vennam.moneypilot.data.repository.TransactionRepository
import prasad.vennam.moneypilot.data.repository.SubscriptionRepository
import prasad.vennam.moneypilot.util.NotificationParser
import prasad.vennam.moneypilot.util.toMajorUnit
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class TransactionNotificationListener : NotificationListenerService() {
    @Inject
    lateinit var repository: TransactionRepository

    @Inject
    lateinit var subscriptionRepository: SubscriptionRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationListener", "TransactionNotificationListener created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val fullBodyText = if (bigText.length > text.length) bigText else text

        if (prasad.vennam.moneypilot.BuildConfig.DEBUG) {
            val maskedTitle = title.take(3) + "...(length=${title.length})"
            val maskedBody = fullBodyText.take(5) + "...(length=${fullBodyText.length})"
            Log.d(
                "NotificationListener",
                "Intercepted notification: title='$maskedTitle', body='$maskedBody' from pkg='${sbn.packageName}'",
            )
        }

        val parsed = NotificationParser.parse(title, fullBodyText, sbn.packageName) ?: return
        if (prasad.vennam.moneypilot.BuildConfig.DEBUG) {
            val maskedMerchant = parsed.merchant.take(2) + "..."
            val maskedBank = parsed.bankAccount.let { it.take(2) + "..." }
            Log.d("NotificationListener", "Parsed successfully: type=${parsed.type}, merchant='$maskedMerchant', bank='$maskedBank'")
        }

        serviceScope.launch {
            try {
                val now = System.currentTimeMillis()

                // 10 minutes duplicate check window
                val timeWindowMs = 10L * 60 * 1000

                if (repository.isDuplicateTransaction(now, timeWindowMs, parsed.amount, parsed.merchant)) {
                    if (prasad.vennam.moneypilot.BuildConfig.DEBUG) {
                        Log.d("NotificationListener", "Skipping: duplicate transaction found in pending or approved")
                    }
                    return@launch
                }

                // Insert into staging queue
                val pendingTx =
                    PendingTransaction(
                        amount = parsed.amount,
                        type = parsed.type,
                        merchant = parsed.merchant,
                        bankAccount = parsed.bankAccount,
                        rawMessage = fullBodyText,
                        timestamp = now,
                    )
                repository.insertPendingTransaction(pendingTx)
                if (prasad.vennam.moneypilot.BuildConfig.DEBUG) {
                    Log.d("NotificationListener", "Successfully inserted pending transaction: id=${pendingTx.id}, type=${pendingTx.type}")
                }

                // 2. Auto-match active subscription renewals and advance next payment date
                val subscriptions = subscriptionRepository.allSubscriptions.first()
                val matchedSubscription = subscriptions.find { subscription ->
                    val subNameClean = subscription.name.lowercase().replace(Regex("[^a-z0-9]"), "")
                    val merchantClean = parsed.merchant.lowercase().replace(Regex("[^a-z0-9]"), "")
                    val nameMatches = (subNameClean.isNotEmpty() && merchantClean.isNotEmpty()) &&
                            (subNameClean.contains(merchantClean) || merchantClean.contains(subNameClean))
                    val amountMatches = Math.abs(subscription.amount - (parsed.amount * 100).toLong()) < 500 // ₹5 allowance
                    nameMatches && amountMatches
                }

                if (matchedSubscription != null) {
                    Log.d("NotificationListener", "Auto-matched subscription renewal: ${matchedSubscription.name}")
                    val nextDate = calculateNextPaymentDate(matchedSubscription.nextPaymentDate, matchedSubscription.billingCycle)
                    subscriptionRepository.updateSubscription(
                        matchedSubscription.copy(
                            nextPaymentDate = nextDate,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                    Log.d("NotificationListener", "Advanced subscription billing date to: $nextDate")
                }
            } catch (e: Exception) {
                Log.e("NotificationListener", "Error inserting pending transaction or processing subscription", e)
            }
        }
    }

    private fun calculateNextPaymentDate(
        currentDate: Long,
        billingCycle: String,
    ): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentDate }
        when (billingCycle) {
            "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "Monthly" -> cal.add(Calendar.MONTH, 1)
            "Yearly" -> cal.add(Calendar.YEAR, 1)
            else -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d("NotificationListener", "TransactionNotificationListener destroyed")
    }
}
