package prasad.vennam.moneypilot.util

import java.util.Locale
import java.util.regex.Pattern

data class ParsedNotification(
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val merchant: String,
    val bankAccount: String,
)

data class ParsedAutopay(
    val amount: Double,
    val merchant: String,
    val scheduledDate: Long,
    val paymentApp: String?,
    val upiMandateId: String?,
)

object NotificationParser {
    // Regex for matching amount (e.g., Rs. 500, Rs 500.50, INR 1500, $50, 20.50 USD) - Captured currency prefix
    private val amountPrefixPattern =
        Pattern.compile(
            "(?i)(rs\\.?|inr|usd|eur|gbp|aed|sar|aud|cad|sgd|cny|jpy|krw|\\$|€|£|¥|₩)\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        )
    private val amountSuffixPattern =
        Pattern.compile(
            "(?i)([\\d,]+(?:\\.\\d{1,2})?)\\s*(rs\\.?|inr|usd|eur|gbp|rupees|dollars|euros|cents|paisa)",
        )

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

    private data class AmountCandidate(
        val amount: Double,
        val index: Int,
        val currency: String,
    )

    fun parse(
        title: String,
        text: String,
        packageName: String? = null,
    ): ParsedNotification? {
        val fullText = "$title $text".replace("\n", " ").trim()
        val lowerText = fullText.lowercase(Locale.getDefault())

        // 1. Discard credit card bills / statement due notifications early
        val isStatementNotice = lowerText.contains("statement for") || 
                                lowerText.contains("statement of") ||
                                lowerText.contains("bill of") || 
                                lowerText.contains("total amount due") || 
                                lowerText.contains("minimum due") || 
                                lowerText.contains("due by") || 
                                lowerText.contains("due on")
        if (isStatementNotice) return null

        // 2. Discard aggregated system tray summaries
        val isGroupedAlert = lowerText.contains("transactions successful") || 
                             lowerText.contains("new alerts") || 
                             lowerText.contains("notifications")
        if (isGroupedAlert) return null

        // 3. Discard non-financial alerts (OTPs, simple security checks, etc.) early
        val isOtp = lowerText.contains("otp") || lowerText.contains("verification code") || lowerText.contains("one time password") || lowerText.contains("one-time password")
        if (isOtp) return null

        // 4. Extract Amount (utilizing lookback check to prioritize transaction over balance)
        val amount = extractAmount(fullText) ?: return null

        // 5. Classify transaction type and check if it contains actual debit/credit keywords
        val hasIncomeKeyword = incomeKeywords.any { lowerText.contains(it) }
        val hasExpenseKeyword = expenseKeywords.any { lowerText.contains(it) }
        
        // Discard simple balance queries or limit alerts that are not transactions
        if (!hasIncomeKeyword && !hasExpenseKeyword) {
            val isBalanceAlert = lowerText.contains("balance") || lowerText.contains("bal:") || lowerText.contains("available") || lowerText.contains("avl bal")
            val isLimitAlert = lowerText.contains("limit")
            if (isBalanceAlert || isLimitAlert) {
                return null
            }
        }

        val type = when {
            hasIncomeKeyword -> "INCOME"
            hasExpenseKeyword -> "EXPENSE"
            else -> "EXPENSE"
        }

        // 6. Extract Merchant
        val merchant = extractMerchant(fullText) ?: getAppNameFromPackage(packageName)

        // 7. Extract Bank/Source Account info
        val bankAccount = extractBankAccount(fullText) ?: getAppNameFromPackage(packageName)

        return ParsedNotification(
            amount = amount,
            type = type,
            merchant = merchant,
            bankAccount = bankAccount,
        )
    }

    private fun extractAmount(text: String): Double? {
        val lowerText = text.lowercase(Locale.getDefault())
        val balanceKeywords = listOf("bal", "balance", "available", "avl")
        val txKeywords = listOf("debited", "credited", "spent", "paid", "charged", "received", "sent", "withdrawn", "purchase", "payment", "txn", "debit", "credit")

        val candidates = mutableListOf<AmountCandidate>()

        // prefix matches
        var matcher = amountPrefixPattern.matcher(text)
        while (matcher.find()) {
            val currencySymbol = matcher.group(1)?.lowercase() ?: ""
            val amount = matcher.group(2)?.replace(",", "")?.toDoubleOrNull()
            if (amount != null && amount > 0) {
                candidates.add(AmountCandidate(amount, matcher.start(), currencySymbol))
            }
        }

        // suffix matches
        matcher = amountSuffixPattern.matcher(text)
        while (matcher.find()) {
            val amount = matcher.group(1)?.replace(",", "")?.toDoubleOrNull()
            val currencySymbol = matcher.group(2)?.lowercase() ?: ""
            if (amount != null && amount > 0) {
                candidates.add(AmountCandidate(amount, matcher.start(), currencySymbol))
            }
        }

        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates[0].amount

        // Sort by occurrence index
        candidates.sortBy { it.index }

        var bestCandidate: AmountCandidate? = null
        var bestScore = -1

        for (candidate in candidates) {
            val index = candidate.index
            val startLookback = maxOf(0, index - 25)
            val lookbackText = lowerText.substring(startLookback, index)

            val isPrecededByBalance = balanceKeywords.any { lookbackText.contains(it) }
            val isPrecededByTx = txKeywords.any { lookbackText.contains(it) }

            // Score based on preceding keywords
            var score = when {
                isPrecededByBalance -> 0
                isPrecededByTx -> 20
                else -> 10
            }

            // Score boost: Prioritize local/home currencies (INR/Rs/₹) over foreign ones (USD/$) when multiple values exist
            val isHomeCurrency = candidate.currency.contains("inr") || 
                                 candidate.currency.contains("rs") || 
                                 candidate.currency.contains("₹") || 
                                 candidate.currency.contains("rupees")
            if (isHomeCurrency) {
                score += 5
            }

            if (score > bestScore) {
                bestScore = score
                bestCandidate = candidate
            }
        }

        return bestCandidate?.amount ?: candidates.firstOrNull()?.amount
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

        val words = candidate.split(Regex("\\s+"))
        val filtered = mutableListOf<String>()

        for (word in words) {
            val cleanWord = word.replace(Regex("[^A-Za-z0-9]"), "").lowercase(Locale.getDefault())
            // Check if cleanWord matches any cleaned stopWord to avoid aggressive prefix triggers (e.g. ATM matching AT)
            val isStopWord = stopWords.any { stopWord ->
                val cleanStop = stopWord.replace(Regex("[^A-Za-z0-9]"), "").lowercase(Locale.getDefault())
                cleanWord == cleanStop
            }
            if (isStopWord) {
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
            if (merchantIgnoreKeywords.contains(cleanWord)) {
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

    fun parseAutopay(
        sender: String,
        message: String,
        packageName: String? = null
    ): ParsedAutopay? {
        val lowerText = message.lowercase()
        // Check if message is an Autopay/Mandate alert
        val isAutopay = lowerText.contains("autopay") ||
                lowerText.contains("auto-pay") ||
                lowerText.contains("mandate") ||
                lowerText.contains("standing instruction") ||
                lowerText.contains("scheduled debit") ||
                lowerText.contains("auto-debit") ||
                lowerText.contains("auto debit")

        if (!isAutopay) return null

        // 1. Extract Amount
        var amountVal = 0.0
        val prefixMatcher = amountPrefixPattern.matcher(message)
        if (prefixMatcher.find()) {
            amountVal = prefixMatcher.group(2)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        } else {
            val suffixMatcher = amountSuffixPattern.matcher(message)
            if (suffixMatcher.find()) {
                amountVal = suffixMatcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            }
        }
        if (amountVal <= 0.0) return null

        // 2. Extract Merchant
        var merchantName = "Autopay Mandate"
        val forPattern = Pattern.compile("(?i)(?:for|towards|to)\\s+([A-Za-z0-9\\s&*'-]+)")
        val forMatcher = forPattern.matcher(message)
        if (forMatcher.find()) {
            val candidate = forMatcher.group(1) ?: ""
            // Truncate at common boundaries
            val cleanedCandidate = candidate
                .split(Regex("(?i)\\b(?:is|on|scheduled|due|to|revoke|manage|register|setup|created|by|at)\\b"))[0]
                .trim()
            if (cleanedCandidate.isNotEmpty()) {
                merchantName = cleanMerchantName(cleanedCandidate)
            }
        }

        // 3. Extract Scheduled Date
        var scheduledTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000L // Default to 24h from now
        val datePattern = Pattern.compile("\\b(\\d{1,2})[-/](\\d{1,2}|[A-Za-z]{3})[-/](\\d{2,4})\\b")
        val dateMatcher = datePattern.matcher(message)
        if (dateMatcher.find()) {
            val day = dateMatcher.group(1)?.toIntOrNull() ?: 1
            val monthStr = dateMatcher.group(2) ?: "1"
            val yearStr = dateMatcher.group(3) ?: "26"

            val month = when (monthStr.lowercase()) {
                "jan" -> 0
                "feb" -> 1
                "mar" -> 2
                "apr" -> 3
                "may" -> 4
                "jun" -> 5
                "jul" -> 6
                "aug" -> 7
                "sep" -> 8
                "oct" -> 9
                "nov" -> 10
                "dec" -> 11
                else -> (monthStr.toIntOrNull() ?: 1) - 1
            }

            var year = yearStr.toIntOrNull() ?: 2026
            if (year < 100) year += 2000

            val cal = java.util.Calendar.getInstance()
            cal.set(year, month, day, 10, 0, 0) // Default to 10:00 AM on that day
            scheduledTime = cal.timeInMillis
        }

        // 4. Extract UPI Mandate ID
        var mandateId: String? = null
        val mandatePattern = Pattern.compile("(?i)(?:mandate|umn|instruction)\\s*(?:id|no|number)?\\s*[:=]?\\s*([a-zA-Z0-9@.-]+)")
        val mandateMatcher = mandatePattern.matcher(message)
        if (mandateMatcher.find()) {
            mandateId = mandateMatcher.group(1)
        }

        // 5. Extract Payment App
        var appName: String? = getAppNameFromPackage(packageName)
        if (appName == "Bank Notification" || appName == "SMS Alert" || appName == null) {
            val matchedApp = when {
                lowerText.contains("gpay") || lowerText.contains("google pay") -> "Google Pay"
                lowerText.contains("phonepe") -> "PhonePe"
                lowerText.contains("paytm") -> "Paytm"
                lowerText.contains("bhim") -> "BHIM"
                else -> null
            }
            appName = matchedApp
        }

        return ParsedAutopay(
            amount = amountVal,
            merchant = merchantName,
            scheduledDate = scheduledTime,
            paymentApp = appName,
            upiMandateId = mandateId
        )
    }
}
