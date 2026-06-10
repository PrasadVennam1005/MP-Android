package prasad.vennam.moneypilot.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.MoneyPilotDatabase
import prasad.vennam.moneypilot.data.dao.BudgetDao
import prasad.vennam.moneypilot.data.dao.CategoryDao
import prasad.vennam.moneypilot.data.dao.InvestmentDao
import prasad.vennam.moneypilot.data.dao.LoanDao
import prasad.vennam.moneypilot.data.dao.TransactionDao
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.data.entity.Transaction

class MoneyPilotRepository(
    val categoryDao: CategoryDao,
    val transactionDao: TransactionDao,
    val budgetDao: BudgetDao,
    val investmentDao: InvestmentDao,
    val loanDao: LoanDao,
    private val database: MoneyPilotDatabase,
) {
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

    suspend fun restoreBackup(
        categories: List<Category>,
        transactions: List<Transaction>,
        budgets: List<Budget>,
        investments: List<Investment>,
        loans: List<Loan> = emptyList(),
    ) {
        database.withTransaction {
            database.clearAllTables()
            categoryDao.insertCategories(categories)
            transactions.forEach { transactionDao.insertTransaction(it) }
            budgets.forEach { budgetDao.insertBudget(it) }
            investments.forEach { investmentDao.insertInvestment(it) }
            loans.forEach { loanDao.insertLoan(it) }
        }
    }

    suspend fun clearAllData() {
        database.withTransaction {
            database.clearAllTables()
        }
    }
}

