package prasad.vennam.moneypilot.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String
)

val currencies = listOf(
    CurrencyInfo("INR", "Indian Rupee", "₹", "🇮🇳"),
    CurrencyInfo("USD", "US Dollar", "$", "🇺🇸"),
    CurrencyInfo("EUR", "Euro", "€", "🇪🇺"),
    CurrencyInfo("GBP", "British Pound", "£", "🇬🇧"),
    CurrencyInfo("JPY", "Japanese Yen", "¥", "🇯🇵"),
    CurrencyInfo("AUD", "Australian Dollar", "A$", "🇦🇺"),
    CurrencyInfo("CAD", "Canadian Dollar", "C$", "🇨🇦"),
    CurrencyInfo("AED", "UAE Dirham", "د.إ", "🇦🇪"),
    CurrencyInfo("SAR", "Saudi Riyal", "﷼", "🇸🇦"),
    CurrencyInfo("SGD", "Singapore Dollar", "S$", "🇸🇬")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectionBottomSheet(
    selectedCurrencyCode: String,
    onCurrencySelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Select Currency",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Rounded.Close, null, modifier = Modifier.size(18.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(currencies) { currency ->
                    val isSelected = currency.code == selectedCurrencyCode
                    Surface(
                        onClick = {
                            onCurrencySelect(currency.code)
                            onDismiss()
                        },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = currency.flag, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${currency.name} (${currency.symbol})",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                )
                                Text(
                                    text = currency.code,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
