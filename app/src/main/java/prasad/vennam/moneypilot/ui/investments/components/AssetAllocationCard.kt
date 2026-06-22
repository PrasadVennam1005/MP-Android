package prasad.vennam.moneypilot.ui.investments.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.ui.viewmodel.state.AllocationDetail
import prasad.vennam.moneypilot.ui.viewmodel.state.AllocationProfile
import prasad.vennam.moneypilot.util.CurrencyFormatter
import kotlin.math.abs

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AssetAllocationCard(
    allocationDetails: List<AllocationDetail>,
    selectedProfile: AllocationProfile,
    onProfileSelected: (AllocationProfile) -> Unit,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val totalCurrent =
        remember(allocationDetails) {
            allocationDetails.sumOf { it.currentAmount }
        }

    // Donut chart sweep animation
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(allocationDetails, selectedProfile) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        )
    }

    val density = LocalDensity.current
    val stroke = remember(density) {
        Stroke(width = with(density) { 14.dp.toPx() }, cap = StrokeCap.Round)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header: Title & Profile Selector
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Asset Allocation Advisor",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Chips Selector
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AllocationProfile.values().forEach { profile ->
                        val isSelected = selectedProfile == profile
                        FilterChip(
                            selected = isSelected,
                            onClick = { onProfileSelected(profile) },
                            label = { Text(profile.label) },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            shape = MaterialTheme.shapes.large,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (totalCurrent <= 0.0) {
                // Empty state or zero assets
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Text(
                            text = "Add investments to see your asset allocation breakdown.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                // Donut Chart + Legend Row
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Donut Canvas
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(140.dp),
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var currentAngle = -90f

                            allocationDetails.forEach { detail ->
                                val sweep = (detail.currentPercent.toFloat() / 100f) * 360f * animatedProgress.value
                                if (sweep > 0f) {
                                    val gap = 2f
                                    val adjustedSweep = if (sweep > gap) sweep - gap else sweep
                                    drawArc(
                                        color = getAssetColor(detail.assetType),
                                        startAngle = currentAngle + (gap / 2f),
                                        sweepAngle = adjustedSweep,
                                        useCenter = false,
                                        style = stroke,
                                    )
                                }
                                currentAngle += (detail.currentPercent.toFloat() / 100f) * 360f
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "Assets",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = CurrencyFormatter.format(totalCurrent, currencyCode),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    // Legend Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        allocationDetails.forEach { detail ->
                            LegendRowItem(detail = detail)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Advisor Recommendations
                AdvisorSection(
                    allocationDetails = allocationDetails,
                    profileLabel = selectedProfile.label,
                    currencyCode = currencyCode,
                )
            }
        }
    }
}

@Composable
private fun LegendRowItem(
    detail: AllocationDetail,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(getAssetColor(detail.assetType)),
        )
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = detail.assetType,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${detail.currentPercent.toInt()}%",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "Target: ${detail.targetPercent.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AdvisorSection(
    allocationDetails: List<AllocationDetail>,
    profileLabel: String,
    currencyCode: String,
) {
    // Collect active items where deviation is greater than 2%
    val deviations =
        remember(allocationDetails) {
            allocationDetails
                .filter { abs(it.differencePercent) >= 2.0 }
                .sortedByDescending { abs(it.differencePercent) }
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (deviations.isEmpty()) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (deviations.isEmpty()) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                    contentDescription = null,
                    tint = if (deviations.isEmpty()) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = if (deviations.isEmpty()) "Optimal Alignment" else "Rebalancing Actions",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (deviations.isEmpty()) {
                Text(
                    text = "Your portfolio matches the $profileLabel risk allocation. Great job!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    deviations.take(3).forEach { detail ->
                        val isUnderweight = detail.differencePercent > 0.0
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = if (isUnderweight) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                                contentDescription = null,
                                tint = if (isUnderweight) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier =
                                    Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp),
                            )
                            Text(
                                text =
                                    buildString {
                                        if (isUnderweight) {
                                            append("Add ")
                                            append(CurrencyFormatter.format(detail.differenceAmount, currencyCode))
                                            append(" to ")
                                            append(detail.assetType)
                                            append(" to meet your ")
                                            append(detail.targetPercent.toInt())
                                            append("% target.")
                                        } else {
                                            append("Reduce ")
                                            append(detail.assetType)
                                            append(" by ")
                                            append(CurrencyFormatter.format(abs(detail.differenceAmount), currencyCode))
                                            append(" to return to your ")
                                            append(detail.targetPercent.toInt())
                                            append("% limit.")
                                        }
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getAssetColor(type: String): Color =
    when (type) {
        "Mutual Fund" -> Color(0xFF2563EB) // Primary Blue
        "Stock" -> Color(0xFF8B5CF6) // Tertiary Violet
        "FD" -> Color(0xFF10B981) // Secondary Green
        "Crypto" -> Color(0xFFEF4444) // Red
        "Gold" -> Color(0xFFF59E0B) // Amber
        "Real Estate" -> Color(0xFFEC4899) // Pink
        else -> Color(0xFF64748B) // Slate/Grey
    }
