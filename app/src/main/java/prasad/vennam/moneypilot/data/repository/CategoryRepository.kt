package prasad.vennam.moneypilot.data.repository

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.dao.CategoryDao
import prasad.vennam.moneypilot.data.entity.Category
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository
    @Inject
    constructor(
        private val categoryDao: CategoryDao,
    ) {
        val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

        suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)

        suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

        suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)
    }
