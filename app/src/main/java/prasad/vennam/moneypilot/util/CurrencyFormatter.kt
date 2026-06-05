package prasad.vennam.moneypilot.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import androidx.compose.runtime.compositionLocalOf

val LocalCurrencyCode = compositionLocalOf { "INR" }

object CurrencyFormatter {

    /**
     * Formats an amount using the specified currency code.
     * Uses the default locale for number formatting (thousands separators, etc.)
     * but injects the correct currency symbol based on the currencyCode.
     */
    fun format(amount: Double, currencyCode: String): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
            format.currency = Currency.getInstance(currencyCode)
            // Handle edge case where minor units might not be desired for some currencies
            format.maximumFractionDigits = 2
            format.minimumFractionDigits = 2
            format.format(amount)
        } catch (e: Exception) {
            // Fallback if currency code is somehow invalid
            "%.2f $currencyCode".format(amount)
        }
    }
}
