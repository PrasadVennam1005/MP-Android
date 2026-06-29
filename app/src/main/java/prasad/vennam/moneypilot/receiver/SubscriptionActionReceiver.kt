package prasad.vennam.moneypilot.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.data.repository.SubscriptionRepository
import prasad.vennam.moneypilot.data.repository.TransactionRepository
import prasad.vennam.moneypilot.worker.SubscriptionReminderWorker
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class SubscriptionActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var subscriptionRepository: SubscriptionRepository

    @Inject
    lateinit var transactionRepository: TransactionRepository

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != SubscriptionReminderWorker.ACTION_APPROVE_AND_LOG) return
        val subscriptionId = intent.getLongExtra(SubscriptionReminderWorker.EXTRA_SUBSCRIPTION_ID, -1L)
        val notificationId = intent.getIntExtra(SubscriptionReminderWorker.EXTRA_NOTIFICATION_ID, -1)

        if (subscriptionId == -1L) return

        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                // 1. Fetch Subscription
                val subscription = subscriptionRepository.getSubscriptionById(subscriptionId)
                if (subscription != null) {
                    Log.d("SubscriptionReceiver", "Auto-logging subscription: ${subscription.name}")

                    // 2. Insert transaction to primary ledger
                    val transaction =
                        Transaction(
                            amount = subscription.amount,
                            type = TransactionType.EXPENSE,
                            categoryId = subscription.categoryId,
                            note = "Paid: ${subscription.name}",
                            paymentMode = subscription.paymentMode,
                            timestamp = System.currentTimeMillis(),
                        )
                    transactionRepository.insertTransaction(transaction)

                    // 3. Update subscription's nextPaymentDate to next cycle
                    val nextDate = calculateNextPaymentDate(subscription.nextPaymentDate, subscription.billingCycle)
                    subscriptionRepository.updateSubscription(
                        subscription.copy(
                            nextPaymentDate = nextDate,
                            lastUpdated = System.currentTimeMillis(),
                        ),
                    )
                    Log.d("SubscriptionReceiver", "Successfully logged transaction and advanced billing cycle.")
                }

                // 4. Dismiss notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                }
            } catch (e: Exception) {
                Log.e("SubscriptionReceiver", "Failed to auto-log subscription", e)
            } finally {
                pendingResult.finish()
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
}
