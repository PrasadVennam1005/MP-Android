package prasad.vennam.moneypilot.ui.viewmodel.state

data class InvestmentSummary(
    val totalInvested: Double = 0.0,
    val totalCurrent: Double = 0.0,
)

sealed class AutoFillState {
    object Idle : AutoFillState()

    object Loading : AutoFillState()

    data class Success(
        val quantity: Double,
        val priceUsed: Double,
    ) : AutoFillState()

    object Error : AutoFillState()
}

enum class AllocationProfile(
    val label: String,
) {
    BALANCED("Balanced"),
    AGGRESSIVE("Aggressive"),
    CONSERVATIVE("Conservative"),
}

data class AllocationDetail(
    val assetType: String,
    val currentAmount: Double,
    val currentPercent: Double,
    val targetPercent: Double,
    val differenceAmount: Double,
    val differencePercent: Double,
)
