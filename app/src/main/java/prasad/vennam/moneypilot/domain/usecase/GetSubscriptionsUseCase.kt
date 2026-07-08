package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.Subscription
import prasad.vennam.moneypilot.data.repository.SubscriptionRepository
import javax.inject.Inject

class GetSubscriptionsUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    operator fun invoke(): Flow<List<Subscription>> = repository.allSubscriptions
}
