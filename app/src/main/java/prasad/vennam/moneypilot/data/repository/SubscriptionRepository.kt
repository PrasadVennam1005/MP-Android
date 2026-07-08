package prasad.vennam.moneypilot.data.repository

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.dao.SubscriptionDao
import prasad.vennam.moneypilot.data.entity.Subscription
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository
    @Inject
    constructor(
        private val subscriptionDao: SubscriptionDao,
    ) {
        val allSubscriptions: Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions()

        suspend fun insertSubscription(subscription: Subscription) = subscriptionDao.insertSubscription(subscription)

        suspend fun updateSubscription(subscription: Subscription) = subscriptionDao.updateSubscription(subscription)

        suspend fun deleteSubscription(subscription: Subscription) = subscriptionDao.deleteSubscription(subscription)

        suspend fun getSubscriptionById(id: Long): Subscription? = subscriptionDao.getSubscriptionById(id)
    }
