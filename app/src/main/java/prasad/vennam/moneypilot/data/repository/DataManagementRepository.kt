package prasad.vennam.moneypilot.data.repository

import androidx.room.withTransaction
import prasad.vennam.moneypilot.data.MoneyPilotDatabase
import prasad.vennam.moneypilot.data.dao.*
import prasad.vennam.moneypilot.data.entity.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Management repository for cross-cutting concerns like backups and clearing all data.
 */
@Singleton
class DataManagementRepository
    @Inject
    constructor(
        val categoryDao: CategoryDao,
        val transactionDao: TransactionDao,
        val budgetDao: BudgetDao,
        val investmentDao: InvestmentDao,
        val loanDao: LoanDao,
        val emergencyFundDao: EmergencyFundDao,
        private val database: MoneyPilotDatabase,
    ) {
        /**
         * Restores main database tables from a backup.
         */
        suspend fun restoreBackup(
            categories: List<Category>,
            transactions: List<Transaction>,
            budgets: List<Budget>,
            investments: List<Investment>,
            loans: List<Loan> = emptyList(),
            emergencyFund: EmergencyFund? = null,
        ) {
            database.withTransaction {
                database.clearAllTables()
                categoryDao.insertCategories(categories)
                transactions.forEach { transactionDao.insertTransaction(it) }
                budgets.forEach { budgetDao.insertBudget(it) }
                investments.forEach { investmentDao.insertInvestment(it) }
                loans.forEach { loanDao.insertLoan(it) }
                emergencyFund?.let { emergencyFundDao.insertEmergencyFund(it) }
            }
        }

        /**
         * Clears all local data.
         */
        suspend fun clearAllData() {
            database.withTransaction {
                database.clearAllTables()
            }
        }
    }
