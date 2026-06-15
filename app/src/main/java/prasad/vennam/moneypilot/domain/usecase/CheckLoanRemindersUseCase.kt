package prasad.vennam.moneypilot.domain.usecase

import javax.inject.Inject

class CheckLoanRemindersUseCase
    @Inject
    constructor(
        private val loanReminderScheduler: LoanReminderScheduler,
    ) {
        suspend operator fun invoke() {
            loanReminderScheduler.checkAndTriggerLoanReminders()
        }
    }
