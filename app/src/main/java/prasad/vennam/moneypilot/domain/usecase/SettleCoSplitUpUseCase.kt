package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepository
import javax.inject.Inject

class SettleCoSplitUpUseCase @Inject constructor(
    private val repository: CoSplitRepository
) {
    suspend operator fun invoke(
        groupId: String,
        payerEmail: String,
        receiverEmail: String,
        amount: Double,
        creatorId: String
    ): Result<Unit> {
        return repository.settleUp(groupId, payerEmail, receiverEmail, amount, creatorId)
    }
}
