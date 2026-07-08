package prasad.vennam.moneypilot.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import prasad.vennam.moneypilot.domain.usecase.*

@OptIn(ExperimentalCoroutinesApi::class)
class EmiCalculatorViewModelTest {
    private val calculateEmiUseCase = CalculateEmiUseCase()
    private val generateScheduleUseCase = GenerateAmortizationScheduleUseCase()
    private val calculatePrepaymentUseCase = CalculatePrepaymentUseCase()
    private val calculateAffordabilityUseCase = CalculateAffordabilityUseCase()
    private val compareLoansUseCase = CompareLoansUseCase()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: EmiCalculatorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel =
            EmiCalculatorViewModel(
                calculateEmiUseCase,
                generateScheduleUseCase,
                calculatePrepaymentUseCase,
                calculateAffordabilityUseCase,
                compareLoansUseCase,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUiStateUpdates() =
        runTest {
            val collectJob =
                launch(testDispatcher) {
                    viewModel.uiState.collect {}
                }

            viewModel.updateAmount("300000")
            waitForCondition { viewModel.uiState.value.amountInput == "300000" }
            assertEquals("300000", viewModel.uiState.value.amountInput)

            viewModel.updateRate("9.5")
            waitForCondition { viewModel.uiState.value.rateInput == "9.5" }
            assertEquals("9.5", viewModel.uiState.value.rateInput)

            viewModel.updateTenure("15")
            waitForCondition { viewModel.uiState.value.tenureInput == "15" }
            assertEquals("15", viewModel.uiState.value.tenureInput)

            viewModel.updateShowDetailedReport(true)
            waitForCondition { viewModel.uiState.value.showDetailedReport }
            assertTrue(viewModel.uiState.value.showDetailedReport)

            viewModel.updateIsMonthlyView(false)
            waitForCondition { !viewModel.uiState.value.isMonthlyView }
            assertFalse(viewModel.uiState.value.isMonthlyView)

            viewModel.updateSearchQuery("Year 1")
            waitForCondition { viewModel.uiState.value.searchQuery == "Year 1" }
            assertEquals("Year 1", viewModel.uiState.value.searchQuery)

            viewModel.updatePageIndex(2)
            waitForCondition { viewModel.uiState.value.pageIndex == 2 }
            assertEquals(2, viewModel.uiState.value.pageIndex)

            collectJob.cancel()
        }

    @Test
    fun testUiStateUpdates_otherLoanTab() =
        runTest {
            val collectJob =
                launch(testDispatcher) {
                    viewModel.uiState.collect {}
                }

            viewModel.updateTab(3, "USD")
            waitForCondition { viewModel.uiState.value.selectedTab == 3 }
            assertEquals(3, viewModel.uiState.value.selectedTab)
            assertEquals("25000", viewModel.uiState.value.amountInput)
            assertEquals("10.0", viewModel.uiState.value.rateInput)
            assertEquals("5", viewModel.uiState.value.tenureInput)

            viewModel.updateTab(3, "INR")
            waitForCondition { viewModel.uiState.value.amountInput == "500000" }
            assertEquals("500000", viewModel.uiState.value.amountInput)

            collectJob.cancel()
        }

    private suspend fun waitForCondition(
        timeoutMs: Long = 2000,
        condition: () -> Boolean,
    ) {
        val startTime = System.currentTimeMillis()
        while (!condition() && System.currentTimeMillis() - startTime < timeoutMs) {
            delay(10)
        }
    }
}
