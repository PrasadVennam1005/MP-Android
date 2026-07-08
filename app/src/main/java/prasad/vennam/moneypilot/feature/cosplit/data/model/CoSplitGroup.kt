package prasad.vennam.moneypilot.feature.cosplit.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CoSplitGroup(
    val id: String = "",
    val name: String = "",
    val createdById: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val members: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    val balances: Map<String, Double> = emptyMap()
)

@Serializable
data class CoSplitSettlementRoute(
    val debtorEmail: String,
    val debtorName: String,
    val creditorEmail: String,
    val creditorName: String,
    val amount: Double
) {
    val displayText: String
        get() = "$debtorName pays $creditorName ₹${String.format("%.2f", amount)}"
}
