package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase
    @Inject
    constructor(
        private val repository: CategoryRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(category: Category) {
            userPreferences.setSynced(false)
            repository.deleteCategory(category)
        }
    }
