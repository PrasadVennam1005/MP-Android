package prasad.vennam.moneypilot

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import prasad.vennam.moneypilot.data.MoneyPilotDatabase
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.dao.BudgetDao
import prasad.vennam.moneypilot.data.dao.CategoryDao
import prasad.vennam.moneypilot.data.dao.EmergencyFundDao
import prasad.vennam.moneypilot.data.dao.InvestmentDao
import prasad.vennam.moneypilot.data.dao.LoanDao
import prasad.vennam.moneypilot.data.dao.TransactionDao
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.EmergencyFund
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.data.repository.DataManagementRepository
import prasad.vennam.moneypilot.util.GoogleSheetsSyncHelper
import prasad.vennam.moneypilot.util.ValueRange
import prasad.vennam.moneypilot.util.WorkManagerSyncScheduler

class GoogleSheetsSyncNameTest {
    @Test
    fun guarantee_uniqueWorkNamesAreAligned() {
        // Enforce that scheduling name matches UI observer name to guarantee synchronization tracking works
        assertEquals(
            "WorkManager scheduler unique name must match the name observed by the UI flow.",
            GoogleSheetsSyncHelper.SYNC_WORK_NAME,
            WorkManagerSyncScheduler.UNIQUE_WORK_NAME,
        )
    }

    @Test
    fun testMergeCloudDataIntoLocal_insertsNewerCloudCategory() =
        runBlocking {
            val (repository, fakes) = createMockRepository()
            val mockUserPrefs = mock(UserPreferences::class.java)

            // Local has older category
            val localCategory = Category(id = 1L, name = "Food", iconName = "ic_food", color = 0L, isExpense = true, lastUpdated = 1000L)
            fakes.categoryDao.categories.add(localCategory)

            // Cloud has newer category
            val cloudValues =
                listOf(
                    ValueRange(
                        range = "Categories!A2:Z10000",
                        values =
                            listOf(
                                listOf("1", "Food", "ic_food", "0", "true", "2000"), // lastUpdated = 2000L
                            ),
                    ),
                )

            GoogleSheetsSyncHelper.mergeCloudDataIntoLocal(
                fakes.categoryDao,
                fakes.budgetDao,
                fakes.investmentDao,
                fakes.transactionDao,
                fakes.emergencyFundDao,
                mockUserPrefs,
                cloudValues,
            )

            // Verify insertion called with newer category
            assertEquals(1, fakes.categoryDao.insertedCategories.size)
            assertEquals(2000L, fakes.categoryDao.insertedCategories[0].lastUpdated)
            assertEquals("Food", fakes.categoryDao.insertedCategories[0].name)
        }

    @Test
    fun testMergeCloudDataIntoLocal_ignoresOlderCloudCategory() =
        runBlocking {
            val (repository, fakes) = createMockRepository()
            val mockUserPrefs = mock(UserPreferences::class.java)

            // Local has newer category
            val localCategory = Category(id = 1L, name = "Food", iconName = "ic_food", color = 0L, isExpense = true, lastUpdated = 3000L)
            fakes.categoryDao.categories.add(localCategory)

            // Cloud has older category
            val cloudValues =
                listOf(
                    ValueRange(
                        range = "Categories!A2:Z10000",
                        values =
                            listOf(
                                listOf("1", "Food", "ic_food", "0", "true", "2000"), // lastUpdated = 2000L
                            ),
                    ),
                )

            GoogleSheetsSyncHelper.mergeCloudDataIntoLocal(
                fakes.categoryDao,
                fakes.budgetDao,
                fakes.investmentDao,
                fakes.transactionDao,
                fakes.emergencyFundDao,
                mockUserPrefs,
                cloudValues,
            )

            // Verify insertion never called
            assertTrue(fakes.categoryDao.insertedCategories.isEmpty())
        }

    @Test
    fun testMergeCloudDataIntoLocal_insertsNewerCloudTransaction() =
        runBlocking {
            val (repository, fakes) = createMockRepository()
            val mockUserPrefs = mock(UserPreferences::class.java)

            // Local has older transaction
            val localTransaction = Transaction(id = 101L, amount = 1000L, timestamp = 1718971200000L, categoryId = 1L, paymentMode = "Cash", note = "Snack", type = TransactionType.EXPENSE, currencyCode = "INR", subCategory = "Fast Food", lastUpdated = 1000L)
            fakes.transactionDao.transactions.add(localTransaction)

            // Cloud has newer transaction
            val cloudValues =
                listOf(
                    ValueRange(
                        range = "Transactions!A2:Z10000",
                        values =
                            listOf(
                                listOf("101", "2026-06-22 12:00:00", "EXPENSE", "1", "Cash", "10.0", "Snack", "INR", "Fast Food", "2000"), // lastUpdated = 2000L
                            ),
                    ),
                )

            GoogleSheetsSyncHelper.mergeCloudDataIntoLocal(
                fakes.categoryDao,
                fakes.budgetDao,
                fakes.investmentDao,
                fakes.transactionDao,
                fakes.emergencyFundDao,
                mockUserPrefs,
                cloudValues,
            )

            // Verify insertion called with newer transaction (10.0 * 100 = 1000L)
            assertEquals(1, fakes.transactionDao.insertedTransactions.size)
            assertEquals(2000L, fakes.transactionDao.insertedTransactions[0].lastUpdated)
            assertEquals(1000L, fakes.transactionDao.insertedTransactions[0].amount)
        }

    @Test
    fun testMergeCloudDataIntoLocal_ignoresOlderCloudTransaction() =
        runBlocking {
            val (repository, fakes) = createMockRepository()
            val mockUserPrefs = mock(UserPreferences::class.java)

            // Local has newer transaction
            val localTransaction = Transaction(id = 101L, amount = 1000L, timestamp = 1718971200000L, categoryId = 1L, paymentMode = "Cash", note = "Snack", type = TransactionType.EXPENSE, currencyCode = "INR", subCategory = "Fast Food", lastUpdated = 3000L)
            fakes.transactionDao.transactions.add(localTransaction)

            // Cloud has older transaction
            val cloudValues =
                listOf(
                    ValueRange(
                        range = "Transactions!A2:Z10000",
                        values =
                            listOf(
                                listOf("101", "2026-06-22 12:00:00", "EXPENSE", "1", "Cash", "10.0", "Snack", "INR", "Fast Food", "2000"), // lastUpdated = 2000L
                            ),
                    ),
                )

            GoogleSheetsSyncHelper.mergeCloudDataIntoLocal(
                fakes.categoryDao,
                fakes.budgetDao,
                fakes.investmentDao,
                fakes.transactionDao,
                fakes.emergencyFundDao,
                mockUserPrefs,
                cloudValues,
            )

            // Verify insertion never called
            assertTrue(fakes.transactionDao.insertedTransactions.isEmpty())
        }

    private class FakeCategoryDao : CategoryDao {
        val categories = mutableListOf<Category>()
        val insertedCategories = mutableListOf<Category>()

        override fun getAllCategories(): Flow<List<Category>> = emptyFlow()

        override suspend fun getAllCategoriesSync(): List<Category> = categories

        override suspend fun insertCategory(category: Category) {
            insertedCategories.add(category)
        }

        override suspend fun insertCategories(categories: List<Category>) {
            insertedCategories.addAll(categories)
        }

        override suspend fun deleteCategory(category: Category) {
            categories.remove(category)
        }

        override suspend fun getCategoryById(id: Long): Category? = categories.find { it.id == id }
    }

    private class FakeTransactionDao : TransactionDao {
        val transactions = mutableListOf<Transaction>()
        val insertedTransactions = mutableListOf<Transaction>()

        override fun getAllTransactions(): Flow<List<Transaction>> = emptyFlow()

        override suspend fun getAllTransactionsSync(): List<Transaction> = transactions

        override suspend fun insertTransaction(transaction: Transaction) {
            insertedTransactions.add(transaction)
        }

        override suspend fun updateTransaction(transaction: Transaction) {
            insertedTransactions.add(transaction)
        }

        override suspend fun deleteTransaction(transaction: Transaction) {
            transactions.remove(transaction)
        }

        override fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> = emptyFlow()

        override suspend fun getTransactionById(id: Long): Transaction? = transactions.find { it.id == id }
    }

    private class FakeBudgetDao : BudgetDao {
        val budgets = mutableListOf<Budget>()
        val insertedBudgets = mutableListOf<Budget>()

        override fun getAllBudgets(): Flow<List<Budget>> = emptyFlow()

        override suspend fun getAllBudgetsSync(): List<Budget> = budgets

        override suspend fun insertBudget(budget: Budget) {
            insertedBudgets.add(budget)
        }

        override suspend fun updateBudget(budget: Budget) {
            insertedBudgets.add(budget)
        }

        override suspend fun deleteBudget(budget: Budget) {
            budgets.remove(budget)
        }

        override suspend fun getBudget(
            categoryId: Long,
            period: String,
        ): Budget? = budgets.find { it.categoryId == categoryId && it.period == period }
    }

    private class FakeInvestmentDao : InvestmentDao {
        val investments = mutableListOf<Investment>()
        val insertedInvestments = mutableListOf<Investment>()

        override fun getAllInvestments(): Flow<List<Investment>> = emptyFlow()

        override suspend fun getAllInvestmentsSync(): List<Investment> = investments

        override suspend fun insertInvestment(investment: Investment) {
            insertedInvestments.add(investment)
        }

        override suspend fun updateInvestment(investment: Investment) {
            insertedInvestments.add(investment)
        }

        override suspend fun deleteInvestment(investment: Investment) {
            investments.remove(investment)
        }
    }

    private class FakeEmergencyFundDao : EmergencyFundDao {
        var emergencyFund: EmergencyFund? = null
        var insertedEmergencyFund: EmergencyFund? = null

        override fun getEmergencyFund(): Flow<EmergencyFund?> = emptyFlow()

        override suspend fun getEmergencyFundSync(): EmergencyFund? = emergencyFund

        override suspend fun insertEmergencyFund(emergencyFund: EmergencyFund) {
            insertedEmergencyFund = emergencyFund
        }

        override suspend fun deleteEmergencyFund() {
            emergencyFund = null
        }
    }

    private data class FakeDaos(
        val categoryDao: FakeCategoryDao,
        val transactionDao: FakeTransactionDao,
        val budgetDao: FakeBudgetDao,
        val investmentDao: FakeInvestmentDao,
        val emergencyFundDao: FakeEmergencyFundDao,
    )

    private fun createMockRepository(): Pair<DataManagementRepository, FakeDaos> {
        val categoryDao = FakeCategoryDao()
        val transactionDao = FakeTransactionDao()
        val budgetDao = FakeBudgetDao()
        val investmentDao = FakeInvestmentDao()
        val emergencyFundDao = FakeEmergencyFundDao()

        val loanDao = mock(LoanDao::class.java)
        val database = mock(MoneyPilotDatabase::class.java)

        val fakes =
            FakeDaos(
                categoryDao,
                transactionDao,
                budgetDao,
                investmentDao,
                emergencyFundDao,
            )

        val repository =
            DataManagementRepository(
                categoryDao,
                transactionDao,
                budgetDao,
                investmentDao,
                loanDao,
                emergencyFundDao,
                database,
            )

        return Pair(repository, fakes)
    }
}
