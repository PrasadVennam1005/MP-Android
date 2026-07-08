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

    @Test
    fun testIndianBankSMSWithDateAndUPI() {
        val title = "Bank Alert"
        val text = "UPDATE: Rs. 1250.00 debited from HDFC Bank A/c ending 1234 on 22-06-26 at Swiggy. Avl Bal: Rs. 10000.00."
        val result = NotificationParser.parse(title, text, "com.google.android.apps.messaging")

        assertNotNull(result)
        assertEquals(1250.0, result!!.amount, 0.0)
        assertEquals("EXPENSE", result.type)
        assertEquals("Swiggy", result.merchant) // Should successfully ignore "on 22-06-26"
    }

    @Test
    fun testIndianBankSMSWithUPIInfo() {
        val title = "Bank Alert"
        val text = "Dear Customer, Acct XX1234 is debited with Rs 500.00 on 22-Jun-26. Info: UPI/312345/AmazonPay/123. Available Balance is Rs 12000.00."
        val result = NotificationParser.parse(title, text, "com.google.android.apps.messaging")

        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.0)
        assertEquals("EXPENSE", result.type)
        assertEquals("AmazonPay", result.merchant) // Should extract the UPI info correctly
    }

    @Test
    fun testParseMultiCurrencyConversion() {
        val title = "Conversion Alert"
        val text = "USD 50.00 converted to INR 4,100.00 debited from A/c ending 1234."
        val result = NotificationParser.parse(title, text, "com.google.android.apps.messaging")

        assertNotNull(result)
        assertEquals(4100.0, result!!.amount, 0.0) // Should prioritize local currency (INR)
        assertEquals("EXPENSE", result.type)
        assertEquals("A/c XX1234", result.bankAccount)
    }

    @Test
    fun testParseIgnoreCreditCardBill() {
        val title = "Card Bill"
        val text = "Your credit card statement for a/c ending 9876 has been generated. Total amount due: Rs 5,500.00, Minimum due: Rs 250.00 by 20-July."
        val result = NotificationParser.parse(title, text, "com.google.android.apps.messaging")

        assertNull(result) // Statement/billing notices should be ignored
    }

    @Test
    fun testParseIgnoreAggregatedNotification() {
        val title = "PhonePe"
        val text = "3 transactions successful."
        val result = NotificationParser.parse(title, text, "com.phonepe.app")

        assertNull(result) // Grouped summary notifications should be ignored
    }

    @Test
    fun testParseCashWithdrawal() {
        val title = "ATM Withdrawal"
        val text = "Cash withdrawal of Rs. 10,000.00 from A/c ending 1234 at SBI ATM."
        val result = NotificationParser.parse(title, text, "com.google.android.apps.messaging")

        assertNotNull(result)
        assertEquals(10000.0, result!!.amount, 0.0)
        assertEquals("EXPENSE", result.type)
        assertEquals("SBI ATM", result.merchant)
    }

    @Test
    fun testParseRefundCashback() {
        val title = "Amazon Pay"
        val text = "Congratulations! Cashback of Rs. 150.00 credited to your A/c ending 4321 for Amazon transaction."
        val result = NotificationParser.parse(title, text, "com.google.android.apps.messaging")

        assertNotNull(result)
        assertEquals(150.0, result!!.amount, 0.0)
        assertEquals("INCOME", result.type)
    }

    @Test
    fun testParseIgnoreOTPAlert() {
        val title = "Axis Bank"
        val text = "123456 is the OTP for your transaction of Rs. 5,000.00 at Flipkart. Do not share this code."
        val result = NotificationParser.parse(title, text, "com.google.android.apps.messaging")

        assertNull(result) // OTP alerts must be ignored
    }

    @Test
    fun testParseAutopayAlerts() {
        val message1 = "Autopay mandate of Rs 199.00 for Netflix is scheduled on 09-Jul-26. To revoke, login to GPay."
        val parsed1 = NotificationParser.parseAutopay("HDFCBank", message1)
        assertNotNull(parsed1)
        assertEquals(199.0, parsed1!!.amount, 0.0)
        assertEquals("Netflix", parsed1.merchant)
        assertEquals("Google Pay", parsed1.paymentApp)

        val message2 = "Standing instruction of Rs 1,500.00 to HDFC Life is scheduled on 12-07-26."
        val parsed2 = NotificationParser.parseAutopay("ICICIBank", message2)
        assertNotNull(parsed2)
        assertEquals(1500.0, parsed2!!.amount, 0.0)
        assertEquals("HDFC Life", parsed2.merchant)
        assertNull(parsed2.paymentApp)
    }
}
