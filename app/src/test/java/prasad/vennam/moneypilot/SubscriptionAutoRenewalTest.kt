package prasad.vennam.moneypilot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import prasad.vennam.moneypilot.data.entity.Subscription
import java.util.Calendar

class SubscriptionAutoRenewalTest {

    private fun findMatchedSubscription(
        parsedMerchant: String,
        parsedAmount: Double,
        subscriptions: List<Subscription>
    ): Subscription? {
        return subscriptions.find { subscription ->
            val subNameClean = subscription.name.lowercase().replace(Regex("[^a-z0-9]"), "")
            val merchantClean = parsedMerchant.lowercase().replace(Regex("[^a-z0-9]"), "")
            val nameMatches = (subNameClean.isNotEmpty() && merchantClean.isNotEmpty()) &&
                    (subNameClean.contains(merchantClean) || merchantClean.contains(subNameClean))
            val amountMatches = Math.abs(subscription.amount - (parsedAmount * 100).toLong()) < 500
            nameMatches && amountMatches
        }
    }

    private fun calculateNextPaymentDate(
        currentDate: Long,
        billingCycle: String
    ): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentDate }
        when (billingCycle) {
            "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "Monthly" -> cal.add(Calendar.MONTH, 1)
            "Yearly" -> cal.add(Calendar.YEAR, 1)
            else -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }

    @Test
    fun testAutoMatchSubscriptionSuccess() {
        val subscriptions = listOf(
            Subscription(id = 1L, name = "Netflix Premium", amount = 19900L, billingCycle = "Monthly", nextPaymentDate = 1700000000000L, categoryId = null),
            Subscription(id = 2L, name = "Spotify Family", amount = 11900L, billingCycle = "Monthly", nextPaymentDate = 1700000000000L, categoryId = null)
        )

        val parsedMerchant = "Netflix"
        val parsedAmount = 199.00

        val match = findMatchedSubscription(parsedMerchant, parsedAmount, subscriptions)
        assertNotNull(match)
        assertEquals("Netflix Premium", match!!.name)

        val nextDate = calculateNextPaymentDate(match.nextPaymentDate, match.billingCycle)
        val calExpected = Calendar.getInstance().apply { 
            timeInMillis = 1700000000000L
            add(Calendar.MONTH, 1)
        }
        assertEquals(calExpected.timeInMillis, nextDate)
    }

    @Test
    fun testAutoMatchSubscriptionAmountMismatch() {
        val subscriptions = listOf(
            Subscription(id = 1L, name = "Netflix", amount = 19900L, billingCycle = "Monthly", nextPaymentDate = 1700000000000L, categoryId = null)
        )

        val parsedMerchant = "Netflix"
        val parsedAmount = 149.00

        val match = findMatchedSubscription(parsedMerchant, parsedAmount, subscriptions)
        assertNull(match)
    }
}
