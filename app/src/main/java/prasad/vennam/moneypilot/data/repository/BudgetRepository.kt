package prasad.vennam.moneypilot.data.repository

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.dao.BudgetDao
import prasad.vennam.moneypilot.data.entity.Budget
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository
    @Inject
    constructor(
        private val budgetDao: BudgetDao,
    ) {
        val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()

        suspend fun insertBudget(budget: Budget) = budgetDao.insertBudget(budget)

        suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)

        suspend fun deleteBudget(budget: Budget) = budgetDao.deleteBudget(budget)

        suspend fun getBudget(
            categoryId: Long,
            period: String,
        ): Budget? = budgetDao.getBudget(categoryId, period)
    }
