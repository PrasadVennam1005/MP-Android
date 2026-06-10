package prasad.vennam.moneypilot.ui.ai

import androidx.compose.ui.graphics.vector.ImageVector

data class SmartInsight(
    val id: String,
    val title: String,
    val description: String,
    val type: InsightType,
    val status: InsightStatus,
    val value: String,
    val percentageChange: Float? = null,
    val recommendation: String? = null,
    val icon: ImageVector? = null,
)

enum class InsightType {
    SPENDING,
    BUDGET,
    SAVINGS,
}

enum class InsightStatus {
    POSITIVE,
    NEGATIVE,
    WARNING,
    NEUTRAL,
}
