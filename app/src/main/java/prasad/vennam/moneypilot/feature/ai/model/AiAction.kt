package prasad.vennam.moneypilot.feature.ai.model

import prasad.vennam.moneypilot.data.entity.TransactionType

/**
 * Represents a structured action the AI can execute against the local database.
 *
 * The model signals these via a structured tag in its response:
 *   [ACTION:ADD_EXPENSE|amount=500|category=Food|note=Swiggy|date=today]
 *
 * The tag is stripped from the displayed text, parsed into an AiAction,
 * and shown to the user as a confirmation card before any DB write happens.
 */
sealed class AiAction {
    /**
     * Add an EXPENSE or INCOME transaction.
     * @param amount   Amount in whole or fractional Rupees (e.g. 500.50 means ₹500.50)
     * @param type     EXPENSE or INCOME
     * @param categoryName  Raw category name from the model (fuzzy-matched to DB category)
     * @param note     Free-text note / merchant name
     * @param dateOffset  Days relative to today (0 = today, -1 = yesterday)
     */
    data class AddTransaction(
        val amount: Double,
        val type: TransactionType,
        val categoryName: String,
        val note: String,
        val dateOffset: Int = 0,
    ) : AiAction()

    /**
     * Add an investment entry.
     * @param name           Investment name (e.g. "HDFC Top 100")
     * @param type           Investment type (Stock, Mutual Fund, Crypto, FD, Gold, Real Estate)
     * @param investedAmount Amount invested in Rupees
     * @param currentValue   Current value in Rupees (defaults to investedAmount if not specified)
     */
    data class AddInvestment(
        val name: String,
        val type: String,
        val investedAmount: Double,
        val currentValue: Double,
    ) : AiAction()

    /**
     * Add a loan entry.
     * @param name         Loan name / lender (e.g. "SBI Home Loan")
     * @param totalAmount  Principal amount in Rupees
     * @param emiAmount    Monthly EMI in Rupees
     * @param nextEmiDays  Days from today until next EMI (default 30)
     */
    data class AddLoan(
        val name: String,
        val totalAmount: Double,
        val emiAmount: Double,
        val interestRate: Double = 0.0,
        val tenureMonths: Int = 12,
        val nextEmiDays: Int = 30,
    ) : AiAction()
}

private fun formatAmount(value: Double): String {
    return if (value % 1 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", value)
    }
}

/** Human-readable summary for the confirmation card */
fun AiAction.displaySummary(): String =
    when (this) {
        is AiAction.AddTransaction -> {
            val typeLabel = if (type == TransactionType.EXPENSE) "Expense" else "Income"
            val noteDisplay = if (note.isBlank()) "(no note)" else "\"$note\""
            val dateLabel = when (dateOffset) {
                0 -> "today"
                -1 -> "yesterday"
                else -> "${kotlin.math.abs(dateOffset)} days ago"
            }
            "Add ₹${formatAmount(amount)} $typeLabel · $categoryName · $noteDisplay · $dateLabel"
        }
        is AiAction.AddInvestment ->
            "Add ₹${formatAmount(investedAmount)} investment in $name ($type)"
        is AiAction.AddLoan ->
            "Add loan \"$name\" · Principal ₹${formatAmount(totalAmount)} · EMI ₹${formatAmount(emiAmount)}/month"
    }
