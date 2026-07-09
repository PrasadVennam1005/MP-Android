package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepository
import javax.inject.Inject

class CreateCoSplitGroupUseCase @Inject constructor(
    private val repository: CoSplitRepository
) {
    suspend operator fun invoke(
        name: String,
        members: List<String>,
        memberNames: Map<String, String>,
        creatorId: String,
        creatorEmail: String
    ): Result<String> {
        return repository.createGroup(name, members, memberNames, creatorId, creatorEmail)
    }
}
