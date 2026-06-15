package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import org.mockito.Mockito.`when` as whenever

class UseCaseTest {
    private val repository = mock(MoneyPilotRepository::class.java)
    private val userPreferences = mock(UserPreferences::class.java)

    private val getTransactionByIdUseCase = GetTransactionByIdUseCase(repository)
    private val saveTransactionUseCase = SaveTransactionUseCase(repository, userPreferences)
    private val deleteTransactionUseCase = DeleteTransactionUseCase(repository, userPreferences)
    private val saveBudgetUseCase = SaveBudgetUseCase(repository, userPreferences)
    private val deleteBudgetUseCase = DeleteBudgetUseCase(repository, userPreferences)
    private val addLoanUseCase = AddLoanUseCase(repository, userPreferences)
    private val updateLoanUseCase = UpdateLoanUseCase(repository, userPreferences)
    private val deleteLoanUseCase = DeleteLoanUseCase(repository, userPreferences)
    private val saveInvestmentUseCase = SaveInvestmentUseCase(repository, userPreferences)
    private val deleteInvestmentUseCase = DeleteInvestmentUseCase(repository, userPreferences)

    @Test
    fun getTransactionByIdUseCase_returnsTransaction_whenFound() =
        runTest {
            val transaction =
                Transaction(
                    id = 1L,
                    amount = 1000,
                    note = "Test note",
                    timestamp = 123456789L,
                    type = TransactionType.EXPENSE,
                    categoryId = 2L,
                    subCategory = "Test sub",
                    paymentMode = "Cash",
                    currencyCode = "INR",
                )
            whenever(repository.getTransactionById(1L)).thenReturn(transaction)

            val result = getTransactionByIdUseCase(1L)

            assertEquals(transaction, result)
            verify(repository).getTransactionById(1L)
        }

    @Test
    fun getTransactionByIdUseCase_returnsNull_whenNotFound() =
        runTest {
            whenever(repository.getTransactionById(2L)).thenReturn(null)

            val result = getTransactionByIdUseCase(2L)

            assertNull(result)
            verify(repository).getTransactionById(2L)
        }

    @Test
    fun saveTransactionUseCase_insertsTransaction_whenIdIsZero() =
        runTest {
            val transaction =
                Transaction(
                    id = 0L,
                    amount = 1000,
                    note = "Test note",
                    timestamp = 123456789L,
                    type = TransactionType.EXPENSE,
                    categoryId = 2L,
                    subCategory = "Test sub",
                    paymentMode = "Cash",
                    currencyCode = "INR",
                )

            saveTransactionUseCase(transaction)

            verify(userPreferences).setSynced(false)
            verify(repository).insertTransaction(transaction)
        }

    @Test
    fun saveTransactionUseCase_updatesTransaction_whenIdIsNotZero() =
        runTest {
            val transaction =
                Transaction(
                    id = 5L,
                    amount = 2000,
                    note = "Updated note",
                    timestamp = 123456789L,
                    type = TransactionType.INCOME,
                    categoryId = 3L,
                    subCategory = "Updated sub",
                    paymentMode = "Card",
                    currencyCode = "INR",
                )

            saveTransactionUseCase(transaction)

            verify(userPreferences).setSynced(false)
            verify(repository).updateTransaction(transaction)
        }

    @Test
    fun deleteTransactionUseCase_deletesTransaction() =
        runTest {
            val transaction =
                Transaction(
                    id = 5L,
                    amount = 2000,
                    note = "Delete note",
                    timestamp = 123456789L,
                    type = TransactionType.INCOME,
                    categoryId = 3L,
                    subCategory = "Delete sub",
                    paymentMode = "Card",
                    currencyCode = "INR",
                )

            deleteTransactionUseCase(transaction)

            verify(userPreferences).setSynced(false)
            verify(repository).deleteTransaction(transaction)
        }

    @Test
    fun saveBudgetUseCase_insertsBudget_whenIdIsZero() =
        runTest {
            val budget =
                Budget(
                    id = 0L,
                    categoryId = 2L,
                    amount = 500000,
                    period = "Monthly",
                    currencyCode = "INR",
                )

            saveBudgetUseCase(budget)

            verify(userPreferences).setSynced(false)
            verify(repository).insertBudget(budget)
        }

    @Test
    fun saveBudgetUseCase_updatesBudget_whenIdIsNotZero() =
        runTest {
            val budget =
                Budget(
                    id = 4L,
                    categoryId = 3L,
                    amount = 600000,
                    period = "Monthly",
                    currencyCode = "INR",
                )

            saveBudgetUseCase(budget)

            verify(userPreferences).setSynced(false)
            verify(repository).updateBudget(budget)
        }

    @Test
    fun deleteBudgetUseCase_deletesBudget() =
        runTest {
            val budget =
                Budget(
                    id = 4L,
                    categoryId = 3L,
                    amount = 600000,
                    period = "Monthly",
                    currencyCode = "INR",
                )

            deleteBudgetUseCase(budget)

            verify(userPreferences).setSynced(false)
            verify(repository).deleteBudget(budget)
        }

    @Test
    fun addLoanUseCase_insertsLoan() =
        runTest {
            val loan =
                Loan(
                    id = 0L,
                    name = "Test Loan",
                    totalAmount = 100000,
                    outstandingAmount = 80000,
                    emiAmount = 5000,
                    nextEmiDate = 123456789L,
                    currencyCode = "INR",
                )

            addLoanUseCase(loan)

            verify(userPreferences).setSynced(false)
            verify(repository).insertLoan(loan)
        }

    @Test
    fun updateLoanUseCase_updatesLoan() =
        runTest {
            val loan =
                Loan(
                    id = 1L,
                    name = "Updated Loan",
                    totalAmount = 100000,
                    outstandingAmount = 75000,
                    emiAmount = 5000,
                    nextEmiDate = 123456789L,
                    currencyCode = "INR",
                )

            updateLoanUseCase(loan)

            verify(userPreferences).setSynced(false)
            verify(repository).updateLoan(loan)
        }

    @Test
    fun deleteLoanUseCase_deletesLoan() =
        runTest {
            val loan =
                Loan(
                    id = 1L,
                    name = "Delete Loan",
                    totalAmount = 100000,
                    outstandingAmount = 75000,
                    emiAmount = 5000,
                    nextEmiDate = 123456789L,
                    currencyCode = "INR",
                )

            deleteLoanUseCase(loan)

            verify(userPreferences).setSynced(false)
            verify(repository).deleteLoan(loan)
        }

    @Test
    fun saveInvestmentUseCase_insertsInvestment_whenIdIsZero() =
        runTest {
            val investment =
                Investment(
                    id = 0L,
                    name = "Test Stock",
                    type = "Stock",
                    investedAmount = 50000,
                    currentValue = 60000,
                    currencyCode = "INR",
                )

            saveInvestmentUseCase(investment)

            verify(userPreferences).setSynced(false)
            verify(repository).insertInvestment(investment)
        }

    @Test
    fun saveInvestmentUseCase_updatesInvestment_whenIdIsNotZero() =
        runTest {
            val investment =
                Investment(
                    id = 2L,
                    name = "Updated Stock",
                    type = "Stock",
                    investedAmount = 50000,
                    currentValue = 65000,
                    currencyCode = "INR",
                )

            saveInvestmentUseCase(investment)

            verify(userPreferences).setSynced(false)
            verify(repository).updateInvestment(investment)
        }

    @Test
    fun deleteInvestmentUseCase_deletesInvestment() =
        runTest {
            val investment =
                Investment(
                    id = 2L,
                    name = "Delete Stock",
                    type = "Stock",
                    investedAmount = 50000,
                    currentValue = 65000,
                    currencyCode = "INR",
                )

            deleteInvestmentUseCase(investment)

            verify(userPreferences).setSynced(false)
            verify(repository).deleteInvestment(investment)
        }

    @Test
    fun saveCategoryUseCase_insertsCategory() =
        runTest {
            val category =
                prasad.vennam.moneypilot.data.entity.Category(
                    id = 0L,
                    name = "Food",
                    iconName = "restaurant",
                    color = 0xFFF44336,
                    isExpense = true,
                )
            val useCase = SaveCategoryUseCase(repository, userPreferences)
            useCase(category)
            verify(userPreferences).setSynced(false)
            verify(repository).insertCategory(category)
        }

    @Test
    fun deleteCategoryUseCase_deletesCategory() =
        runTest {
            val category =
                prasad.vennam.moneypilot.data.entity.Category(
                    id = 1L,
                    name = "Food",
                    iconName = "restaurant",
                    color = 0xFFF44336,
                    isExpense = true,
                )
            val useCase = DeleteCategoryUseCase(repository, userPreferences)
            useCase(category)
            verify(userPreferences).setSynced(false)
            verify(repository).deleteCategory(category)
        }

    @Test
    fun saveEmergencyFundUseCase_savesEmergencyFund() =
        runTest {
            val fund =
                prasad.vennam.moneypilot.data.entity.EmergencyFund(
                    id = 1L,
                    monthlyExpenses = 20000.0,
                    targetMonths = 6,
                    currentSaved = 10000.0,
                )
            val useCase = SaveEmergencyFundUseCase(repository, userPreferences)
            useCase(fund)
            verify(userPreferences).setSynced(false)
            verify(repository).insertEmergencyFund(fund)
        }

    @Test
    fun getBookmarksUseCase_returnsBookmarksFlow() =
        runTest {
            val flow = kotlinx.coroutines.flow.flowOf(emptyList<prasad.vennam.moneypilot.data.entity.BookmarkedArticle>())
            whenever(repository.allBookmarks).thenReturn(flow)
            val useCase = GetBookmarksUseCase(repository)
            val result = useCase()
            assertEquals(flow, result)
            verify(repository).allBookmarks
        }

    @Test
    fun addBookmarkUseCase_savesBookmark() =
        runTest {
            val article =
                prasad.vennam.moneypilot.data.entity.BookmarkedArticle(
                    title = "Test",
                    url = "http",
                    timestamp = 0L,
                    currencyCode = "INR",
                )
            val useCase = AddBookmarkUseCase(repository)
            useCase(article)
            verify(repository).insertBookmark(article)
        }

    @Test
    fun removeBookmarkUseCase_deletesBookmarkByUrl() =
        runTest {
            val useCase = RemoveBookmarkUseCase(repository)
            useCase("http")
            verify(repository).deleteBookmarkByUrl("http")
        }

    @Test
    fun getNotificationsUseCase_returnsNotificationsFlow() =
        runTest {
            val flow = kotlinx.coroutines.flow.flowOf(emptyList<prasad.vennam.moneypilot.data.entity.Notification>())
            whenever(repository.allNotifications).thenReturn(flow)
            val useCase = GetNotificationsUseCase(repository)
            val result = useCase()
            assertEquals(flow, result)
            verify(repository).allNotifications
        }

    @Test
    fun insertNotificationsUseCase_savesNotifications() =
        runTest {
            val notifications =
                listOf(
                    prasad.vennam.moneypilot.data.entity
                        .Notification(title = "Welcome", message = "msg", category = "sys", timestamp = 0L),
                )
            val useCase = InsertNotificationsUseCase(repository)
            useCase(notifications)
            verify(repository).insertNotifications(notifications)
        }

    @Test
    fun deleteNotificationUseCase_deletesNotificationById() =
        runTest {
            val useCase = DeleteNotificationUseCase(repository)
            useCase(42L)
            verify(repository).deleteNotification(42L)
        }

    @Test
    fun clearAllNotificationsUseCase_clearsAllNotifications() =
        runTest {
            val useCase = ClearAllNotificationsUseCase(repository)
            useCase()
            verify(repository).clearAllNotifications()
        }

    @Test
    fun restoreBackupUseCase_restoresBackup() =
        runTest {
            val useCase = RestoreBackupUseCase(repository, userPreferences)
            useCase(emptyList(), emptyList(), emptyList(), emptyList())
            verify(repository).restoreBackup(emptyList(), emptyList(), emptyList(), emptyList())
            verify(userPreferences).setSynced(true)
        }

    @Test
    fun clearAllDataUseCase_clearsAllData() =
        runTest {
            val useCase = ClearAllDataUseCase(repository)
            useCase()
            verify(repository).clearAllData()
        }
}
