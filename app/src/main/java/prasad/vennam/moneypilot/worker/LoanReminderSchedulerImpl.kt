package prasad.vennam.moneypilot.worker

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import prasad.vennam.moneypilot.data.dao.LoanDao
import prasad.vennam.moneypilot.data.dao.NotificationDao
import prasad.vennam.moneypilot.domain.usecase.LoanReminderScheduler
import javax.inject.Inject

class LoanReminderSchedulerImpl
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val loanDao: LoanDao,
    private val notificationDao: NotificationDao,
) : LoanReminderScheduler {
    override suspend fun checkAndTriggerLoanReminders() {
        LoanNotificationScheduler.checkAndTriggerLoanReminders(
            context = context,
            loanDao = loanDao,
            notificationDao = notificationDao,
        )
    }
}
