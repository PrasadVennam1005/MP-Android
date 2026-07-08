package prasad.vennam.moneypilot.ui.investments.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.domain.model.SymbolResult
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import prasad.vennam.moneypilot.util.toMajorUnit

@Composable
fun InvestmentSummaryCard(
    totalValue: Double,
    gain: Double,
    percent: Double,
) {
    val currencyCode = LocalCurrencyCode.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                stringResource(R.string.total_portfolio_value),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
            )
            Text(
                CurrencyFormatter.format(totalValue, currencyCode),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Rounded.TrendingUp,
                    contentDescription = null,
                    tint = if (gain >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                val formattedGain = CurrencyFormatter.format(kotlin.math.abs(gain), currencyCode)
                val sign = if (gain >= 0) "+" else "-"
                Text(
                    text = "$sign$formattedGain (${String.format(LocalLocale.current.platformLocale, "%.1f", percent)}%)",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (gain >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableInvestmentCard(
    investment: Investment,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                scope.launch { dismissState.reset() }
            },
            title = { Text(stringResource(R.string.delete_investment_title)) },
            text = { Text(stringResource(R.string.delete_investment_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                        }
                        onDelete()
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch { dismissState.reset() }
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        onDismiss = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.EndToStart -> {
                    showDeleteDialog = true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    scope.launch { dismissState.reset() }
                }
                else -> {}
            }
        },
        backgroundContent = {
            val color =
                when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent
                }
            val alignment =
                when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
            val icon =
                when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> Icons.Rounded.Edit
                    SwipeToDismissBoxValue.EndToStart -> Icons.Rounded.Delete
                    else -> Icons.Rounded.Delete
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(color)
                        .padding(horizontal = 24.dp),
                contentAlignment = alignment,
            ) {
                Icon(icon, contentDescription = null)
            }
        },
        content = {
            InvestmentItem(investment = investment)
        },
    )
}

@Composable
fun InvestmentItem(investment: Investment) {
    val currencyCode = LocalCurrencyCode.current
    val gain = investment.currentValue.toMajorUnit - investment.investedAmount.toMajorUnit
    val gainPercentage =
        if (investment.investedAmount > 0) (gain / investment.investedAmount.toMajorUnit) * 100 else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                AssetTypeIcon(
                    type = investment.type,
                    size = 48.dp,
                    iconSize = 24.dp,
                    shape = MaterialTheme.shapes.large,
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        investment.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        getLocalizedAssetType(investment.type).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    CurrencyFormatter.format(investment.currentValue.toMajorUnit, currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${if (gain >= 0) "+" else ""}${
                        String.format(
                            LocalLocale.current.platformLocale,
                            "%.1f",
                            gainPercentage,
                        )
                    }%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (gain >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun EmptyInvestmentState() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.Analytics,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.portfolio_is_empty),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            stringResource(R.string.tap_to_add_your_first),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        )
    }
}

@Composable
fun AssetTypeIcon(
    type: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 32.dp,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
) {
    val icon =
        when (type) {
            "Stock" -> Icons.AutoMirrored.Rounded.ShowChart
            "Mutual Fund" -> Icons.Rounded.AccountBalance
            "Crypto" -> Icons.Rounded.CurrencyBitcoin
            "Real Estate" -> Icons.Rounded.HomeWork
            "Gold" -> Icons.Rounded.Savings
            "FD" -> Icons.Rounded.LockClock
            else -> Icons.Rounded.AccountBalanceWallet
        }

    val color =
        when (type) {
            "Stock" -> Color(0xFF2196F3)
            "Mutual Fund" -> Color(0xFF673AB7)
            "Crypto" -> Color(0xFFFF9800)
            "Real Estate" -> Color(0xFF795548)
            "Gold" -> Color(0xFFFFC107)
            "FD" -> Color(0xFF4CAF50)
            else -> Color.Gray
        }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = shape,
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
fun SymbolResultRow(
    result: SymbolResult,
    assetType: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AssetTypeIcon(type = assetType, size = 36.dp, iconSize = 18.dp)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.symbol,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = result.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }

        if (result.exchange.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = result.exchange,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
fun getLocalizedAssetType(type: String): String =
    when (type) {
        "Stock" -> stringResource(R.string.asset_stock)
        "Mutual Fund" -> stringResource(R.string.asset_mutual_fund)
        "Crypto" -> stringResource(R.string.asset_crypto)
        "Real Estate" -> stringResource(R.string.asset_real_estate)
        "Gold" -> stringResource(R.string.asset_gold)
        "FD" -> stringResource(R.string.asset_fd)
        else -> type
    }
