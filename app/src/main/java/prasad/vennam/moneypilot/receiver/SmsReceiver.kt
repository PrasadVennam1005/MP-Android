package prasad.vennam.moneypilot.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.entity.PendingTransaction
import prasad.vennam.moneypilot.data.repository.TransactionRepository
import prasad.vennam.moneypilot.util.NotificationParser
import prasad.vennam.moneypilot.util.inRupees
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    @Inject
    lateinit var repository: TransactionRepository

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        // Extract sender and compile the full body text from multipart SMS parts
        val sender = messages[0].displayOriginatingAddress ?: ""
        val bodyBuilder = StringBuilder()
        for (msg in messages) {
            bodyBuilder.append(msg.displayMessageBody)
        }
        val fullBodyText = bodyBuilder.toString()

        if (prasad.vennam.moneypilot.BuildConfig.DEBUG) {
            val maskedSender = sender.take(3) + "..."
            val maskedBody = fullBodyText.take(5) + "...(length=${fullBodyText.length})"
            Log.d("SmsReceiver", "Intercepted SMS: sender='$maskedSender', body='$maskedBody'")
        }

        // Parse utilizing the existing NotificationParser patterns
        val parsed = NotificationParser.parse(sender, fullBodyText, null) ?: return
        if (prasad.vennam.moneypilot.BuildConfig.DEBUG) {
            val maskedMerchant = parsed.merchant.take(2) + "..."
            Log.d("SmsReceiver", "Parsed SMS successfully: type=${parsed.type}, merchant='$maskedMerchant', amount=${parsed.amount}")
        }

        receiverScope.launch {
            try {
                val now = System.currentTimeMillis()
                val timeWindowMs = 10 * 60 * 1000 // 10 minutes duplicate check window

                // 1. Check pending transactions for duplicates
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
                        Log.d("SmsReceiver", "Skipping duplicate SMS: already in pending queue")
                    }
                    return@launch
                }

                // 2. Check approved transactions for duplicates
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
                        Log.d("SmsReceiver", "Skipping duplicate SMS: already in transactions database")
                    }
                    return@launch
                }

                // 3. Insert into staging pending queue
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
                    Log.d("SmsReceiver", "Inserted pending transaction from SMS: id=${pendingTx.id}")
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error saving SMS pending transaction", e)
            }
        }
    }
}
