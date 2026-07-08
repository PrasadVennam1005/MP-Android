package prasad.vennam.moneypilot.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddChart
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.MoneyOff
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.TimeFrame
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.LocalCurrencyCode

@Composable
fun KPISection(
    today: Double,
    periodExp: Double,
    periodInc: Double,
    savings: Double,
    investment: Double,
    currentInvestmentValue: Double,
    savingsRate: Double,
    totalDebt: Double,
    timeFrame: TimeFrame,
) {
    val currencyCode = LocalCurrencyCode.current
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val incomeTitle =
        when (timeFrame) {
            TimeFrame.MONTHLY -> stringResource(R.string.monthly_income)
            TimeFrame.QUARTERLY -> stringResource(R.string.quarterly_income)
            TimeFrame.YEARLY -> stringResource(R.string.yearly_income)
        }

    val expenseTitle =
        when (timeFrame) {
            TimeFrame.MONTHLY -> stringResource(R.string.monthly_expense)
            TimeFrame.QUARTERLY -> stringResource(R.string.quarterly_expense)
            TimeFrame.YEARLY -> stringResource(R.string.yearly_expense)
        }

    val savingsTitle =
        when (timeFrame) {
            TimeFrame.MONTHLY -> stringResource(R.string.savings)
            TimeFrame.QUARTERLY -> stringResource(R.string.quarterly_savings)
            TimeFrame.YEARLY -> stringResource(R.string.yearly_savings)
        }

    val profitLoss = currentInvestmentValue - investment
    val isProfit = profitLoss >= 0

    val kpis = listOf(
        KPIData(
            title = incomeTitle,
            amount = periodInc,
            icon = Icons.Rounded.ArrowUpward,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        KPIData(
            title = expenseTitle,
            amount = periodExp,
            icon = Icons.Rounded.ArrowDownward,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        KPIData(
            title = savingsTitle,
            amount = savings,
            icon = Icons.Rounded.AccountBalance,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        KPIData(
            title = stringResource(R.string.savings_rate),
            amount = null,
            icon = Icons.Rounded.Analytics,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            value = stringResource(R.string.savings_rate_value, savingsRate)
        ),
        KPIData(
            title = stringResource(R.string.investments),
            amount = investment,
            icon = Icons.Rounded.Savings,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        KPIData(
            title = stringResource(R.string.profit_loss),
            amount = profitLoss,
            icon = Icons.Rounded.AddChart,
            containerColor = if (isProfit) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
            contentColor = if (isProfit) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
        ),
        KPIData(
            title = stringResource(R.string.todays_expense),
            amount = today,
            icon = Icons.Rounded.Today,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        KPIData(
            title = stringResource(R.string.total_debt),
            amount = totalDebt,
            icon = Icons.Rounded.MoneyOff,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    )

    val columns = if (isExpanded) 4 else 2
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        kpis.chunked(columns).forEach { rowKpis ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                rowKpis.forEach { kpi ->
                    KPICard(
                        title = kpi.title,
                        amount = kpi.amount,
                        value = kpi.value,
                        icon = kpi.icon,
                        containerColor = kpi.containerColor,
                        contentColor = kpi.contentColor,
                        currencyCode = currencyCode,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Fill empty slots if any (though not expected with current data)
                if (rowKpis.size < columns) {
                    repeat(columns - rowKpis.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

data class KPIData(
    val title: String,
    val amount: Double?,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val value: String? = null,
)

@Composable
fun KPICard(
    title: String,
    amount: Double? = null,
    value: String? = null,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    currencyCode: String = "",
    modifier: Modifier = Modifier,
) {
    val displayValue = when {
        value != null -> value
        amount != null -> CurrencyFormatter.format(amount, currencyCode)
        else -> ""
    }

    Card(
        modifier = modifier.height(130.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = MaterialTheme.shapes.extraLarge,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            contentColor.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        color = contentColor.copy(alpha = 0.1f),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, contentColor.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = contentColor.copy(alpha = 0.8f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayValue,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        ),
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
