package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepository
import javax.inject.Inject

class AddCoSplitExpenseUseCase @Inject constructor(
    private val repository: CoSplitRepository
) {
    suspend operator fun invoke(
        groupId: String,
        description: String,
        amount: Double,
        paidBy: String,
        splitType: String,
        splitDetails: Map<String, Double>,
        creatorId: String
    ): Result<Unit> {
        return repository.addExpense(groupId, description, amount, paidBy, splitType, splitDetails, creatorId)
    }
}
