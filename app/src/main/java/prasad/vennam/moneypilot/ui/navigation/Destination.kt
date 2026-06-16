package prasad.vennam.moneypilot.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import prasad.vennam.moneypilot.data.entity.TransactionType

@Serializable
sealed interface Destination : NavKey {
    @Serializable
    data class Auth(
        val skipSplash: Boolean = false,
    ) : Destination

    @Serializable
    data object History : Destination

    @Serializable
    data class AddEditTransaction(
        val transactionId: Long? = null,
        val initialType: TransactionType = TransactionType.EXPENSE,
    ) : Destination

    @Serializable
    data object Dashboard : Destination

    @Serializable
    data object Insights : Destination

    @Serializable
    data object AiChat : Destination

    @Serializable
    data object Investments : Destination

    @Serializable
    data object Loans : Destination

    @Serializable
    data object Reports : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data object ManageCategories : Destination

    @Serializable
    data object ReceiptScanner : Destination

    @Serializable
    data object Notifications : Destination

    @Serializable
    data object FAQ : Destination

    @Serializable
    data object EmergencyFund : Destination

    @Serializable
    data object TermsOfService : Destination

    @Serializable
    data object PrivacyPolicy : Destination

    @Serializable
    data object FinancialNews : Destination

    @Serializable
    data class NewsWebFrame(
        val url: String,
        val title: String,
    ) : Destination

    @Serializable
    data object FinancialSandbox : Destination
}
