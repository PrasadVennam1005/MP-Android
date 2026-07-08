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
