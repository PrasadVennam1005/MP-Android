package prasad.vennam.moneypilot.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import prasad.vennam.moneypilot.data.MoneyPilotDatabase
import prasad.vennam.moneypilot.data.UserPreferences
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
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.repository.DataManagementRepository
import prasad.vennam.moneypilot.domain.usecase.BackupSyncManager
import prasad.vennam.moneypilot.domain.usecase.LoanReminderScheduler
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.SecureStorageHelper
import prasad.vennam.moneypilot.worker.BackupSyncManagerImpl
import prasad.vennam.moneypilot.worker.LoanReminderSchedulerImpl
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideSecureStorageHelper(
        @ApplicationContext context: Context,
    ): SecureStorageHelper = SecureStorageHelper(context)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        secureStorageHelper: SecureStorageHelper,
        categoryDaoProvider: Provider<CategoryDao>,
    ): MoneyPilotDatabase {
        try {
            System.loadLibrary("sqlcipher")
        } catch (t: Throwable) {
            android.util.Log.e("DatabaseModule", "Failed to load sqlcipher native library", t)
        }

        val dbName = "money_pilot_database"
        val dbFile = context.getDatabasePath(dbName)
        val passphraseStr = secureStorageHelper.getOrGenerateDatabasePassphraseString()
        val passphraseBytes = passphraseStr.toByteArray(Charsets.UTF_8)
        val factory = SupportOpenHelperFactory(passphraseBytes)

        if (dbFile.exists()) {
            try {
                val db =
                    SQLiteDatabase.openDatabase(
                        dbFile.absolutePath,
                        passphraseStr,
                        null,
                        SQLiteDatabase.OPEN_READONLY,
                        null,
                    )
                db.close()
            } catch (e: Exception) {
                android.util.Log.e("DatabaseModule", "Existing database cannot be decrypted, deleting it to recreate securely", e)
                context.deleteDatabase(dbName)
            }
        }

        return Room
            .databaseBuilder(
                context,
                MoneyPilotDatabase::class.java,
                dbName,
            ).openHelperFactory(factory)
            .addMigrations(
                MoneyPilotDatabase.MIGRATION_1_2,
                MoneyPilotDatabase.MIGRATION_2_3,
                MoneyPilotDatabase.MIGRATION_3_4,
                MoneyPilotDatabase.MIGRATION_4_5,
                MoneyPilotDatabase.MIGRATION_5_6,
            ).fallbackToDestructiveMigration(true)
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                        scope.launch {
                            val categoryDao = categoryDaoProvider.get()
                            val existingCategories = categoryDao.getAllCategories().first()
                            if (existingCategories.isEmpty()) {
                                categoryDao.insertCategories(Category.DEFAULT_CATEGORIES)
                            } else {
                                // Insert missing default categories (e.g. after app update)
                                val existingNames = existingCategories.map { it.name }.toSet()
                                val missingCategories = Category.DEFAULT_CATEGORIES.filter { it.name !in existingNames }
                                if (missingCategories.isNotEmpty()) {
                                    categoryDao.insertCategories(missingCategories)
                                }
                            }
                        }
                    }
                },
            ).build()
    }

    @Provides
    fun provideNotificationDao(database: MoneyPilotDatabase): NotificationDao = database.notificationDao()

    @Provides
    fun provideCategoryDao(database: MoneyPilotDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: MoneyPilotDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideBudgetDao(database: MoneyPilotDatabase): BudgetDao = database.budgetDao()

    @Provides
    @Singleton
    fun provideLoanDao(database: MoneyPilotDatabase): LoanDao = database.loanDao()

    @Provides
    @Singleton
    fun provideLoanPaymentDao(database: MoneyPilotDatabase): LoanPaymentDao = database.loanPaymentDao()

    @Provides
    @Singleton
    fun provideInvestmentDao(database: MoneyPilotDatabase): InvestmentDao = database.investmentDao()

    @Provides
    @Singleton
    fun provideExchangeRateDao(database: MoneyPilotDatabase): ExchangeRateDao = database.exchangeRateDao()

    @Provides
    @Singleton
    fun provideEmergencyFundDao(database: MoneyPilotDatabase): EmergencyFundDao = database.emergencyFundDao()

    @Provides
    @Singleton
    fun providePendingTransactionDao(database: MoneyPilotDatabase): PendingTransactionDao = database.pendingTransactionDao()

    @Provides
    @Singleton
    fun provideBookmarkedArticleDao(database: MoneyPilotDatabase): BookmarkedArticleDao = database.bookmarkedArticleDao()

    @Provides
    @Singleton
    fun provideBookmarkedFinanceArticleDao(database: MoneyPilotDatabase): BookmarkedFinanceArticleDao = database.bookmarkedFinanceArticleDao()

    @Provides
    @Singleton
    fun provideSubscriptionDao(database: MoneyPilotDatabase): SubscriptionDao = database.subscriptionDao()

    @Provides
    @Singleton
    fun provideSavingGoalDao(database: MoneyPilotDatabase): SavingGoalDao = database.savingGoalDao()

    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context,
    ): UserPreferences = UserPreferences(context)

    @Provides
    @Singleton
    fun provideRepository(
        categoryDao: CategoryDao,
        transactionDao: TransactionDao,
        budgetDao: BudgetDao,
        investmentDao: InvestmentDao,
        loanDao: LoanDao,
        emergencyFundDao: EmergencyFundDao,
        subscriptionDao: SubscriptionDao,
        savingGoalDao: SavingGoalDao,
        loanPaymentDao: LoanPaymentDao,
        database: MoneyPilotDatabase,
    ): DataManagementRepository =
        DataManagementRepository(
            categoryDao = categoryDao,
            transactionDao = transactionDao,
            budgetDao = budgetDao,
            investmentDao = investmentDao,
            loanDao = loanDao,
            emergencyFundDao = emergencyFundDao,
            subscriptionDao = subscriptionDao,
            savingGoalDao = savingGoalDao,
            loanPaymentDao = loanPaymentDao,
            database = database,
        )

    @Provides
    @Singleton
    fun provideLoanReminderScheduler(
        @ApplicationContext context: Context,
        loanDao: LoanDao,
        notificationDao: NotificationDao,
    ): LoanReminderScheduler = LoanReminderSchedulerImpl(context, loanDao, notificationDao)

    @Provides
    @Singleton
    fun provideBackupSyncManager(
        repository: DataManagementRepository,
        userPreferences: UserPreferences,
        analyticsHelper: AnalyticsHelper,
    ): BackupSyncManager = BackupSyncManagerImpl(repository, userPreferences, analyticsHelper)
}
