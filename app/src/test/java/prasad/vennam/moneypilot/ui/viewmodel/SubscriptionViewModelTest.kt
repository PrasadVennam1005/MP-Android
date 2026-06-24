package prasad.vennam.moneypilot.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever
import prasad.vennam.moneypilot.data.entity.Subscription
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModelTest {

    private val repository = mock(MoneyPilotRepository::class.java)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(repository.allSubscriptions).thenReturn(flowOf(emptyList()))
        whenever(repository.allCategories).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSaveSubscriptionNew() = runTest {
        val viewModel = SubscriptionViewModel(repository)
        val newSubscription = Subscription(
            id = 0L,
            name = "Netflix",
            amount = 19900L,
            billingCycle = "Monthly",
            nextPaymentDate = 1700000000000L,
            categoryId = null
        )

        viewModel.saveSubscription(newSubscription)
        verify(repository).insertSubscription(newSubscription)
    }

    @Test
    fun testSaveSubscriptionExisting() = runTest {
        val viewModel = SubscriptionViewModel(repository)
        val existingSubscription = Subscription(
            id = 10L,
            name = "Netflix Premium",
            amount = 64900L,
            billingCycle = "Monthly",
            nextPaymentDate = 1700000000000L,
            categoryId = null
        )

        viewModel.saveSubscription(existingSubscription)
        verify(repository).updateSubscription(existingSubscription)
    }

    @Test
    fun testDeleteSubscription() = runTest {
        val viewModel = SubscriptionViewModel(repository)
        val target = Subscription(
            id = 5L,
            name = "Spotify",
            amount = 11900L,
            billingCycle = "Monthly",
            nextPaymentDate = 1700000000000L,
            categoryId = null
        )

        viewModel.deleteSubscription(target)
        verify(repository).deleteSubscription(target)
    }
}
