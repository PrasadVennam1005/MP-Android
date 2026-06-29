package prasad.vennam.moneypilot.util

import java.util.Locale
import java.util.regex.Pattern

data class ParsedNotification(
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val merchant: String,
    val bankAccount: String,
)

object NotificationParser {
    // Regex for matching amount (e.g., Rs. 500, Rs 500.50, INR 1500, $50, 20.50 USD)
    private val amountPrefixPattern =
        Pattern.compile(
            "(?i)(?:rs\\.?|inr|usd|eur|gbp|aed|sar|aud|cad|sgd|cny|jpy|krw|inr|\\$|€|£|¥|₩)\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        )
    private val amountSuffixPattern =
        Pattern.compile(
            "(?i)([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:rs\\.?|inr|usd|eur|gbp|rupees|dollars|euros|cents|paisa)",
        )

    // Keywords to classify transactions
    private val expenseKeywords =
        setOf(
            "debited",
            "spent",
            "paid",
            "payment",
            "sent",
            "withdrawn",
            "purchase",
            "txn",
            "transaction",
            "charged",
            "debit",
            "transfer to",
        )
    private val incomeKeywords =
        setOf(
            "credited",
            "received",
            "deposited",
            "added",
            "refund",
            "cashback",
            "credit",
        )

    private val merchantPatterns =
        listOf(
            Pattern.compile("(?i)info:\\s*(?:upi/\\d+/)?([^/]+)"), // Handles Info: Swiggy or Info: UPI/123/Swiggy/Remark
            Pattern.compile("(?i)upi/\\d+/([^/]+)"), // Handles UPI/123/Swiggy/Remark directly
            Pattern.compile("(?i)\\b(?:at|to|for|vpa|transfer to)\\b\\s+([A-Za-z0-9\\s&*'-]+)"), // Removed 'on'
            Pattern.compile("(?i)paid\\s+([A-Za-z0-9\\s&*'-]+)\\s+(?:to|Rs|inr)"),
            Pattern.compile("(?i)sent\\s+([A-Za-z0-9\\s&*'-]+)\\s+to"),
        )

    private val merchantIgnoreKeywords =
        setOf(
            "your",
            "account",
            "a/c",
            "bank",
            "card",
            "using",
            "via",
            "otp",
            "code",
            "password",
            "balance",
            "bal",
            "limit",
            "available",
            "avbl",
            "success",
            "successful",
            "ref",
            "txn",
            "transaction",
            "withdrawn",
            "credited",
            "debited",
            "deposited",
            "paid",
            "sent",
            "received",
        )

    // Patterns for matching account identifiers (e.g., a/c ending in 1234, card xx1234)
    private val accountPattern =
        Pattern.compile(
            "(?i)(?:a/c|acct|account|card|ending|xx)\\s*(?:no\\.?\\s*)?\\b([*Xx]*\\d{3,4})\\b",
        )

    fun parse(
        title: String,
        text: String,
        packageName: String? = null,
    ): ParsedNotification? {
        val fullText = "$title $text".replace("\n", " ").trim()
        val lowerText = fullText.lowercase(Locale.getDefault())

        // 1. Extract Amount
        val amount = extractAmount(fullText) ?: return null

        // 2. Extract Type (default to EXPENSE/debit as it's the most common case)
        val type =
            when {
                incomeKeywords.any { lowerText.contains(it) } -> "INCOME"
                expenseKeywords.any { lowerText.contains(it) } -> "EXPENSE"
                else -> "EXPENSE"
            }

        // 3. Extract Merchant
        val merchant = extractMerchant(fullText) ?: getAppNameFromPackage(packageName)

        // 4. Extract Bank/Source Account info
        val bankAccount = extractBankAccount(fullText) ?: getAppNameFromPackage(packageName)

        return ParsedNotification(
            amount = amount,
            type = type,
            merchant = merchant,
            bankAccount = bankAccount,
        )
    }

    private fun extractAmount(text: String): Double? {
        var matcher = amountPrefixPattern.matcher(text)
        if (matcher.find()) {
            matcher.group(1)?.replace(",", "")?.toDoubleOrNull()?.let {
                if (it > 0) return it
            }
        }

        matcher = amountSuffixPattern.matcher(text)
        if (matcher.find()) {
            matcher.group(1)?.replace(",", "")?.toDoubleOrNull()?.let {
                if (it > 0) return it
            }
        }

        return null
    }

    private fun extractMerchant(text: String): String? {
        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim() ?: continue
                // Filter candidate to get a meaningful merchant name
                val cleaned = cleanMerchantName(candidate)
                if (cleaned.isNotEmpty()) {
                    return cleaned
                }
            }
        }
        return null
    }

    private fun cleanMerchantName(candidate: String): String {
        // Stop at words indicating bank balance, reference numbers, or transaction ends
        val stopWords =
            listOf(
                "ref",
                "txn",
                "bal",
                "balance",
                "available",
                "avbl",
                "using",
                "via",
                "card",
                "on",
                "at",
                "date",
                "linked",
                "effective",
                "limit",
                "chg",
                "charge",
                "with",
                "from",
                "for",
                "acct",
                "a/c",
                "account",
            )

        var words = candidate.split(Regex("\\s+"))
        val filtered = mutableListOf<String>()

        for (word in words) {
            val lowerWord = word.lowercase(Locale.getDefault())
            // If we hit any stop-word, truncate the merchant name here
            if (stopWords.any { lowerWord == it || lowerWord.startsWith(it) }) {
                break
            }
            // Skip numeric-only parts (like transaction IDs)
            if (word.all { it.isDigit() } && word.length > 4) {
                break
            }
            // Skip dates and times (e.g., 22-06-26, 22-Jun, 12:30)
            val isDateOrTime =
                word.matches(Regex("\\d{1,2}[-/](?:\\d{1,2}|[a-zA-Z]{3})[-/]\\d{2,4}")) ||
                    word.matches(Regex("\\d{1,2}:\\d{2}(?::\\d{2})?.*"))
            if (isDateOrTime) {
                break
            }
            if (merchantIgnoreKeywords.contains(lowerWord)) {
                continue
            }
            filtered.add(word)
        }

        return filtered
            .joinToString(" ")
            .replace(Regex("[^A-Za-z0-9\\s&'-]"), "") // Clean up punctuation
            .trim()
            .take(30) // Limit length
    }

    private fun extractBankAccount(text: String): String? {
        val matcher = accountPattern.matcher(text)
        if (matcher.find()) {
            val acctNum = matcher.group(1) ?: return null
            return if (acctNum.startsWith("x", true) || acctNum.startsWith("*")) {
                "A/c $acctNum"
            } else {
                "A/c XX$acctNum"
            }
        }
        return null
    }

    private fun getAppNameFromPackage(packageName: String?): String {
        if (packageName == null) return "Bank Notification"
        return when (packageName) {
            "com.google.android.apps.nbu.paisa.user" -> "Google Pay"
            "com.phonepe.app" -> "PhonePe"
            "net.one97.paytm" -> "Paytm"
            "com.google.android.apps.messaging", "com.android.mms" -> "SMS Alert"
            "com.paypal.android.p2pmobile" -> "PayPal"
            "com.venmo" -> "Venmo"
            "com.squareup.cash" -> "Cash App"
            else -> "Bank Notification"
        }
    }
}
