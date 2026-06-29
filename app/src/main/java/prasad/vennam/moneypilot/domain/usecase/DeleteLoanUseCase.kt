package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.data.repository.LoanRepository
import javax.inject.Inject

class DeleteLoanUseCase
    @Inject
    constructor(
        private val repository: LoanRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(loan: Loan) {
            userPreferences.setSynced(false)
            repository.deleteLoan(loan)
        }
    }
