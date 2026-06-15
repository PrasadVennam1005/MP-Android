package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.entity.BookmarkedArticle
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class AddBookmarkUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
    ) {
        suspend operator fun invoke(bookmark: BookmarkedArticle) {
            repository.insertBookmark(bookmark)
        }

        suspend operator fun invoke(
            title: String,
            url: String,
            currencyCode: String,
        ) {
            repository.insertBookmark(
                BookmarkedArticle(
                    title = title,
                    url = url,
                    timestamp = System.currentTimeMillis(),
                    currencyCode = currencyCode,
                ),
            )
        }
    }
