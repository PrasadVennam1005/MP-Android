package prasad.vennam.moneypilot.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmiCalculatorViewModelTest {
    private val viewModel = EmiCalculatorViewModel()

    @Test
    fun testCalculateMonthlyEmi_normalCase() {
        val principal = 100000.0
        val annualRate = 12.0
        val tenureYears = 1
        val expectedEmi = 8884.88 // Standard EMI for 100k at 12% for 1 year
        val emi = viewModel.calculateMonthlyEmi(principal, annualRate, tenureYears)
        assertEquals(expectedEmi, emi, 0.01)
    }

    @Test
    fun testCalculateMonthlyEmi_zeroInterest() {
        val principal = 120000.0
        val annualRate = 0.0
        val tenureYears = 1
        val expectedEmi = 10000.0
        val emi = viewModel.calculateMonthlyEmi(principal, annualRate, tenureYears)
        assertEquals(expectedEmi, emi, 0.01)
    }

    @Test
    fun testGenerateMonthlySchedule_normalCase() {
        val principal = 100000.0
        val annualRate = 12.0
        val tenureYears = 1
        val emi = viewModel.calculateMonthlyEmi(principal, annualRate, tenureYears)
        val schedule = viewModel.generateMonthlySchedule(principal, annualRate, tenureYears, emi)

        assertEquals(12, schedule.size)
        assertEquals(0.0, schedule.last().remainingBalance, 0.01)

        val totalPrincipalPaid = schedule.sumOf { it.principalPaid }
        assertEquals(principal, totalPrincipalPaid, 0.01)
    }

    @Test
    fun testGenerateYearlySchedule() {
        val principal = 100000.0
        val annualRate = 12.0
        val tenureYears = 2
        val emi = viewModel.calculateMonthlyEmi(principal, annualRate, tenureYears)
        val monthlySchedule = viewModel.generateMonthlySchedule(principal, annualRate, tenureYears, emi)
        val yearlySchedule = viewModel.generateYearlySchedule(monthlySchedule)

        assertEquals(2, yearlySchedule.size)
        assertEquals("Year 1", yearlySchedule[0].dateLabel)
        assertEquals("Year 2", yearlySchedule[1].dateLabel)
        assertEquals(0.0, yearlySchedule[1].remainingBalance, 0.01)
    }

    @Test
    fun testUiStateUpdates() {
        viewModel.updateAmount("300000")
        assertEquals("300000", viewModel.uiState.value.amountInput)

        viewModel.updateRate("9.5")
        assertEquals("9.5", viewModel.uiState.value.rateInput)

        viewModel.updateTenure("15")
        assertEquals("15", viewModel.uiState.value.tenureInput)

        viewModel.updateShowDetailedReport(true)
        assertTrue(viewModel.uiState.value.showDetailedReport)

        viewModel.updateIsMonthlyView(false)
        assertFalse(viewModel.uiState.value.isMonthlyView)

        viewModel.updateSearchQuery("Year 1")
        assertEquals("Year 1", viewModel.uiState.value.searchQuery)

        viewModel.updatePageIndex(2)
        assertEquals(2, viewModel.uiState.value.pageIndex)
    }

    @Test
    fun testUiStateUpdates_otherLoanTab() {
        viewModel.updateTab(3, "USD")
        assertEquals(3, viewModel.uiState.value.selectedTab)
        assertEquals("25000", viewModel.uiState.value.amountInput)
        assertEquals("10.0", viewModel.uiState.value.rateInput)
        assertEquals("5", viewModel.uiState.value.tenureInput)

        viewModel.updateTab(3, "INR")
        assertEquals("500000", viewModel.uiState.value.amountInput)
    }
}
