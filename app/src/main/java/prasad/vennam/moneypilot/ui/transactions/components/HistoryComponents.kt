package prasad.vennam.moneypilot.ui.transactions.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.ui.budget.utils.getCategoryIcon
import prasad.vennam.moneypilot.ui.theme.MoneyPilotTheme
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import prasad.vennam.moneypilot.util.toMajorUnit
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun FintechTransactionCard(
    transaction: Transaction,
    category: Category?,
    onClick: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val dateFormatter = remember(locale) { SimpleDateFormat("dd MMM, yyyy", locale) }
    val currencyCode = LocalCurrencyCode.current

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = if (category != null) Color(category.color).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = remember(category?.iconName) {
                        getCategoryIcon(category?.iconName)
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (category != null) Color(category.color) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note.ifBlank { category?.name ?: stringResource(R.string.transaction) },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${category?.name ?: stringResource(R.string.general)} • ${dateFormatter.format(Date(transaction.timestamp))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (transaction.paymentMode.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            transaction.paymentMode,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val formattedAmount = CurrencyFormatter.format(transaction.amount.toMajorUnit, currencyCode)
                val sign = if (transaction.type == TransactionType.INCOME) "+" else "-"
                Text(
                    text = "$sign$formattedAmount",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (transaction.type == TransactionType.INCOME) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }
    }
}

@Composable
fun EmptyState(isFiltering: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            if (isFiltering) Icons.Rounded.SearchOff else Icons.Rounded.History,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (isFiltering) stringResource(R.string.no_results_found) else stringResource(R.string.no_transactions_yet),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FintechTransactionCardPreview() {
    MoneyPilotTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FintechTransactionCard(
                transaction = Transaction(
                    id = 1L,
                    amount = 4500L,
                    type = TransactionType.EXPENSE,
                    categoryId = 1L,
                    timestamp = System.currentTimeMillis(),
                    note = "Weekly organic groceries",
                    paymentMode = "UPI"
                ),
                category = Category(
                    id = 1L,
                    name = "Groceries",
                    iconName = "shopping_cart",
                    color = 0xFFE91E63,
                    isExpense = true
                ),
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    MoneyPilotTheme {
        EmptyState(isFiltering = false)
    }
}
