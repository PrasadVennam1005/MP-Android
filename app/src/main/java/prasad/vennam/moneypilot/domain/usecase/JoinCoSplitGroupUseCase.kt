package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepository
import javax.inject.Inject

class JoinCoSplitGroupUseCase @Inject constructor(
    private val repository: CoSplitRepository
) {
    suspend operator fun invoke(groupId: String, userEmail: String): Result<Unit> {
        return repository.joinGroup(groupId, userEmail)
    }
}
