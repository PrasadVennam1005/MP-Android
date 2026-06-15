package prasad.vennam.moneypilot.ui.budget.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.ui.viewmodel.BudgetProgress
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.LocalCurrencyCode

@Composable
fun BudgetHeaderSection(
    budgetProgresses: List<BudgetProgress>,
    month: Int,
    year: Int,
) {
    val currencyCode = LocalCurrencyCode.current
    val totalBudget = remember(budgetProgresses) { budgetProgresses.sumOf { it.limit } }
    val totalSpent = remember(budgetProgresses) { budgetProgresses.sumOf { it.spent } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.primaryContainer.copy(
                        alpha = 0.3f,
                    ),
            ),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                val monthName =
                    remember(month) {
                        val symbols = java.text.DateFormatSymbols.getInstance(java.util.Locale.getDefault())
                        symbols.months.getOrNull(month) ?: ""
                    }
                Column {
                    Text(
                        stringResource(R.string.budget_overview),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (monthName.isNotEmpty()) {
                        Text(
                            text = "$monthName $year",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(stringResource(R.string.total_budget), style = MaterialTheme.typography.labelSmall)
                    Text(
                        CurrencyFormatter.format(totalBudget, currencyCode),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.total_spent), style = MaterialTheme.typography.labelSmall)
                    Text(
                        CurrencyFormatter.format(totalSpent, currencyCode),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
