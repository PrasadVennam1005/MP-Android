package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

class RemoveBookmarkUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
    ) {
        suspend operator fun invoke(url: String) {
            repository.deleteBookmarkByUrl(url)
        }
    }
