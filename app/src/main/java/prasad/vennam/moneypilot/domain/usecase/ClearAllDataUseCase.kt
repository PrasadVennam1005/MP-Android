package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.repository.DataManagementRepository
import javax.inject.Inject

class ClearAllDataUseCase
    @Inject
    constructor(
        private val repository: DataManagementRepository,
    ) {
        suspend operator fun invoke() {
            repository.clearAllData()
        }
    }
