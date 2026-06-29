package prasad.vennam.moneypilot.ui.loans.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.domain.model.PrepaymentResult
import prasad.vennam.moneypilot.util.CurrencyFormatter

@Composable
fun PrepaymentCalculator(
    prepaymentAmount: String,
    onAmountChange: (String) -> Unit,
    isMonthly: Boolean,
    onTypeToggle: (Boolean) -> Unit,
    result: PrepaymentResult?,
    currencyCode: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.prepayment_calculator),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = isMonthly,
                    onClick = { onTypeToggle(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text(stringResource(R.string.monthly)) },
                )
                SegmentedButton(
                    selected = !isMonthly,
                    onClick = { onTypeToggle(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text(stringResource(R.string.one_time)) },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = prepaymentAmount,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.prepayment_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            )

            if (result != null) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    PrepaymentMetric(
                        label = stringResource(R.string.interest_saved),
                        value = CurrencyFormatter.format(result.totalInterestSaved, currencyCode),
                        modifier = Modifier.weight(1f),
                    )
                    PrepaymentMetric(
                        label = stringResource(R.string.months_saved),
                        value = stringResource(R.string.months_saved_format, result.monthsSaved.toString()),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                PrepaymentMetric(
                    label = stringResource(R.string.new_closure_date),
                    value = result.newCompletionDate,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PrepaymentMetric(
    label: String,
    value: String,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
