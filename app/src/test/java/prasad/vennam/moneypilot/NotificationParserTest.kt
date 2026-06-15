package prasad.vennam.moneypilot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import prasad.vennam.moneypilot.util.NotificationParser

class NotificationParserTest {
    @Test
    fun testParseStandardDebitNotification() {
        val title = "Bank Alert"
        val text = "Your a/c no. XX1234 has been debited by Rs 1500.00 at Starbucks."
        val result = NotificationParser.parse(title, text, "com.google.android.apps.messaging")

        assertNotNull(result)
        assertEquals(1500.0, result!!.amount, 0.0)
        assertEquals("EXPENSE", result.type)
        assertEquals("Starbucks", result.merchant)
        assertEquals("A/c XX1234", result.bankAccount)
    }

    @Test
    fun testParseUPIPaymentNotification() {
        val title = "UPI Transaction"
        val text = "Paid Rs. 250 to starbucks@upi via GPay."
        val result = NotificationParser.parse(title, text, "com.google.android.apps.nbu.paisa.user")

        assertNotNull(result)
        assertEquals(250.0, result!!.amount, 0.0)
        assertEquals("EXPENSE", result.type)
        assertEquals("starbucks", result.merchant)
        assertEquals("Google Pay", result.bankAccount)
    }

    @Test
    fun testParseCreditNotification() {
        val title = "Salary Credited"
        val text = "Salary of INR 45,000.00 credited to Account ending 9876."
        val result = NotificationParser.parse(title, text, "com.google.android.apps.messaging")

        assertNotNull(result)
        assertEquals(45000.0, result!!.amount, 0.0)
        assertEquals("INCOME", result.type)
        assertEquals("SMS Alert", result.merchant) // Fallback to SMS Alert package name
        assertEquals("A/c XX9876", result.bankAccount)
    }

    @Test
    fun testParseUSDCreditCardSpent() {
        val title = "Card Transaction"
        val text = "Charged $45.50 at Amazon.com on Card ending 4321."
        val result = NotificationParser.parse(title, text, "com.paypal.android.p2pmobile")

        assertNotNull(result)
        assertEquals(45.50, result!!.amount, 0.0)
        assertEquals("EXPENSE", result.type)
        assertEquals("Amazon", result.merchant) // Regex stops at the dot
        assertEquals("A/c XX4321", result.bankAccount)
    }

    @Test
    fun testNonTransactionNotification() {
        val title = "Security Update"
        val text = "Your passcode was changed successfully. If this wasn't you, call support."
        val result = NotificationParser.parse(title, text, "com.google.android.apps.messaging")

        // Since there is no financial amount matching pattern in the text, it should return null
        assertNull(result)
    }
}
