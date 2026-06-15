package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class DeleteCategoryUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(category: Category) {
            userPreferences.setSynced(false)
            repository.deleteCategory(category)
        }
    }
