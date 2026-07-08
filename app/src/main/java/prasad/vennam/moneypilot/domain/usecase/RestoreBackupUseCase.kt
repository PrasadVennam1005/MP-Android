package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.EmergencyFund
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.repository.DataManagementRepository
import javax.inject.Inject

class RestoreBackupUseCase
    @Inject
    constructor(
        private val repository: DataManagementRepository,
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
