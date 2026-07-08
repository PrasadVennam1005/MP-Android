package prasad.vennam.moneypilot.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

data class PaymentMode(
    val name: String,
    val icon: ImageVector
)

/**
 * Single source of truth for payment mode options.
 * Used in both AddEditTransactionScreen and HistoryScreen filter to ensure consistency.
 */
object PaymentModes {
    val ALL_MODES = listOf(
        PaymentMode("Cash", Icons.Rounded.Money),
        PaymentMode("UPI", Icons.Rounded.QrCodeScanner),
        PaymentMode("Bank Transfer", Icons.Rounded.AccountBalance),
        PaymentMode("Credit Card", Icons.Rounded.CreditCard),
        PaymentMode("Debit Card", Icons.Rounded.CreditCard),
        PaymentMode("Wallet", Icons.Rounded.AccountBalanceWallet),
        PaymentMode("Cheque", Icons.Rounded.Receipt),
        PaymentMode("Net Banking", Icons.Rounded.Language),
        PaymentMode("Other", Icons.Rounded.Payments)
    )

    val ALL = ALL_MODES.map { it.name }
}
