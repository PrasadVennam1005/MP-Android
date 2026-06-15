package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.*
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class RestoreBackupUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(
            categories: List<Category>,
            transactions: List<Transaction>,
            budgets: List<Budget>,
            investments: List<Investment>,
            loans: List<Loan> = emptyList(),
            emergencyFund: EmergencyFund? = null,
        ) {
            repository.restoreBackup(categories, transactions, budgets, investments, loans, emergencyFund)
            userPreferences.setSynced(true)
        }
    }
