package prasad.vennam.moneypilot.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import prasad.vennam.moneypilot.data.dao.BookmarkedArticleDao
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
    entities = [Category::class, Transaction::class, Budget::class, Investment::class, ExchangeRate::class, Notification::class, Loan::class, LoanPayment::class, EmergencyFund::class, PendingTransaction::class, BookmarkedArticle::class],
    version = 10,
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

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN currencyCode TEXT NOT NULL DEFAULT 'INR'")
                    db.execSQL("ALTER TABLE budgets ADD COLUMN currencyCode TEXT NOT NULL DEFAULT 'INR'")
                    db.execSQL("ALTER TABLE investments ADD COLUMN currencyCode TEXT NOT NULL DEFAULT 'INR'")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `exchange_rates` (`currencyCode` TEXT NOT NULL, `rateAgainstUSD` REAL NOT NULL, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`currencyCode`))",
                    )
                }
            }

        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `message` TEXT NOT NULL, `category` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `isRead` INTEGER NOT NULL DEFAULT 0)",
                    )
                }
            }

        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE notifications ADD COLUMN url TEXT")
                }
            }

        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `loans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `totalAmount` INTEGER NOT NULL, `outstandingAmount` INTEGER NOT NULL, `emiAmount` INTEGER NOT NULL, `nextEmiDate` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL DEFAULT 'INR')",
                    )
                }
            }

        val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE loans ADD COLUMN lenderName TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE loans ADD COLUMN interestRate REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE loans ADD COLUMN tenureMonths INTEGER NOT NULL DEFAULT 12")
                    db.execSQL("ALTER TABLE loans ADD COLUMN dueDayOfMonth INTEGER NOT NULL DEFAULT 1")
                    db.execSQL("ALTER TABLE loans ADD COLUMN isNotificationEnabled INTEGER NOT NULL DEFAULT 1")
                }
            }

        val MIGRATION_6_7 =
            object : Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `emergency_fund` (`id` INTEGER NOT NULL, `monthlyExpenses` REAL NOT NULL, `targetMonths` INTEGER NOT NULL, `currentSaved` REAL NOT NULL, PRIMARY KEY(`id`))",
                    )
                }
            }

        val MIGRATION_7_8 =
            object : Migration(7, 8) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `pending_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `merchant` TEXT NOT NULL, `bankAccount` TEXT NOT NULL, `rawMessage` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)",
                    )
                }
            }

        val MIGRATION_8_9 =
            object : Migration(8, 9) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `bookmarked_articles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `url` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL)",
                    )
                }
            }

        val MIGRATION_9_10 =
            object : Migration(9, 10) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE loans ADD COLUMN startDate INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `loan_payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `loanId` INTEGER NOT NULL, `amount` INTEGER NOT NULL, `date` INTEGER NOT NULL, `isExtraPayment` INTEGER NOT NULL DEFAULT 0, `note` TEXT NOT NULL DEFAULT '', FOREIGN KEY(`loanId`) REFERENCES `loans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_loan_payments_loanId` ON `loan_payments` (`loanId`)")
                }
            }
    }
}
