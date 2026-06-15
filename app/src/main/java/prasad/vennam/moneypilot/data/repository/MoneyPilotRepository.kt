package prasad.vennam.moneypilot.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.MoneyPilotDatabase
import prasad.vennam.moneypilot.data.dao.BookmarkedArticleDao
import prasad.vennam.moneypilot.data.dao.BudgetDao
import prasad.vennam.moneypilot.data.dao.CategoryDao
import prasad.vennam.moneypilot.data.dao.EmergencyFundDao
import prasad.vennam.moneypilot.data.dao.InvestmentDao
import prasad.vennam.moneypilot.data.dao.LoanDao
import prasad.vennam.moneypilot.data.dao.LoanPaymentDao
import prasad.vennam.moneypilot.data.dao.NotificationDao
import prasad.vennam.moneypilot.data.dao.PendingTransactionDao
import prasad.vennam.moneypilot.data.dao.TransactionDao
import prasad.vennam.moneypilot.data.entity.BookmarkedArticle
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.EmergencyFund
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.data.entity.LoanPayment
import prasad.vennam.moneypilot.data.entity.Notification
import prasad.vennam.moneypilot.data.entity.PendingTransaction
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType

class MoneyPilotRepository(
    val categoryDao: CategoryDao,
    val transactionDao: TransactionDao,
    val budgetDao: BudgetDao,
    val investmentDao: InvestmentDao,
    val loanDao: LoanDao,
    val loanPaymentDao: LoanPaymentDao,
    val emergencyFundDao: EmergencyFundDao,
    val pendingTransactionDao: PendingTransactionDao,
    val bookmarkedArticleDao: BookmarkedArticleDao,
    val notificationDao: NotificationDao,
    private val database: MoneyPilotDatabase,
) {
    // Bookmarked Articles
    val allBookmarks: Flow<List<BookmarkedArticle>> = bookmarkedArticleDao.getAllBookmarks()

    suspend fun insertBookmark(bookmark: BookmarkedArticle) = bookmarkedArticleDao.insertBookmark(bookmark)

    suspend fun deleteBookmark(bookmark: BookmarkedArticle) = bookmarkedArticleDao.deleteBookmark(bookmark)

    suspend fun deleteBookmarkByUrl(url: String) = bookmarkedArticleDao.deleteBookmarkByUrl(url)

    suspend fun getBookmarkByUrl(url: String): BookmarkedArticle? = bookmarkedArticleDao.getBookmarkByUrl(url)

    // Notifications
    val allNotifications: Flow<List<Notification>> = notificationDao.getAllNotifications()

    suspend fun insertNotification(notification: Notification) = notificationDao.insertNotification(notification)

    suspend fun insertNotifications(notifications: List<Notification>) = notificationDao.insertNotifications(notifications)

    suspend fun deleteNotification(id: Long) = notificationDao.deleteNotification(id)

    suspend fun clearAllNotifications() = notificationDao.clearAllNotifications()

    // Emergency Fund
    val emergencyFund: Flow<EmergencyFund?> = emergencyFundDao.getEmergencyFund()

    suspend fun getEmergencyFundSync(): EmergencyFund? = emergencyFundDao.getEmergencyFundSync()

    suspend fun insertEmergencyFund(emergencyFund: EmergencyFund) = emergencyFundDao.insertEmergencyFund(emergencyFund)

    suspend fun deleteEmergencyFund() = emergencyFundDao.deleteEmergencyFund()

    // Pending Transactions
    val allPendingTransactions: Flow<List<PendingTransaction>> = pendingTransactionDao.getAllPendingTransactions()

    suspend fun insertPendingTransaction(pending: PendingTransaction) = pendingTransactionDao.insertPendingTransaction(pending)

    suspend fun deletePendingTransaction(pending: PendingTransaction) = pendingTransactionDao.deletePendingTransaction(pending)

    suspend fun clearAllPendingTransactions() = pendingTransactionDao.clearAllPendingTransactions()

    // Categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)

    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)

    // Transactions
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insertTransaction(transaction: Transaction) = transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: Transaction) = transactionDao.deleteTransaction(transaction)

    suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getTransactionById(id)

    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> = transactionDao.getTransactionsByCategory(categoryId)

    // Budgets
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()

    suspend fun insertBudget(budget: Budget) = budgetDao.insertBudget(budget)

    suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)

    suspend fun deleteBudget(budget: Budget) = budgetDao.deleteBudget(budget)

    suspend fun getBudget(
        categoryId: Long,
        period: String,
    ): Budget? = budgetDao.getBudget(categoryId, period)

    // Investments
    val allInvestments: Flow<List<Investment>> = investmentDao.getAllInvestments()

    suspend fun insertInvestment(investment: Investment) = investmentDao.insertInvestment(investment)

    suspend fun updateInvestment(investment: Investment) = investmentDao.updateInvestment(investment)

    suspend fun deleteInvestment(investment: Investment) = investmentDao.deleteInvestment(investment)

    // Loans
    val allLoans: Flow<List<Loan>> = loanDao.getAllLoans()

    suspend fun insertLoan(loan: Loan) = loanDao.insertLoan(loan)

    suspend fun updateLoan(loan: Loan) = loanDao.updateLoan(loan)

    suspend fun deleteLoan(loan: Loan) = loanDao.deleteLoan(loan)

    suspend fun getLoanById(id: Long) = loanDao.getLoanById(id)

    // Loan Payments
    fun getPaymentsForLoan(loanId: Long) = loanPaymentDao.getPaymentsForLoan(loanId)

    suspend fun insertLoanPayment(payment: LoanPayment) {
        database.withTransaction {
            loanPaymentDao.insertPayment(payment)
            // Update outstanding amount
            val loan = loanDao.getLoanById(payment.loanId)
            loan?.let {
                val newOutstanding = (it.outstandingAmount - payment.amount).coerceAtLeast(0)

                // Increment next EMI date by 1 month
                val cal =
                    java.util.Calendar.getInstance().apply {
                        timeInMillis = it.nextEmiDate
                    }
                cal.add(java.util.Calendar.MONTH, 1)
                val newNextEmiDate = cal.timeInMillis

                // Record matching expense transaction
                val categories = categoryDao.getAllCategoriesSync()
                var categoryId =
                    categories
                        .find { cat ->
                            cat.name.equals("Bills", ignoreCase = true) ||
                                cat.name.equals("Bills/EMI", ignoreCase = true) ||
                                cat.name.equals("Loan", ignoreCase = true)
                        }?.id
                if (categoryId == null && categories.isNotEmpty()) {
                    categoryId = categories.firstOrNull { cat -> cat.isExpense }?.id
                }

                val transaction =
                    Transaction(
                        amount = payment.amount,
                        timestamp = payment.date,
                        categoryId = categoryId,
                        note = if (payment.note.isNotBlank()) payment.note else "EMI Payment: ${it.name}",
                        type = TransactionType.EXPENSE,
                        currencyCode = it.currencyCode,
                    )
                transactionDao.insertTransaction(transaction)

                loanDao.updateLoan(
                    it.copy(
                        outstandingAmount = newOutstanding,
                        nextEmiDate = newNextEmiDate,
                    ),
                )
            }
        }
    }

    suspend fun deleteLoanPayment(payment: LoanPayment) {
        database.withTransaction {
            loanPaymentDao.deletePayment(payment)
            // Update outstanding amount
            val loan = loanDao.getLoanById(payment.loanId)
            loan?.let {
                val newOutstanding = it.outstandingAmount + payment.amount
                loanDao.updateLoan(it.copy(outstandingAmount = newOutstanding))
            }
        }
    }

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

    suspend fun clearAllData() {
        database.withTransaction {
            database.clearAllTables()
        }
    }
}
