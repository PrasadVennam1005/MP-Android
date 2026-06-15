package prasad.vennam.moneypilot.domain.usecase

interface LoanReminderScheduler {
    suspend fun checkAndTriggerLoanReminders()
}
