package prasad.vennam.moneypilot.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
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
import prasad.vennam.moneypilot.data.dao.SavingGoalDao
import prasad.vennam.moneypilot.data.dao.SubscriptionDao
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
import prasad.vennam.moneypilot.data.entity.SavingGoal
import prasad.vennam.moneypilot.data.entity.Subscription
import prasad.vennam.moneypilot.data.entity.Transaction

@Database(
    entities = [
        Category::class,
        Transaction::class,
        Budget::class,
        Investment::class,
        ExchangeRate::class,
        Notification::class,
        Loan::class,
        LoanPayment::class,
        EmergencyFund::class,
        PendingTransaction::class,
        BookmarkedArticle::class,
        BookmarkedFinanceArticle::class,
        Subscription::class,
        SavingGoal::class,
    ],
    version = 11,
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

    abstract fun subscriptionDao(): SubscriptionDao

    abstract fun savingGoalDao(): SavingGoalDao

    companion object {
        val MIGRATION_1_2 =
            object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `bookmarked_finance_articles` (`articleId` TEXT NOT NULL, `bookmarkedAt` INTEGER NOT NULL, PRIMARY KEY(`articleId`))")
                }
            }

        val MIGRATION_2_3 =
            object : androidx.room.migration.Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `transactions` ADD COLUMN `lastUpdated` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE `categories` ADD COLUMN `lastUpdated` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE `budgets` ADD COLUMN `lastUpdated` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE `investments` ADD COLUMN `lastUpdated` INTEGER NOT NULL DEFAULT 0")
                }
            }

        val MIGRATION_3_4 =
            object : androidx.room.migration.Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                    CREATE TABLE IF NOT EXISTS `subscriptions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `amount` INTEGER NOT NULL, 
                        `billingCycle` TEXT NOT NULL, 
                        `nextPaymentDate` INTEGER NOT NULL, 
                        `paymentMode` TEXT NOT NULL DEFAULT 'UPI', 
                        `categoryId` INTEGER, 
                        `isNotificationEnabled` INTEGER NOT NULL DEFAULT 1, 
                        `lastUpdated` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """,
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_categoryId` ON `subscriptions` (`categoryId`)")

                    db.execSQL(
                        """
                    CREATE TABLE IF NOT EXISTS `saving_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `targetAmount` INTEGER NOT NULL, 
                        `currentSavedAmount` INTEGER NOT NULL, 
                        `deadline` INTEGER NOT NULL, 
                        `colorHex` TEXT NOT NULL DEFAULT '#3F51B5', 
                        `iconName` TEXT NOT NULL DEFAULT 'Savings', 
                        `isCompleted` INTEGER NOT NULL DEFAULT 0, 
                        `lastUpdated` INTEGER NOT NULL DEFAULT 0
                    )
                """,
                    )
                }
            }

        val MIGRATION_4_5 =
            object : androidx.room.migration.Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `loan_payments` ADD COLUMN `paymentMode` TEXT NOT NULL DEFAULT 'Cash'")
                }
            }

        val MIGRATION_5_6 =
            object : androidx.room.migration.Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Safe column addition for Transactions
                    try {
                        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `loanPaymentId` INTEGER DEFAULT NULL")
                    } catch (ignored: Exception) {}
                    
                    try {
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_loanPaymentId` ON `transactions` (`loanPaymentId`)")
                    } catch (ignored: Exception) {}

                    // IMPORTANT: This was missing and caused the integrity error
                    try {
                        db.execSQL("ALTER TABLE `loans` ADD COLUMN `lastUpdated` INTEGER NOT NULL DEFAULT 0")
                    } catch (ignored: Exception) {}
                }
            }

        val MIGRATION_6_7 =
            object : androidx.room.migration.Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Placeholder for future schema refinement
                }
            }

        val MIGRATION_7_8 =
            object : androidx.room.migration.Migration(7, 8) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // No-op: Placeholder for future schema refinement
                }
            }

        val MIGRATION_8_9 =
            object : androidx.room.migration.Migration(8, 9) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // No-op: Placeholder for future schema refinement
                }
            }

        val MIGRATION_9_10 =
            object : androidx.room.migration.Migration(9, 10) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // No-op: Placeholder for future schema refinement
                }
            }

        val MIGRATION_10_11 =
            object : androidx.room.migration.Migration(10, 11) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Final safety check for any columns that might have been missed in messy dev cycles
                    try {
                        db.execSQL("ALTER TABLE `loans` ADD COLUMN `startDate` INTEGER NOT NULL DEFAULT 0")
                    } catch (ignored: Exception) {}
                    try {
                        db.execSQL("ALTER TABLE `loans` ADD COLUMN `lastUpdated` INTEGER NOT NULL DEFAULT 0")
                    } catch (ignored: Exception) {}
                }
            }
    }
}
