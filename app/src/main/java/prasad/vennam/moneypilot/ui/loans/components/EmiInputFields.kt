package prasad.vennam.moneypilot.ui.loans.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LoanAmountInput(
    amountInput: String,
    onAmountChange: (String) -> Unit,
    currencySymbol: String,
    minAmount: Float,
    maxAmount: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.loan_amount).uppercase(LocalLocale.current.platformLocale),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            InputBox(
                value = amountInput,
                onValueChange = { onAmountChange(it.filter { char -> char.isDigit() }) },
                prefix = currencySymbol,
                width = 120.dp,
                keyboardType = KeyboardType.Number
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        val principal = amountInput.toDoubleOrNull() ?: 0.0
        val sliderAmount = principal.toFloat().coerceIn(minAmount, maxAmount)
        Slider(
            value = sliderAmount,
            onValueChange = { onAmountChange(it.roundToInt().toString()) },
            valueRange = minAmount..maxAmount,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
        )
    }
}

@Composable
fun InterestRateInput(
    rateInput: String,
    onRateChange: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.interest_rate_percent).uppercase(LocalLocale.current.platformLocale),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            InputBox(
                value = rateInput,
                onValueChange = onRateChange,
                suffix = "%",
                width = 80.dp,
                keyboardType = KeyboardType.Decimal
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        val interestRate = rateInput.toDoubleOrNull() ?: 0.0
        val sliderRate = interestRate.toFloat().coerceIn(1f, 25f)
        Slider(
            value = sliderRate,
            onValueChange = { onRateChange(String.format(Locale.US, "%.1f", it)) },
            valueRange = 1f..25f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenureInput(
    tenureInput: String,
    onTenureChange: (String) -> Unit,
    isYears: Boolean,
    onUnitToggle: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tenure).uppercase(LocalLocale.current.platformLocale),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.width(140.dp)) {
                    SegmentedButton(
                        selected = isYears,
                        onClick = { onUnitToggle(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("Yr", style = MaterialTheme.typography.labelSmall) }
                    )
                    SegmentedButton(
                        selected = !isYears,
                        onClick = { onUnitToggle(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("Mo", style = MaterialTheme.typography.labelSmall) }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                InputBox(
                    value = tenureInput,
                    onValueChange = { onTenureChange(it.filter { char -> char.isDigit() }) },
                    width = 60.dp,
                    keyboardType = KeyboardType.Number
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        val tenure = tenureInput.toIntOrNull() ?: 0
        val maxTenure = if (isYears) 30f else 360f
        val minTenure = 1f
        val sliderTenure = tenure.toFloat().coerceIn(minTenure, maxTenure)
        Slider(
            value = sliderTenure,
            onValueChange = { onTenureChange(it.roundToInt().toString()) },
            valueRange = minTenure..maxTenure,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
        )
    }
}

@Composable
private fun InputBox(
    value: String,
    onValueChange: (String) -> Unit,
    prefix: String? = null,
    suffix: String? = null,
    width: androidx.compose.ui.unit.Dp,
    keyboardType: KeyboardType
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(44.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp),
    ) {
        if (prefix != null) {
            Text(
                text = prefix,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            modifier = Modifier.width(width),
        )
        if (suffix != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = suffix,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
