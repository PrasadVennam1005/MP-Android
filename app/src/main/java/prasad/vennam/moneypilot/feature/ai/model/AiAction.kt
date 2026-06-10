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
     * @param amount   Amount in whole Rupees (e.g. 500 means ₹500)
     * @param type     EXPENSE or INCOME
     * @param categoryName  Raw category name from the model (fuzzy-matched to DB category)
     * @param note     Free-text note / merchant name
     * @param dateOffset  Days relative to today (0 = today, -1 = yesterday)
     */
    data class AddTransaction(
        val amount: Long,
        val type: TransactionType,
        val categoryName: String,
        val note: String,
        val dateOffset: Int = 0,
    ) : AiAction()

    /**
     * Add an investment entry.
     * @param name           Investment name (e.g. "HDFC Top 100")
     * @param type           Investment type (Stock, Mutual Fund, Crypto, FD, Gold, Real Estate)
     * @param investedAmount Amount invested in whole Rupees
     * @param currentValue   Current value in whole Rupees (defaults to investedAmount if not specified)
     */
    data class AddInvestment(
        val name: String,
        val type: String,
        val investedAmount: Long,
        val currentValue: Long,
    ) : AiAction()

    /**
     * Add a loan entry.
     * @param name         Loan name / lender (e.g. "SBI Home Loan")
     * @param totalAmount  Principal amount in whole Rupees
     * @param emiAmount    Monthly EMI in whole Rupees
     * @param nextEmiDays  Days from today until next EMI (default 30)
     */
    data class AddLoan(
        val name: String,
        val totalAmount: Long,
        val emiAmount: Long,
        val nextEmiDays: Int = 30,
    ) : AiAction()
}

/** Human-readable summary for the confirmation card */
fun AiAction.displaySummary(): String =
    when (this) {
        is AiAction.AddTransaction -> {
            val typeLabel = if (type == TransactionType.EXPENSE) "Expense" else "Income"
            val dateLabel =
                when (dateOffset) {
                    0 -> "today"
                    -1 -> "yesterday"
                    else -> "$dateOffset days ago"
                }
            "Add ₹$amount $typeLabel · $categoryName · \"$note\" · $dateLabel"
        }
        is AiAction.AddInvestment ->
            "Add ₹$investedAmount investment in $name ($type)"
        is AiAction.AddLoan ->
            "Add loan \"$name\" · Principal ₹$totalAmount · EMI ₹$emiAmount/month"
    }
