package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepository
import javax.inject.Inject

class GetCoSplitUserProfileUseCase @Inject constructor(
    private val repository: CoSplitRepository
) {
    suspend operator fun invoke(email: String): String? {
        return repository.getUserProfile(email)
    }
}
