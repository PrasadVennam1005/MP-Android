package prasad.vennam.moneypilot.util

import androidx.compose.runtime.compositionLocalOf
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private const val DEFAULT_CURRENCY = "INR"

val LocalCurrencyCode = compositionLocalOf { DEFAULT_CURRENCY }

object CurrencyFormatter {
    private const val FALLBACK_FORMAT = "%.2f %s"

    /**
     * Formats an amount using the specified currency code.
     * Uses the default locale for number formatting (thousands separators, etc.)
     * but injects the correct currency symbol based on the currencyCode.
     */
    fun format(
        amount: Double,
        currencyCode: String,
    ): String =
        try {
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
            format.currency = Currency.getInstance(currencyCode)
            // Handle edge case where minor units might not be desired for some currencies
            format.maximumFractionDigits = 2
            format.minimumFractionDigits = 2
            format.format(amount)
        } catch (_: Exception) {
            // Fallback if currency code is somehow invalid
            String.format(Locale.getDefault(), FALLBACK_FORMAT, amount, currencyCode)
        }
}
