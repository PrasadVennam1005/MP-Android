package prasad.vennam.moneypilot.ui.dashboard.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FinancialHealthCard(
    savingsRate: Double,
    totalDebt: Double,
    periodIncome: Double,
    totalInvestment: Double,
    hasEmergencyFund: Boolean,
    budgetExceededCount: Int,
    isPremium: Boolean,
    onNavigateToPremium: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val score = remember(savingsRate, totalDebt, periodIncome, totalInvestment, hasEmergencyFund, budgetExceededCount) {
        calculateFinancialHealthScore(
            savingsRate = savingsRate,
            totalDebt = totalDebt,
            periodIncome = periodIncome,
            totalInvestment = totalInvestment,
            hasEmergencyFund = hasEmergencyFund,
            budgetExceededCount = budgetExceededCount
        )
    }

    val (ratingText, ratingColor, ratingDesc) = remember(score) {
        when {
            score >= 80 -> Triple(
                "Excellent",
                Color(0xFF4CAF50),
                "Outstanding financial discipline! You save regularly, keep debts low, and invest."
            )
            score >= 50 -> Triple(
                "Good",
                Color(0xFFFF9800),
                "Stable financial habits. Try building a stronger investment profile or cutting budget leakages."
            )
            else -> Triple(
                "Needs Attention",
                Color(0xFFF44336),
                "Let's get back on track. High debt, budget overshoots, or low savings need correction."
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Financial Health Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isPremium) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PREMIUM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Gauge + Score Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                HealthGauge(score = score)

                Spacer(modifier = Modifier.width(24.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ratingColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ratingText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = ratingColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ratingDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Audit Button
            if (isPremium) {
                Button(
                    onClick = onNavigateToAiChat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Get Detailed AI Audit Report",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            } else {
                Button(
                    onClick = onNavigateToPremium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                )
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Run AI Financial Audit ⚡ (PRO)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthGauge(score: Int, modifier: Modifier = Modifier) {
    val animatedScore = animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "scoreAnimation"
    )

    Box(
        modifier = modifier.size(90.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val size = this.size
            val radius = (size.minDimension - strokeWidth) / 2

            // Draw background gray track
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            // Determine gauge color based on score
            val color = when {
                score >= 80 -> Color(0xFF4CAF50) // Green
                score >= 50 -> Color(0xFFFF9800) // Orange
                else -> Color(0xFFF44336) // Red
            }

            // Draw progress arc
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = (animatedScore.value / 100f) * 270f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "/100",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun calculateFinancialHealthScore(
    savingsRate: Double,
    totalDebt: Double,
    periodIncome: Double,
    totalInvestment: Double,
    hasEmergencyFund: Boolean,
    budgetExceededCount: Int,
): Int {
    var score = 15 // base/default score

    // 1. Savings Rate contribution (Max 30)
    score += when {
        savingsRate >= 0.30 -> 30
        savingsRate >= 0.15 -> 20
        savingsRate >= 0.0 -> 10
        else -> 0
    }

    // 2. Budget compliance contribution (Max 25)
    score += when (budgetExceededCount) {
        0 -> 25
        1 -> 15
        2 -> 5
        else -> 0
    }

    // 3. Debt to Income level (Max 20)
    score += when {
        totalDebt <= 0 -> 20
        periodIncome > 0 && totalDebt < periodIncome * 3 -> 15
        else -> 5
    }

    // 4. Investments and Emergency Prep (Max 10)
    if (totalInvestment > 0) score += 5
    if (hasEmergencyFund) score += 5

    return score.coerceIn(0, 100)
}
