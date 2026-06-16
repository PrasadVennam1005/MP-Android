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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.entity.PendingTransaction
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import prasad.vennam.moneypilot.util.NotificationParser
import prasad.vennam.moneypilot.util.inRupees
import javax.inject.Inject

@AndroidEntryPoint
class TransactionNotificationListener : NotificationListenerService() {
    @Inject
    lateinit var repository: MoneyPilotRepository

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
                val timeWindowMs = 10 * 60 * 1000

                // Check pending transactions for duplicates
                val currentPending = repository.allPendingTransactions.first()
                val isPendingDuplicate =
                    currentPending.any { pending ->
                        Math.abs(pending.timestamp - now) < timeWindowMs &&
                            Math.abs(pending.amount - parsed.amount) < 0.01 &&
                            (
                                pending.merchant.equals(parsed.merchant, ignoreCase = true) ||
                                    pending.rawMessage.contains(parsed.merchant, ignoreCase = true)
                            )
                    }
                if (isPendingDuplicate) {
                    if (prasad.vennam.moneypilot.BuildConfig.DEBUG) {
                        Log.d("NotificationListener", "Skipping: duplicate pending transaction found")
                    }
                    return@launch
                }

                // Check approved transactions for duplicates
                val currentTransactions = repository.allTransactions.first()
                val isTransactionDuplicate =
                    currentTransactions.any { trans ->
                        Math.abs(trans.timestamp - now) < timeWindowMs &&
                            Math.abs(trans.amount.inRupees - parsed.amount) < 0.01 &&
                            (
                                trans.note.equals(parsed.merchant, ignoreCase = true) ||
                                    trans.note.contains(parsed.merchant, ignoreCase = true)
                            )
                    }
                if (isTransactionDuplicate) {
                    if (prasad.vennam.moneypilot.BuildConfig.DEBUG) {
                        Log.d("NotificationListener", "Skipping: duplicate transaction already recorded in database")
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
            } catch (e: Exception) {
                Log.e("NotificationListener", "Error inserting pending transaction", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d("NotificationListener", "TransactionNotificationListener destroyed")
    }
}
