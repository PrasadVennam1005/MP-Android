package prasad.vennam.moneypilot.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddChart
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    timeFrame: TimeFrame,
) {
    val currencyCode = LocalCurrencyCode.current

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

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KPICard(
                title = stringResource(R.string.todays_expense),
                amount = today,
                icon = Icons.Rounded.Today,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                currencyCode = currencyCode,
                modifier = Modifier.weight(1f),
            )
            KPICard(
                title = savingsTitle,
                amount = savings,
                icon = Icons.Rounded.AccountBalance,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                currencyCode = currencyCode,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KPICard(
                title = incomeTitle,
                amount = periodInc,
                icon = Icons.Rounded.ArrowUpward,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                currencyCode = currencyCode,
                modifier = Modifier.weight(1f),
            )
            KPICard(
                title = expenseTitle,
                amount = periodExp,
                icon = Icons.Rounded.ArrowDownward,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                currencyCode = currencyCode,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KPICard(
                title = stringResource(R.string.investments),
                amount = investment,
                icon = Icons.Rounded.Savings,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                currencyCode = currencyCode,
                modifier = Modifier.weight(1f),
            )
            KPICard(
                title = stringResource(R.string.profit_loss),
                amount = currentInvestmentValue - investment,
                icon = Icons.Rounded.AddChart,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                currencyCode = currencyCode,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun KPICard(
    title: String,
    amount: Double,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val animatedAmount by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "KPICardAmountAnimation",
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.7f),
            )
            Text(
                CurrencyFormatter.format(animatedAmount.toDouble(), currencyCode),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
            )
        }
    }
}
