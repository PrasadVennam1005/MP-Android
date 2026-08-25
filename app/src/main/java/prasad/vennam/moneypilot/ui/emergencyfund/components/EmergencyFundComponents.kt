package prasad.vennam.moneypilot.ui.emergencyfund.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.ui.theme.MoneyPilotTheme

@Composable
fun EmergencyFundHeaderSection(
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.emergency_fund),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.safety_net_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onInfoClick() }
                .padding(vertical = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(height = 12.dp, width = 6.dp)
                    .background(
                        MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.what_should_i_add_here),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                ),
            )
        }
    }
}

@Composable
fun EmptyEmergencyFundCard(
    onSetUpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.extraLarge,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.build_safety_net),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.safety_net_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSetUpClick,
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
        ) {
            Text(
                text = stringResource(R.string.get_started),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
fun EmergencyFundGaugeCard(
    percentAchieved: Float,
    coverageMonths: Double,
    targetMonths: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp),
            ) {
                val animatedPercent by animateFloatAsState(
                    targetValue = percentAchieved,
                    animationSpec = tween(durationMillis = 1000),
                    label = "GaugeAnimation"
                )

                val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                val sweepColor = MaterialTheme.colorScheme.secondary

                val density = LocalDensity.current
                val gaugeStroke = remember(density) {
                    Stroke(width = with(density) { 16.dp.toPx() }, cap = StrokeCap.Round)
                }
                val sweepGradientBrush = remember(sweepColor) {
                    Brush.sweepGradient(
                        listOf(
                            sweepColor.copy(alpha = 0.6f),
                            sweepColor,
                        ),
                    )
                }

                Canvas(modifier = Modifier.size(170.dp)) {
                    drawCircle(
                        color = trackColor,
                        style = gaugeStroke,
                    )
                    drawArc(
                        brush = sweepGradientBrush,
                        startAngle = -90f,
                        sweepAngle = (animatedPercent / 100f) * 360f,
                        useCenter = false,
                        style = gaugeStroke,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${percentAchieved.toInt()}%",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp,
                        ),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.current_progress),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(
                    R.string.safety_net_achieved,
                    coverageMonths,
                ),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(
                    R.string.safety_net_months_target,
                    targetMonths,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun EmergencyFundDetailStatsRow(
    currentSavedFormatted: String,
    targetGoalFormatted: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.saved_amount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentSavedFormatted,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.target_amount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = targetGoalFormatted,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun EmergencyFundRemainingStatusCard(
    remainingToSave: Double,
    remainingToSaveFormatted: String,
    modifier: Modifier = Modifier
) {
    if (remainingToSave > 0) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.remaining_target),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = remainingToSaveFormatted,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Safety Net fully funded! Amazing job.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                )
            }
        }
    }
}

@Composable
fun EmergencyFundActionRow(
    onWithdrawClick: () -> Unit,
    onDepositClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onWithdrawClick,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
            ),
        ) {
            Text(
                text = stringResource(R.string.withdraw),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            onClick = onDepositClick,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
            Text(
                text = stringResource(R.string.deposit),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmergencyFundGaugeCardPreview() {
    MoneyPilotTheme {
        EmergencyFundGaugeCard(
            percentAchieved = 65f,
            coverageMonths = 3.9,
            targetMonths = 6
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyEmergencyFundCardPreview() {
    MoneyPilotTheme {
        EmptyEmergencyFundCard(onSetUpClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun EmergencyFundDetailStatsRowPreview() {
    MoneyPilotTheme {
        EmergencyFundDetailStatsRow(
            currentSavedFormatted = "$3,900.00",
            targetGoalFormatted = "$6,000.00"
        )
    }
}
