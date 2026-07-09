package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitExpense
import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepository
import javax.inject.Inject

class GetCoSplitExpensesUseCase @Inject constructor(
    private val repository: CoSplitRepository
) {
    operator fun invoke(groupId: String): Flow<List<CoSplitExpense>> {
        return repository.getExpenses(groupId)
    }
}
