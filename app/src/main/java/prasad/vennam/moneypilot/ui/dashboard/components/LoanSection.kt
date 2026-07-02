package prasad.vennam.moneypilot.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import prasad.vennam.moneypilot.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.toMajorUnit
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun LoanSection(
    loans: List<Loan>,
    currencyCode: String,
    onViewAll: () -> Unit,
) {
    if (loans.isEmpty()) return

    Column {
        SectionHeader(
            title = "Track My Loans",
            onActionClick = onViewAll,
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(loans, key = { it.id }) { loan ->
                LoanCard(loan, currencyCode)
            }
        }
    }
}

@Composable
fun LoanCard(
    loan: Loan,
    currencyCode: String,
) {
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", LocalConfiguration.current.locales[0])

    Card(
        modifier =
            Modifier
                .width(280.dp)
                .padding(vertical = 4.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = loan.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.next_emi),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = CurrencyFormatter.format(loan.emiAmount.toMajorUnit, currencyCode),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = dateFormatter.format(Date(loan.nextEmiDate)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text(
                        text = stringResource(R.string.total_outstanding),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = CurrencyFormatter.format(loan.outstandingAmount.toMajorUnit, currencyCode),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (loan.totalAmount > 0) {
                                (1f - (loan.outstandingAmount.toFloat() / loan.totalAmount.toFloat())).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                        },
                        modifier = Modifier.width(80.dp).padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}
