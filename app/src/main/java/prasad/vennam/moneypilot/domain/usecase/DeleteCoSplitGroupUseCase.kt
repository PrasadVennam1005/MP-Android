package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepository
import javax.inject.Inject

class DeleteCoSplitGroupUseCase @Inject constructor(
    private val repository: CoSplitRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> {
        return repository.deleteGroup(groupId)
    }
}
