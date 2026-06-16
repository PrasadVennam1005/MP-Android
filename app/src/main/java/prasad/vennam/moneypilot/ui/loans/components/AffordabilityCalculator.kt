package prasad.vennam.moneypilot.ui.loans.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.domain.model.AffordabilityResult
import prasad.vennam.moneypilot.util.CurrencyFormatter

@Composable
fun AffordabilityCalculator(
    monthlyIncome: String,
    onIncomeChange: (String) -> Unit,
    result: AffordabilityResult?,
    currencyCode: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.affordability_calculator),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = monthlyIncome,
                onValueChange = onIncomeChange,
                label = { Text(stringResource(R.string.monthly_income_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            )
            
            if (result != null && result.isAffordable) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    AffordabilityMetric(
                        label = stringResource(R.string.max_safe_loan),
                        value = CurrencyFormatter.format(result.maxLoanAmount, currencyCode),
                        modifier = Modifier.weight(1f)
                    )
                    AffordabilityMetric(
                        label = stringResource(R.string.recommended_emi),
                        value = CurrencyFormatter.format(result.recommendedEmi, currencyCode),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.infoContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.info, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recommended EMI is 30% of your monthly income.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AffordabilityMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// Add info extension to MaterialTheme
val ColorScheme.info: androidx.compose.ui.graphics.Color get() = androidx.compose.ui.graphics.Color(0xFF0288D1)
val ColorScheme.infoContainer: androidx.compose.ui.graphics.Color get() = androidx.compose.ui.graphics.Color(0xFFE1F5FE)
