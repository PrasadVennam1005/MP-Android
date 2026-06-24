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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever
import prasad.vennam.moneypilot.data.entity.SavingGoal
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SavingGoalViewModelTest {

    private val repository = mock(MoneyPilotRepository::class.java)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(repository.allSavingGoals).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSaveSavingGoalNewNotCompleted() = runTest {
        val viewModel = SavingGoalViewModel(repository)
        val goal = SavingGoal(
            id = 0L,
            name = "New Laptop",
            targetAmount = 5000000L,
            currentSavedAmount = 1000000L,
            deadline = 1800000000000L,
            isCompleted = false
        )

        viewModel.saveSavingGoal(goal)
        verify(repository).insertSavingGoal(goal.copy(isCompleted = false))
    }

    @Test
    fun testSaveSavingGoalNewCompleted() = runTest {
        val viewModel = SavingGoalViewModel(repository)
        val goal = SavingGoal(
            id = 0L,
            name = "New Laptop",
            targetAmount = 5000000L,
            currentSavedAmount = 5000000L,
            deadline = 1800000000000L,
            isCompleted = false
        )

        viewModel.saveSavingGoal(goal)
        verify(repository).insertSavingGoal(goal.copy(isCompleted = true))
    }

    @Test
    fun testSaveSavingGoalExisting() = runTest {
        val viewModel = SavingGoalViewModel(repository)
        val goal = SavingGoal(
            id = 15L,
            name = "Vacation",
            targetAmount = 10000000L,
            currentSavedAmount = 2000000L,
            deadline = 1800000000000L,
            isCompleted = false
        )

        viewModel.saveSavingGoal(goal)
        verify(repository).updateSavingGoal(goal.copy(isCompleted = false))
    }

    @Test
    fun testDeleteSavingGoal() = runTest {
        val viewModel = SavingGoalViewModel(repository)
        val goal = SavingGoal(
            id = 15L,
            name = "Vacation",
            targetAmount = 10000000L,
            currentSavedAmount = 2000000L,
            deadline = 1800000000000L,
            isCompleted = false
        )

        viewModel.deleteSavingGoal(goal)
        verify(repository).deleteSavingGoal(goal)
    }

    @Test
    fun testDepositToGoal() = runTest {
        val viewModel = SavingGoalViewModel(repository)
        val goal = SavingGoal(
            id = 15L,
            name = "Vacation",
            targetAmount = 10000000L,
            currentSavedAmount = 2000000L,
            deadline = 1800000000000L,
            isCompleted = false
        )

        viewModel.depositToGoal(goal, 3000000L)
        verify(repository).updateSavingGoal(
            safeArgThat { updatedGoal ->
                updatedGoal.currentSavedAmount == 5000000L && !updatedGoal.isCompleted
            }
        )
    }

    @Test
    fun testWithdrawFromGoal() = runTest {
        val viewModel = SavingGoalViewModel(repository)
        val goal = SavingGoal(
            id = 15L,
            name = "Vacation",
            targetAmount = 10000000L,
            currentSavedAmount = 5000000L,
            deadline = 1800000000000L,
            isCompleted = false
        )

        viewModel.withdrawFromGoal(goal, 2000000L)
        verify(repository).updateSavingGoal(
            safeArgThat { updatedGoal ->
                updatedGoal.currentSavedAmount == 3000000L && !updatedGoal.isCompleted
            }
        )
    }

    private fun <T> safeArgThat(matcher: (T) -> Boolean): T {
        org.mockito.Mockito.argThat(matcher)
        return castNull()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> castNull(): T = null as T
}
