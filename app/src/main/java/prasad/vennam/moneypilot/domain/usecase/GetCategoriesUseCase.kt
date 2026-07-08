package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.repository.CategoryRepository
import javax.inject.Inject

class GetCategoriesUseCase
    @Inject
    constructor(
        private val repository: CategoryRepository,
    ) {
        operator fun invoke(): Flow<List<Category>> = repository.allCategories
    }
