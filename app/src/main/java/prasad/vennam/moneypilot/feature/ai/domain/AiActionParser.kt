package prasad.vennam.moneypilot.feature.ai.domain

import android.util.Log
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.feature.ai.model.AiAction

/**
 * Parses structured action tags embedded in the AI model's response.
 *
 * Tag format:
 *   [ACTION:TYPE|key=value|key=value|...]
 *
 * Supported types:
 *   ADD_EXPENSE    — amount, category, note, date (optional, default "today")
 *   ADD_INCOME     — amount, category, note, date (optional)
 *   ADD_INVESTMENT — name, type, amount, current_value (optional)
 *   ADD_LOAN       — name, amount, emi, next_emi_days (optional)
 *
 * Example tag (produced by the model):
 *   [ACTION:ADD_EXPENSE|amount=500|category=Food|note=Swiggy|date=today]
 *
 * Returns a Pair<AiAction?, String> where the String is the cleaned text
 * with the action tag stripped out.
 */
object AiActionParser {
    private val TAG = "AiActionParser"

    // Matches [ACTION:TYPE|key=value|...] including multi-line variants
    private val ACTION_REGEX =
        Regex(
            """\[ACTION:([A-Z_]+)\|([^\]]+)\]""",
            setOf(RegexOption.IGNORE_CASE),
        )

    /**
     * Parse the model's raw response.
     * @return Pair of (parsed action or null, cleaned display text)
     */
    fun parse(rawResponse: String): Pair<AiAction?, String> {
        val match = ACTION_REGEX.find(rawResponse) ?: return Pair(null, rawResponse.trim())

        val actionType = match.groupValues[1].uppercase()
        val paramsRaw = match.groupValues[2]

        // Parse key=value pairs
        val params =
            paramsRaw
                .split("|")
                .mapNotNull { segment ->
                    val idx = segment.indexOf('=')
                    if (idx < 0) {
                        null
                    } else {
                        segment.substring(0, idx).trim().lowercase() to segment.substring(idx + 1).trim()
                    }
                }.toMap()

        Log.d(TAG, "Parsed action type=$actionType, params=$params")

        val action: AiAction? =
            try {
                when (actionType) {
                    "ADD_EXPENSE" -> parseTransaction(params, TransactionType.EXPENSE)
                    "ADD_INCOME" -> parseTransaction(params, TransactionType.INCOME)
                    "ADD_INVESTMENT" -> parseInvestment(params)
                    "ADD_LOAN" -> parseLoan(params)
                    else -> {
                        Log.w(TAG, "Unknown action type: $actionType")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse action params: ${e.message}")
                null
            }

        // Strip the action tag from the text shown to the user
        val cleanedText = rawResponse.replace(match.value, "").trim()
        return Pair(action, cleanedText)
    }

    // ── Private parsers ──────────────────────────────────────────────────────

    private fun parseTransaction(
        params: Map<String, String>,
        type: TransactionType,
    ): AiAction.AddTransaction {
        val amount = parseAmount(params["amount"] ?: params["amt"] ?: "0")
        val category = params["category"] ?: params["cat"] ?: "Other"
        val note = params["note"] ?: params["description"] ?: params["desc"] ?: ""
        val date = params["date"] ?: "today"
        val offset = parseDateOffset(date)
        return AiAction.AddTransaction(
            amount = amount,
            type = type,
            categoryName = category.trim(),
            note = note.trim(),
            dateOffset = offset,
        )
    }

    private fun parseInvestment(params: Map<String, String>): AiAction.AddInvestment {
        val name = params["name"] ?: params["fund"] ?: params["stock"] ?: "Investment"
        val invType = params["type"] ?: params["investment_type"] ?: "Mutual Fund"
        val invested = parseAmount(params["amount"] ?: params["invested"] ?: params["invested_amount"] ?: "0")
        val current = parseAmount(params["current_value"] ?: params["current"] ?: params["value"] ?: invested.toString())
        return AiAction.AddInvestment(
            name = name.trim(),
            type = invType.trim(),
            investedAmount = invested,
            currentValue = current,
        )
    }

    private fun parseLoan(params: Map<String, String>): AiAction.AddLoan {
        val name = params["name"] ?: params["lender"] ?: "Loan"
        val total = parseAmount(params["amount"] ?: params["total"] ?: params["principal"] ?: "0")
        val emi = parseAmount(params["emi"] ?: params["emi_amount"] ?: params["monthly_emi"] ?: "0")
        val nextEmiDays = (params["next_emi_days"] ?: params["days"] ?: "30").toIntOrNull() ?: 30
        return AiAction.AddLoan(
            name = name.trim(),
            totalAmount = total,
            emiAmount = emi,
            nextEmiDays = nextEmiDays,
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Parses amount strings like "500", "1,500", "1.5L", "50000", "50 thousand"
     * Returns whole Rupees as Long.
     */
    private fun parseAmount(raw: String): Long {
        val cleaned = raw.replace(",", "").trim().lowercase()
        return when {
            cleaned.endsWith("l") || cleaned.endsWith("lakh") || cleaned.endsWith("lakhs") -> {
                val num =
                    cleaned
                        .replace("lakh", "")
                        .replace("lakhs", "")
                        .replace("l", "")
                        .trim()
                        .toDoubleOrNull() ?: 0.0
                (num * 100_000).toLong()
            }
            cleaned.endsWith("cr") || cleaned.endsWith("crore") -> {
                val num =
                    cleaned
                        .replace("crore", "")
                        .replace("cr", "")
                        .trim()
                        .toDoubleOrNull() ?: 0.0
                (num * 10_000_000).toLong()
            }
            cleaned.endsWith("k") || cleaned.endsWith("thousand") -> {
                val num =
                    cleaned
                        .replace("thousand", "")
                        .replace("k", "")
                        .trim()
                        .toDoubleOrNull() ?: 0.0
                (num * 1000).toLong()
            }
            else -> cleaned.toDoubleOrNull()?.toLong() ?: 0L
        }
    }

    /**
     * Maps date strings to day offsets from today.
     * 0 = today, -1 = yesterday
     */
    private fun parseDateOffset(date: String): Int =
        when (date.lowercase().trim()) {
            "today", "now", "" -> 0
            "yesterday" -> -1
            else -> {
                // Try to parse numeric offset like "-2" or "2 days ago"
                val numeric =
                    date
                        .replace("days ago", "")
                        .replace("day ago", "")
                        .trim()
                        .toIntOrNull()
                if (numeric != null) -kotlin.math.abs(numeric) else 0
            }
        }
}
