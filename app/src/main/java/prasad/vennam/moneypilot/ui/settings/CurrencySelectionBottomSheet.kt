package prasad.vennam.moneypilot.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import prasad.vennam.moneypilot.R

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String,
)

val currencies =
    listOf(
        CurrencyInfo("INR", "Indian Rupee", "₹", "🇮🇳"),
        CurrencyInfo("USD", "US Dollar", "$", "🇺🇸"),
        CurrencyInfo("EUR", "Euro", "€", "🇪🇺"),
        CurrencyInfo("GBP", "British Pound", "£", "🇬🇧"),
        CurrencyInfo("JPY", "Japanese Yen", "¥", "🇯🇵"),
        CurrencyInfo("AUD", "Australian Dollar", "A$", "🇦🇺"),
        CurrencyInfo("CAD", "Canadian Dollar", "C$", "🇨🇦"),
        CurrencyInfo("AED", "UAE Dirham", "د.إ", "🇦🇪"),
        CurrencyInfo("SAR", "Saudi Riyal", "﷼", "🇸🇦"),
        CurrencyInfo("SGD", "Singapore Dollar", "S$", "🇸🇬"),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectionBottomSheet(
    selectedCurrencyCode: String,
    onCurrencySelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
        ) {
            // Header
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.select_currency),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier =
                        Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
                ) {
                    Icon(Icons.Rounded.Close, null, modifier = Modifier.size(18.dp))
                }
            }

            var searchQuery by remember { mutableStateOf("") }
            val keyboardController = LocalSoftwareKeyboardController.current

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_currency)) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            )

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            val filteredCurrencies =
                remember(searchQuery) {
                    if (searchQuery.isBlank()) {
                        currencies
                    } else {
                        currencies.filter {
                            it.code.contains(searchQuery, ignoreCase = true) ||
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                it.symbol.contains(searchQuery, ignoreCase = true)
                        }
                    }
                }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filteredCurrencies, key = { it.code }) { currency ->
                    val isSelected = currency.code == selectedCurrencyCode
                    Surface(
                        onClick = {
                            onCurrencySelect(currency.code)
                            onDismiss()
                        },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = currency.flag, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${currency.name} (${currency.symbol})",
                                    style =
                                        MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        ),
                                )
                                Text(
                                    text = currency.code,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = stringResource(R.string.save),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
