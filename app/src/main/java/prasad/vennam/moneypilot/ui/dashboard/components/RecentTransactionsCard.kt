package prasad.vennam.moneypilot.ui.dashboard.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import prasad.vennam.moneypilot.util.inRupees
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun RecentTransactionsCard(
    transactions: List<Transaction>,
    categories: List<Category>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            transactions.forEach {
                TransactionItem(
                    it,
                    categories.find { c -> c.id == it.categoryId },
                )
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    category: Category?,
) {
    val currencyCode = LocalCurrencyCode.current
    val locale = LocalLocale.current
    val dateFormatter =
        remember(locale) {
            SimpleDateFormat("dd MMM, hh:mm a", locale.platformLocale)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color =
                if (transaction.type ==
                    TransactionType.INCOME
                ) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (transaction.type == TransactionType.INCOME) Icons.Rounded.Add else Icons.Rounded.Remove,
                    contentDescription = stringResource(R.string.add),
                    tint =
                        if (transaction.type ==
                            TransactionType.INCOME
                        ) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.note.ifBlank { category?.name ?: stringResource(R.string.transaction) },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = dateFormatter.format(Date(transaction.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text =
                buildString {
                    append(if (transaction.type == TransactionType.INCOME) "+" else "-")
                    append(CurrencyFormatter.format(transaction.amount.inRupees, currencyCode))
                },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (transaction.type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}
