package prasad.vennam.moneypilot.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.MoneyPilotDatabase
import prasad.vennam.moneypilot.data.dao.BudgetDao
import prasad.vennam.moneypilot.data.dao.CategoryDao
import prasad.vennam.moneypilot.data.entity.Category
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository
    @Inject
    constructor(
        private val categoryDao: CategoryDao,
        private val budgetDao: BudgetDao,
        private val database: MoneyPilotDatabase,
    ) {
        val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

        suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)

        suspend fun deleteCategory(category: Category) {
            database.withTransaction {
                // Delete linked budgets first to prevent "ghost" budgets
                budgetDao.deleteBudgetsByCategoryId(category.id)
                categoryDao.deleteCategory(category)
            }
        }

        suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)
    }
