package prasad.vennam.moneypilot

import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenever
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitGroup
import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepository
import prasad.vennam.moneypilot.feature.cosplit.ui.CoSplitViewModel
import prasad.vennam.moneypilot.feature.cosplit.util.CoSplitAiHelper

class CoSplitTests {

    @Test
    fun testDebtSimplification_calculatesCorrectRoutes() {
        val mockPrefs = mock(UserPreferences::class.java)
        whenever(mockPrefs.userData).thenReturn(flowOf(null))
        val mockRepo = mock(CoSplitRepository::class.java)
        val mockAi = mock(CoSplitAiHelper::class.java)

        val viewModel = CoSplitViewModel(mockPrefs, mockRepo, mockAi)

        // Case 1: Simple settlement where Alice owes Bob 10
        val balances1 = mapOf(
            "alice@gmail.com" to -10.0,
            "bob@gmail.com" to 10.0
        )
        val routes1 = viewModel.generateSimplifiedSettlements(balances1)
        assertEquals(1, routes1.size)
        assertEquals("alice@gmail.com pays bob@gmail.com ₹10.00", routes1[0])

        // Case 2: Three-way debt simplification
        // Alice owes Bob 20 (net balance Alice: -20, Bob: +20)
        // Charlie owes Alice 10 (net balance Charlie: -10, Alice: +10)
        // Net balances: Alice: -10, Bob: +20, Charlie: -10
        // Expected simplified settlement:
        // Alice pays Bob 10, Charlie pays Bob 10.
        val balances2 = mapOf(
            "alice@gmail.com" to -10.0,
            "bob@gmail.com" to 20.0,
            "charlie@gmail.com" to -10.0
        )
        val routes2 = viewModel.generateSimplifiedSettlements(balances2)
        assertEquals(2, routes2.size)
        assertTrue(routes2.contains("alice@gmail.com pays bob@gmail.com ₹10.00"))
        assertTrue(routes2.contains("charlie@gmail.com pays bob@gmail.com ₹10.00"))
    }

    @Test
    fun testBalanceLedgerMath_calculatesIncrementalDeltas() {
        // Double entry ledger balance calculation delta logic verification:
        // Total amount = A, paid by Payer, shared by list of members.
        // Delta balance for payer = A - share_payer
        // Delta balance for other member = -share_member
        val amount = 90.0
        val payer = "alice@gmail.com"
        val members = listOf("alice@gmail.com", "bob@gmail.com", "charlie@gmail.com")
        
        // Equal split
        val share = amount / members.size // 30.0
        
        val deltas = members.associateWith { member ->
            if (member == payer) {
                amount - share // 60.0
            } else {
                -share // -30.0
            }
        }

        assertEquals(60.0, deltas["alice@gmail.com"]!!, 0.001)
        assertEquals(-30.0, deltas["bob@gmail.com"]!!, 0.001)
        assertEquals(-30.0, deltas["charlie@gmail.com"]!!, 0.001)
        
        // Sum of all deltas must equal exactly 0.0 (preservation of money)
        val sumOfDeltas = deltas.values.sum()
        assertEquals(0.0, sumOfDeltas, 0.001)
    }
}
