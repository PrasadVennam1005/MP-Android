package prasad.vennam.moneypilot.feature.cosplit.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CoSplitExpense(
    val id: String = "",
    val groupId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "",
    val splitType: String = "EQUAL",
    val splitDetails: Map<String, Double> = emptyMap(),
    val createdById: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val date: Long = System.currentTimeMillis()
)
