package prasad.vennam.moneypilot.util

/**
 * Single source of truth for payment mode options.
 * Used in both AddEditTransactionScreen and HistoryScreen filter to ensure consistency.
 */
object PaymentModes {
    val ALL = listOf("Cash", "UPI", "Bank Transfer", "Credit Card", "Debit Card", "Wallet")
}
