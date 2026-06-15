package prasad.vennam.moneypilot

import org.junit.Assert.assertEquals
import org.junit.Test
import prasad.vennam.moneypilot.util.LoanIntelligenceUtil

class LoanIntelligenceUtilTest {
    @Test
    fun testCalculateEmi() {
        val emi = LoanIntelligenceUtil.calculateEmi(100000.0, 12.0, 12)
        assertEquals(8884.88, emi, 0.01)
    }

    @Test
    fun testCalculatePrincipal() {
        val principal = LoanIntelligenceUtil.calculatePrincipal(8884.88, 12.0, 12)
        assertEquals(100000.0, principal, 0.1)
    }

    @Test
    fun testCalculateTenure() {
        val tenure = LoanIntelligenceUtil.calculateTenure(100000.0, 8884.88, 12.0)
        assertEquals(12, tenure)
    }

    @Test
    fun testCalculateInterestRate() {
        val rate = LoanIntelligenceUtil.calculateInterestRate(100000.0, 8884.88, 12)
        assertEquals(12.0, rate, 0.01)
    }
}
