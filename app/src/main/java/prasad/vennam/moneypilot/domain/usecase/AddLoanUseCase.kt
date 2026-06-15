package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class AddLoanUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(loan: Loan) {
            userPreferences.setSynced(false)
            repository.insertLoan(loan)
        }
    }
