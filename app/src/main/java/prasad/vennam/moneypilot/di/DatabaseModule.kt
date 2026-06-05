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
import prasad.vennam.moneypilot.data.MoneyPilotDatabase
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.dao.BudgetDao
import prasad.vennam.moneypilot.data.dao.CategoryDao
import prasad.vennam.moneypilot.data.dao.InvestmentDao
import prasad.vennam.moneypilot.data.dao.ExchangeRateDao
import prasad.vennam.moneypilot.data.dao.TransactionDao
import prasad.vennam.moneypilot.data.dao.NotificationDao
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        categoryDaoProvider: Provider<CategoryDao>
    ): MoneyPilotDatabase {
        return Room.databaseBuilder(
            context,
            MoneyPilotDatabase::class.java,
            "money_pilot_database"
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                    scope.launch {
                        val categoryDao = categoryDaoProvider.get()
                        // Only seed if empty
                        if (categoryDao.getAllCategories().first().isEmpty()) {
                            categoryDao.insertCategories(Category.DEFAULT_CATEGORIES)
                        }
                    }
                }
            })
            .addMigrations(
                MoneyPilotDatabase.MIGRATION_1_2,
                MoneyPilotDatabase.MIGRATION_2_3,
                MoneyPilotDatabase.MIGRATION_3_4
            )
            .build()
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
    fun provideInvestmentDao(database: MoneyPilotDatabase): InvestmentDao = database.investmentDao()

    @Provides
    @Singleton
    fun provideExchangeRateDao(database: MoneyPilotDatabase): ExchangeRateDao = database.exchangeRateDao()

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }

    @Provides
    @Singleton
    fun provideRepository(
        categoryDao: CategoryDao,
        transactionDao: TransactionDao,
        budgetDao: BudgetDao,
        investmentDao: InvestmentDao,
        database: MoneyPilotDatabase
    ): MoneyPilotRepository {
        return MoneyPilotRepository(categoryDao, transactionDao, budgetDao, investmentDao, database)
    }
}
