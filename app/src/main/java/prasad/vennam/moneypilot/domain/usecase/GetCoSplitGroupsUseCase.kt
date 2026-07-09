package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitGroup
import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepository
import javax.inject.Inject

class GetCoSplitGroupsUseCase @Inject constructor(
    private val repository: CoSplitRepository
) {
    operator fun invoke(userEmail: String): Flow<List<CoSplitGroup>> {
        return repository.getGroups(userEmail)
    }
}
