package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitGroup
import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepository
import javax.inject.Inject

class GetCoSplitGroupUseCase @Inject constructor(
    private val repository: CoSplitRepository
) {
    operator fun invoke(groupId: String): Flow<CoSplitGroup?> {
        return repository.getGroup(groupId)
    }
}
