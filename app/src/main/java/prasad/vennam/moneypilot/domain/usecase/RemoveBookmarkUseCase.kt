package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.repository.ArticleRepository
import javax.inject.Inject

class RemoveBookmarkUseCase
    @Inject
    constructor(
        private val repository: ArticleRepository,
    ) {
        suspend operator fun invoke(url: String) {
            repository.deleteBookmarkByUrl(url)
        }
    }
