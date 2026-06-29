package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.repository.TransactionRepository
import javax.inject.Inject

class SaveCategoryUseCase
    @Inject
    constructor(
        private val repository: TransactionRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(category: Category) {
            userPreferences.setSynced(false)
            repository.insertCategory(category)
        }
    }
