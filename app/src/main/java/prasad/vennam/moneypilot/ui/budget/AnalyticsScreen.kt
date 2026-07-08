package prasad.vennam.moneypilot.ui.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.ui.components.AdBannerView
import prasad.vennam.moneypilot.ui.components.SpendingDonutChart
import prasad.vennam.moneypilot.ui.components.TrendLineChart
import prasad.vennam.moneypilot.ui.viewmodel.AnalyticsState
import prasad.vennam.moneypilot.ui.viewmodel.AnalyticsViewModel
import prasad.vennam.moneypilot.ui.viewmodel.AssetAllocation
import prasad.vennam.moneypilot.ui.viewmodel.FinancialInsight
import prasad.vennam.moneypilot.ui.viewmodel.InsightType
import prasad.vennam.moneypilot.ui.viewmodel.InsightActionType
import prasad.vennam.moneypilot.ui.viewmodel.TimeFilter
import androidx.compose.ui.platform.LocalConfiguration
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.TrackScreen

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    analyticsHelper: AnalyticsHelper,
    isPremium: Boolean = false,
    modifier: Modifier = Modifier,
    onNavigateToTab: (Int) -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToInvestments: () -> Unit = {},
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.ANALYTICS_TAB)
    val state by viewModel.uiState.collectAsState()
    val currencyCode = prasad.vennam.moneypilot.util.LocalCurrencyCode.current

    val chartColors =
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.outline,
        )

    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        AnimatedVisibility(
            visible = !state.isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!isPremium) {
                    item {
                        AdBannerView(
                            isPremium = isPremium,
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                        )
                    }
                }
                // 1. Time Filter Row
                item {
                    TimeFilterRow(
                        selectedFilter = state.timeFilter,
                        onFilterSelected = { viewModel.setTimeFilter(it) },
                    )
                }



                // 3. Line Chart Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.income_vs_expense),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TrendLineChart(
                                points = state.trendPoints,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(220.dp),
                                incomeColor = MaterialTheme.colorScheme.secondary,
                                expenseColor = MaterialTheme.colorScheme.primary,
                                currencySymbol =
                                    java.util.Currency
                                        .getInstance(currencyCode)
                                        .getSymbol(LocalConfiguration.current.locales[0]),
                            )
                        }
                    }
                }

                // 4. Spending Breakdown Card
                if (state.spendingByCategory.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.expense_breakdown),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val sortedList =
                                        state.spendingByCategory
                                            .toList()
                                            .sortedByDescending { it.second }

                                    SpendingDonutChart(
                                        sortedSpending = sortedList.map { (it.first?.name ?: "Other") to it.second },
                                        colors = chartColors,
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clickable { onNavigateToTransactions() },
                                    )
                                    Spacer(modifier = Modifier.width(24.dp))
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        sortedList.take(4).forEachIndexed { index, pair ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onNavigateToTransactions() },
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier =
                                                            Modifier
                                                                .size(10.dp)
                                                                .background(chartColors.getOrElse(index) { Color.Gray }, CircleShape),
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = pair.first?.name ?: "Other",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                                Text(
                                                    text = CurrencyFormatter.format(pair.second, currencyCode),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Asset Allocation Card
                if (state.assetAllocations.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.portfolio_distribution),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    state.assetAllocations.forEach { alloc ->
                                        AssetAllocationRow(alloc = alloc, currencyCode = currencyCode)
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Insights Section
                if (state.insights.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.financial_insights),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    items(state.insights, key = { it.title }) { insight ->
                        InsightCard(
                            insight = insight,
                            onActionClick = { action ->
                                when (action) {
                                    InsightActionType.ADJUST_BUDGET -> onNavigateToTab(0)
                                    InsightActionType.VIEW_TRANSACTIONS -> onNavigateToTransactions()
                                    InsightActionType.ANALYZE_PORTFOLIO -> onNavigateToInvestments()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeFilterRow(
    selectedFilter: TimeFilter,
    onFilterSelected: (TimeFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TimeFilter.entries.forEach { filter ->
            val isSelected = selectedFilter == filter
            val label =
                when (filter) {
                    TimeFilter.THIS_MONTH -> stringResource(R.string.time_this_month)
                    TimeFilter.LAST_3_MONTHS -> stringResource(R.string.time_3_months)
                    TimeFilter.LAST_6_MONTHS -> stringResource(R.string.time_6_months)
                    TimeFilter.ALL_TIME -> stringResource(R.string.time_all_time)
                }

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(label) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        selectedBorderColor = Color.Transparent,
                    ),
                shape = RoundedCornerShape(50),
            )
        }
    }
}



@Composable
fun AssetAllocationRow(
    alloc: AssetAllocation,
    currencyCode: String,
) {
    val profit = alloc.currentValue - alloc.investedAmount
    val profitPct = if (alloc.investedAmount > 0) (profit / alloc.investedAmount) * 100 else 0.0
    val profitText =
        if (profit >= 0) {
            "+${String.format("%.1f", profitPct)}%"
        } else {
            "${String.format("%.1f", profitPct)}%"
        }
    val profitColor = if (profit >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = alloc.type,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.invested_amount, CurrencyFormatter.format(alloc.investedAmount, currencyCode)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(alloc.currentValue, currencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = profitText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = profitColor,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { alloc.sharePercentage },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
fun InsightCard(
    insight: FinancialInsight,
    onActionClick: (InsightActionType) -> Unit = {},
) {
    val (bgColor, contentColor) =
        when (insight.type) {
            InsightType.SUCCESS ->
                Pair(
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f),
                    MaterialTheme.colorScheme.secondary,
                )
            InsightType.WARNING ->
                Pair(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                    MaterialTheme.colorScheme.error,
                )
            InsightType.INFO ->
                Pair(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                    MaterialTheme.colorScheme.primary,
                )
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = "Insight",
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (insight.actionType != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { onActionClick(insight.actionType) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
                    ) {
                        val actionText = when (insight.actionType) {
                            InsightActionType.ADJUST_BUDGET -> "Adjust Budget"
                            InsightActionType.VIEW_TRANSACTIONS -> "View Transactions"
                            InsightActionType.ANALYZE_PORTFOLIO -> "Analyze Portfolio"
                        }
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}
