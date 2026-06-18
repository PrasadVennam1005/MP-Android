package prasad.vennam.moneypilot.ui.ai

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.ui.budget.utils.getCategoryIcon
import prasad.vennam.moneypilot.ui.dashboard.SyncState
import prasad.vennam.moneypilot.ui.viewmodel.AiRecommendationState
import prasad.vennam.moneypilot.ui.viewmodel.AnalyticsState
import prasad.vennam.moneypilot.ui.viewmodel.AnalyticsViewModel
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    userData: UserPreferences.UserData?,
    syncState: SyncState?,
    onProfileClick: () -> Unit,
    onBackClick: () -> Unit,
    onNavigateToAiChat: () -> Unit = {},
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val aiRecState by viewModel.aiRecommendation.collectAsState()
    val currencyCode = LocalCurrencyCode.current

    val insights =
        remember(uiState, currencyCode) {
            if (uiState.isLoading) emptyList() else generateSmartInsights(uiState, currencyCode)
        }

    LaunchedEffect(uiState.isLoading, currencyCode) {
        if (!uiState.isLoading) {
            viewModel.generateAiRecommendation(currencyCode)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.smart_insights),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = Color.Unspecified,
                        navigationIconContentColor = Color.Unspecified,
                        titleContentColor = Color.Unspecified,
                        actionIconContentColor = Color.Unspecified,
                    ),
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    FinancialHealthCard(uiState, currencyCode)
                }

                item {
                    AiRecommendationCard(aiRecState, onNavigateToAiChat)
                }

                if (insights.isEmpty()) {
                    item {
                        EmptyInsightsState(onNavigateToAiChat)
                    }
                } else {
                    items(insights, key = { it.id }) { insight ->
                        InsightCard(insight)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun FinancialHealthCard(
    state: AnalyticsState,
    currencyCode: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                                ),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.financial_health_summary),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        stringResource(R.string.auto_generated_local_records),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                KPIItem(
                    label = stringResource(R.string.net_savings),
                    value = CurrencyFormatter.format(state.netSavings, currencyCode),
                    color = if (state.netSavings >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(
                    modifier =
                        Modifier
                            .height(44.dp)
                            .padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                )
                KPIItem(
                    label = stringResource(R.string.savings_rate),
                    value = "${String.format("%.1f", state.savingsRate)}%",
                    color =
                        when {
                            state.savingsRate >= 30.0 -> Color(0xFF4CAF50)
                            state.savingsRate < 10.0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val progress = (state.savingsRate / 100.0).coerceIn(0.0, 1.0).toFloat()
            val progressColor =
                when {
                    state.savingsRate >= 30.0 -> Color(0xFF4CAF50)
                    state.savingsRate < 10.0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(R.string.savings_progress),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.savings_target_20),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                )
            }
        }
    }
}

@Composable
fun KPIItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color,
        )
    }
}

@Composable
fun InsightCard(insight: SmartInsight) {
    val statusColor =
        when (insight.status) {
            InsightStatus.POSITIVE -> Color(0xFF4CAF50)
            InsightStatus.NEGATIVE -> MaterialTheme.colorScheme.error
            InsightStatus.WARNING -> Color(0xFFFF9800)
            InsightStatus.NEUTRAL -> MaterialTheme.colorScheme.primary
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(statusColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = insight.icon ?: Icons.Rounded.Info,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (insight.percentageChange != null) {
                    TrendIndicator(insight.percentageChange, insight.status)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = insight.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = statusColor,
            )

            if (insight.recommendation != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
                        ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = insight.recommendation,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrendIndicator(
    percentage: Float,
    status: InsightStatus,
) {
    val isPositive = percentage > 0
    val color =
        when (status) {
            InsightStatus.POSITIVE -> Color(0xFF4CAF50)
            InsightStatus.NEGATIVE -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isPositive) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${if (isPositive) "+" else ""}${percentage.toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
fun EmptyInsightsState(onNavigateToAiChat: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.not_enough_data_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.not_enough_data_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onNavigateToAiChat,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.talk_to_copilot_btn), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AiRecommendationCard(
    state: AiRecommendationState,
    onNavigateToAiChat: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = state is AiRecommendationState.Error) {
                    if (state is AiRecommendationState.Error) {
                        onNavigateToAiChat()
                    }
                },
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f),
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primary),
                                ),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource(R.string.ai_smart_tip),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (state) {
                is AiRecommendationState.Idle, is AiRecommendationState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.tertiary,
                            strokeWidth = 2.5.dp,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.analyzing_data_llm),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is AiRecommendationState.Success -> {
                    Text(
                        text = state.text,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                is AiRecommendationState.Error -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.tap_to_download_model),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }
    }
}

private data class InsightTheme(
    val status: InsightStatus,
    val description: String,
    val recommendation: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private fun generateSmartInsights(
    state: AnalyticsState,
    currencyCode: String,
): List<SmartInsight> {
    val list = mutableListOf<SmartInsight>()

    // 1. Savings Rate Insight
    if (state.totalIncome > 0.0) {
        val rate = state.savingsRate
        val theme =
            when {
                rate >= 30.0 ->
                    InsightTheme(
                        InsightStatus.POSITIVE,
                        "Excellent savings rate! You saved ${String.format("%.1f", rate)}% of your income this period.",
                        "You are on a strong track. Consider moving some savings to high-yield investments.",
                        Icons.Rounded.Savings,
                    )
                rate < 10.0 ->
                    InsightTheme(
                        InsightStatus.NEGATIVE,
                        "Your savings rate is ${String.format("%.1f", rate)}% this period, which is below the recommended 20%.",
                        "Try cutting down on non-essential categories like dining out or shopping.",
                        Icons.Rounded.Warning,
                    )
                else ->
                    InsightTheme(
                        InsightStatus.POSITIVE,
                        "Healthy savings rate! You saved ${String.format("%.1f", rate)}% of your income.",
                        "Keep maintaining this habit. Consistency is key to long-term wealth building.",
                        Icons.Rounded.Savings,
                    )
            }
        list.add(
            SmartInsight(
                id = "savings_rate",
                title = "Savings Rate",
                description = theme.description,
                type = InsightType.SAVINGS,
                status = theme.status,
                value = CurrencyFormatter.format(state.netSavings, currencyCode),
                percentageChange = rate.toFloat(),
                recommendation = theme.recommendation,
                icon = theme.icon,
            ),
        )
    } else if (state.totalExpense > 0.0) {
        list.add(
            SmartInsight(
                id = "no_income",
                title = "Savings Rate Alert",
                description = "You have logged expenses this month but no income yet. Savings rate cannot be calculated.",
                type = InsightType.SAVINGS,
                status = InsightStatus.WARNING,
                value = "-" + CurrencyFormatter.format(state.totalExpense, currencyCode),
                recommendation = "Make sure to log all income sources to get an accurate savings rate analysis.",
                icon = Icons.Rounded.Warning,
            ),
        )
    }

    // 2. Highest Category Spending Insight
    if (state.spendingByCategory.isNotEmpty()) {
        val topCategoryEntry = state.spendingByCategory.maxByOrNull { it.value }
        if (topCategoryEntry != null) {
            val category = topCategoryEntry.key
            val amount = topCategoryEntry.value
            val totalExpense = state.totalExpense
            val pct = if (totalExpense > 0.0) (amount / totalExpense) * 100.0 else 0.0

            val catName = category?.name ?: "Other"
            val catIcon = getCategoryIcon(category?.iconName)

            list.add(
                SmartInsight(
                    id = "top_category",
                    title = "Top Spend: $catName",
                    description = "Your largest spending this period was in $catName, which represents ${String.format(
                        "%.1f",
                        pct,
                    )}% of all expenses.",
                    type = InsightType.SPENDING,
                    status = if (pct > 30.0) InsightStatus.WARNING else InsightStatus.NEUTRAL,
                    value = CurrencyFormatter.format(amount, currencyCode),
                    percentageChange = pct.toFloat(),
                    recommendation = "Track individual transactions in $catName to find potential areas for optimization.",
                    icon = catIcon,
                ),
            )
        }
    }

    // 3. Investment Portfolio Analysis
    if (state.assetAllocations.isNotEmpty()) {
        val totalInvested = state.assetAllocations.sumOf { it.investedAmount }
        val totalCurrent = state.assetAllocations.sumOf { it.currentValue }
        val gain = totalCurrent - totalInvested
        val gainPct = if (totalInvested > 0.0) (gain / totalInvested) * 100.0 else 0.0

        val theme =
            when {
                gain > 0.0 ->
                    InsightTheme(
                        InsightStatus.POSITIVE,
                        "Your investment portfolio is in the green! Total return of ${String.format("%.1f", gainPct)}%.",
                        "Your diversified assets are performing well. Reinvest returns if possible.",
                        Icons.AutoMirrored.Rounded.TrendingUp,
                    )
                gain < 0.0 ->
                    InsightTheme(
                        InsightStatus.NEGATIVE,
                        "Your investment portfolio is currently experiencing a paper loss of ${String.format(
                            "%.1f",
                            abs(gainPct),
                        )}% due to market fluctuation.",
                        "Market volatility is normal. Avoid panic selling and focus on long-term compound growth.",
                        Icons.AutoMirrored.Rounded.TrendingDown,
                    )
                else ->
                    InsightTheme(
                        InsightStatus.NEUTRAL,
                        "Your investment portfolio value is flat compared to your invested capital.",
                        "Regularly review your asset allocation strategy.",
                        Icons.AutoMirrored.Rounded.TrendingUp,
                    )
            }

        list.add(
            SmartInsight(
                id = "investment_performance",
                title = "Investment Returns",
                description = theme.description,
                type = InsightType.SAVINGS,
                status = theme.status,
                value = (if (gain >= 0) "+" else "") + CurrencyFormatter.format(gain, currencyCode),
                percentageChange = gainPct.toFloat(),
                recommendation = theme.recommendation,
                icon = theme.icon,
            ),
        )
    }

    // 4. Budget Overruns from Hilt ViewModel calculated insights
    state.insights.forEachIndexed { index, fi ->
        if (fi.title.startsWith("Budget Exceeded") || fi.title.startsWith("High Spending Warning")) {
            list.add(
                SmartInsight(
                    id = "budget_overrun_$index",
                    title = fi.title,
                    description = fi.description,
                    type = InsightType.BUDGET,
                    status = InsightStatus.WARNING,
                    value = "Limit Exceeded",
                    recommendation = "Review category budgets and set strict caps in the reports tab.",
                    icon = Icons.Rounded.Warning,
                ),
            )
        }
    }

    return list
}
