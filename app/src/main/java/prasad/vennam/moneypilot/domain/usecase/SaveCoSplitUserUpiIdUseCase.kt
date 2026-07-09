package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepository
import javax.inject.Inject

class SaveCoSplitUserUpiIdUseCase @Inject constructor(
    private val repository: CoSplitRepository
) {
    suspend operator fun invoke(email: String, upiId: String): Result<Unit> {
        return repository.saveUserUpiId(email, upiId)
    }
}
