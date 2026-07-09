package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepository
import javax.inject.Inject

class UpdateCoSplitGroupMembersUseCase @Inject constructor(
    private val repository: CoSplitRepository
) {
    suspend operator fun invoke(groupId: String, members: List<String>, memberNames: Map<String, String>): Result<Unit> {
        return repository.updateGroupMembers(groupId, members, memberNames)
    }
}
