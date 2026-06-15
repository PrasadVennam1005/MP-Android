package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.BookmarkedArticle
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class GetBookmarksUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
    ) {
        operator fun invoke(): Flow<List<BookmarkedArticle>> = repository.allBookmarks
    }
