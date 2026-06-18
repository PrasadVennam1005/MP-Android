package prasad.vennam.moneypilot.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import prasad.vennam.moneypilot.data.dao.BookmarkedArticleDao
import prasad.vennam.moneypilot.data.dao.BookmarkedFinanceArticleDao
import prasad.vennam.moneypilot.data.dao.BudgetDao
import prasad.vennam.moneypilot.data.dao.CategoryDao
import prasad.vennam.moneypilot.data.dao.EmergencyFundDao
import prasad.vennam.moneypilot.data.dao.ExchangeRateDao
import prasad.vennam.moneypilot.data.dao.InvestmentDao
import prasad.vennam.moneypilot.data.dao.LoanDao
import prasad.vennam.moneypilot.data.dao.LoanPaymentDao
import prasad.vennam.moneypilot.data.dao.NotificationDao
import prasad.vennam.moneypilot.data.dao.PendingTransactionDao
import prasad.vennam.moneypilot.data.dao.TransactionDao
import prasad.vennam.moneypilot.data.entity.BookmarkedArticle
import prasad.vennam.moneypilot.data.entity.BookmarkedFinanceArticle
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.EmergencyFund
import prasad.vennam.moneypilot.data.entity.ExchangeRate
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.data.entity.LoanPayment
import prasad.vennam.moneypilot.data.entity.Notification
import prasad.vennam.moneypilot.data.entity.PendingTransaction
import prasad.vennam.moneypilot.data.entity.Transaction

@Database(
    entities = [Category::class, Transaction::class, Budget::class, Investment::class, ExchangeRate::class, Notification::class, Loan::class, LoanPayment::class, EmergencyFund::class, PendingTransaction::class, BookmarkedArticle::class, BookmarkedFinanceArticle::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MoneyPilotDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao

    abstract fun transactionDao(): TransactionDao

    abstract fun budgetDao(): BudgetDao

    abstract fun investmentDao(): InvestmentDao

    abstract fun exchangeRateDao(): ExchangeRateDao

    abstract fun notificationDao(): NotificationDao

    abstract fun loanDao(): LoanDao

    abstract fun loanPaymentDao(): LoanPaymentDao

    abstract fun emergencyFundDao(): EmergencyFundDao

    abstract fun pendingTransactionDao(): PendingTransactionDao

    abstract fun bookmarkedArticleDao(): BookmarkedArticleDao

    abstract fun bookmarkedFinanceArticleDao(): BookmarkedFinanceArticleDao
}
