package prasad.vennam.moneypilot.ui.investments.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.ui.viewmodel.AutoFillState
import prasad.vennam.moneypilot.ui.viewmodel.InvestmentViewModel
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import prasad.vennam.moneypilot.util.inRupees
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentFormBottomSheet(
    initialInvestment: Investment? = null,
    viewModel: InvestmentViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, String?, Double?, Double?, Long?) -> Unit,
) {
    var name by remember { mutableStateOf(initialInvestment?.name ?: "") }
    var type by remember { mutableStateOf(initialInvestment?.type ?: "Stock") }

    // Tracking fields
    var symbol by remember { mutableStateOf(initialInvestment?.symbol ?: "") }
    var invested by remember {
        mutableStateOf(
            initialInvestment
                ?.investedAmount
                ?.inRupees
                ?.toString()
                ?.removeSuffix(".0") ?: "",
        )
    }
    var current by remember {
        mutableStateOf(
            initialInvestment
                ?.currentValue
                ?.inRupees
                ?.toString()
                ?.removeSuffix(".0") ?: "",
        )
    }
    var quantity by remember { mutableStateOf(initialInvestment?.quantity?.toString() ?: "") }
    var interestRate by remember { mutableStateOf(initialInvestment?.interestRate?.toString() ?: "") }
    var startDate by remember { mutableLongStateOf(initialInvestment?.startDate ?: System.currentTimeMillis()) }

    var showStartDatePicker by remember { mutableStateOf(false) }

    // Symbol search
    val symbolResults by viewModel.symbolResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    // Auto-fill quantity from historical price
    val autoFillState by viewModel.autoFillState.collectAsState()

    // When a price is fetched successfully, fill in the quantity field
    LaunchedEffect(autoFillState) {
        val s = autoFillState
        if (s is AutoFillState.Success) {
            quantity =
                String
                    .format("%.4f", s.quantity)
                    .trimEnd('0')
                    .trimEnd('.')
        }
    }

    // Clear auto-fill result whenever symbol / date / type changes
    LaunchedEffect(symbol, startDate, type) {
        viewModel.clearAutoFill()
    }

    val types = listOf("Stock", "Mutual Fund", "Crypto", "Real Estate", "Gold", "FD")
    var showTypeBottomSheet by remember { mutableStateOf(false) }

    val locale = LocalLocale.current.platformLocale
    val dateFormatter = remember(locale) { SimpleDateFormat("dd MMM, yyyy", locale) }

    val currencyCode = LocalCurrencyCode.current
    val currencySymbol = remember(currencyCode) { Currency.getInstance(currencyCode).symbol }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val investedVal = invested.toDoubleOrNull()
    val interestRateVal = interestRate.toDoubleOrNull()
    val quantityVal = quantity.toDoubleOrNull()

    val isInvestedError = invested.isNotEmpty() && (investedVal == null || investedVal <= 0.0 || investedVal > 100000000.0)
    val isQuantityError = quantity.isNotEmpty() && (quantityVal == null || quantityVal <= 0.0 || quantityVal > 100000000.0)
    val isInterestRateError = interestRate.isNotEmpty() && (interestRateVal == null || interestRateVal < 0.0 || interestRateVal > 100.0)

    val isFormValid =
        name.isNotBlank() &&
            when (type) {
                "Stock", "Mutual Fund", "Crypto", "Gold" -> {
                    symbol.isNotBlank() &&
                        quantity.isNotBlank() &&
                        !isQuantityError &&
                        invested.isNotBlank() &&
                        !isInvestedError
                }
                "FD", "Real Estate" -> {
                    invested.isNotBlank() &&
                        !isInvestedError &&
                        interestRate.isNotBlank() &&
                        !isInterestRateError
                }
                else -> {
                    invested.isNotBlank() && !isInvestedError
                }
            }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            // Header with Close Icon
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        if (initialInvestment ==
                            null
                        ) {
                            stringResource(R.string.add_investment)
                        } else {
                            stringResource(R.string.edit_investment)
                        },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier =
                        Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                CircleShape,
                            ),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.investment_name)) },
                    placeholder = { Text(stringResource(R.string.eg_bitcoin_apple_stocks)) },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Rounded.Label,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Type Field
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = getLocalizedAssetType(type),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.asset_type)) },
                        leadingIcon = {
                            AssetTypeIcon(
                                type = type,
                                size = 30.dp,
                                iconSize = 16.dp,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeBottomSheet) },
                    )
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .clickable { showTypeBottomSheet = true },
                    )
                }

                // Invested Amount & Date Picker Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = invested,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) invested = it
                        },
                        label = {
                            Text(
                                when (type) {
                                    "FD" -> stringResource(R.string.principal_invested)
                                    "Real Estate" -> stringResource(R.string.purchase_price)
                                    else -> stringResource(R.string.invested_label)
                                },
                            )
                        },
                        leadingIcon = {
                            Text(
                                currencySymbol,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        isError = isInvestedError,
                        supportingText =
                            if (isInvestedError) {
                                {
                                    val text =
                                        when {
                                            investedVal == null -> "Invalid format"
                                            investedVal <= 0.0 -> stringResource(R.string.invested_amount_error_desc)
                                            else -> "Amount cannot exceed 100,000,000"
                                        }
                                    Text(text)
                                }
                            } else {
                                null
                            },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f),
                    )

                    Box(modifier = Modifier.weight(1.2f)) {
                        OutlinedTextField(
                            value = dateFormatter.format(Date(startDate)),
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    when (type) {
                                        "FD" -> stringResource(R.string.start_date)
                                        "Real Estate" -> stringResource(R.string.purchase_date)
                                        else -> stringResource(R.string.invested_date)
                                    },
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                            },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .matchParentSize()
                                    .clickable { showStartDatePicker = true },
                        )
                    }
                }

                // Conditional Fields: Stocks, Crypto, Gold, Mutual Funds (Symbol + Quantity)
                if (type in listOf("Stock", "Crypto", "Gold", "Mutual Fund")) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        // ── Symbol search field ──────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            OutlinedTextField(
                                value = symbol,
                                onValueChange = { newVal ->
                                    symbol = newVal
                                    viewModel.searchSymbols(newVal, type)
                                },
                                label = {
                                    Text(
                                        if (type ==
                                            "Mutual Fund"
                                        ) {
                                            stringResource(R.string.search_fund_scheme)
                                        } else {
                                            stringResource(R.string.search_symbol)
                                        },
                                    )
                                },
                                placeholder = {
                                    Text(
                                        when (type) {
                                            "Crypto" -> stringResource(R.string.eg_crypto_symbol)
                                            "Mutual Fund" -> stringResource(R.string.eg_mutual_fund_symbol)
                                            "Gold" -> stringResource(R.string.tap_to_see_options)
                                            else -> stringResource(R.string.eg_stock_symbol)
                                        },
                                    )
                                },
                                leadingIcon = {
                                    if (isSearching) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    } else {
                                        Icon(
                                            Icons.Rounded.Search,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (symbol.isNotEmpty()) {
                                        IconButton(onClick = {
                                            symbol = ""
                                            viewModel.clearSymbolSearch()
                                        }) {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = stringResource(R.string.clear),
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                },
                                keyboardOptions =
                                    KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        autoCorrectEnabled = false,
                                        imeAction = ImeAction.Done,
                                    ),
                                singleLine = true,
                                shape = MaterialTheme.shapes.large,
                                modifier =
                                    Modifier
                                        .weight(1.2f)
                                        .clickable(enabled = type == "Gold") {
                                            viewModel.searchSymbols("", type)
                                        },
                            )

                            OutlinedTextField(
                                value = quantity,
                                onValueChange = {
                                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,4}$"))) quantity = it
                                },
                                label = {
                                    Text(
                                        if (type ==
                                            "Mutual Fund"
                                        ) {
                                            stringResource(R.string.units)
                                        } else {
                                            stringResource(R.string.quantity)
                                        },
                                    )
                                },
                                placeholder = { Text(stringResource(R.string.eg_15)) },
                                isError = isQuantityError,
                                supportingText =
                                    if (isQuantityError) {
                                        {
                                            val text =
                                                when {
                                                    quantityVal == null -> "Invalid format"
                                                    quantityVal <= 0.0 -> stringResource(R.string.quantity_error_desc)
                                                    else -> "Quantity cannot exceed 100,000,000"
                                                }
                                            Text(text)
                                        }
                                    } else {
                                        null
                                    },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.weight(0.8f),
                            )
                        }

                        // ── Autocomplete Dropdown ─────────────────────────────
                        if (symbolResults.isNotEmpty()) {
                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                shape = MaterialTheme.shapes.large,
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            ) {
                                Column {
                                    symbolResults.forEachIndexed { index, result ->
                                        SymbolResultRow(
                                            result = result,
                                            assetType = type,
                                            onClick = {
                                                symbol = result.symbol
                                                if (name.isBlank()) name = result.name
                                                viewModel.clearSymbolSearch()
                                            },
                                        )
                                        if (index < symbolResults.lastIndex) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Auto-fill quantity button ─────────────────────────
                        val investedValue = invested.toDoubleOrNull()
                        val canAutoFill =
                            symbolResults.isEmpty() &&
                                symbol.isNotBlank() &&
                                investedValue != null &&
                                investedValue > 0.0

                        if (canAutoFill) {
                            when (val state = autoFillState) {
                                AutoFillState.Idle -> {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.fetchQuantityForDate(
                                                symbol = symbol,
                                                assetType = type,
                                                investedAmount = invested.toDouble(),
                                                dateMs = startDate,
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.large,
                                    ) {
                                        Icon(
                                            Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.autocalculate_quantity_from_invested_date))
                                    }
                                }
                                AutoFillState.Loading -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            stringResource(R.string.fetching_price_on, dateFormatter.format(Date(startDate))),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                is AutoFillState.Success -> {
                                    val priceStr = String.format("%.2f", state.priceUsed)
                                    val dateStr = dateFormatter.format(Date(startDate))
                                    Surface(
                                        color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                                        shape = MaterialTheme.shapes.large,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Icon(
                                                Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Text(
                                                stringResource(R.string.quantity_filled_price, dateStr, priceStr),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF4CAF50),
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            IconButton(
                                                onClick = { viewModel.clearAutoFill() },
                                                modifier = Modifier.size(20.dp),
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    null,
                                                    tint = Color(0xFF4CAF50),
                                                    modifier = Modifier.size(14.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                                AutoFillState.Error -> {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        shape = MaterialTheme.shapes.large,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Icon(
                                                Icons.Rounded.ErrorOutline,
                                                null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Text(
                                                stringResource(R.string.price_not_available_for_this),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Conditional Fields: FD & Real Estate (Interest Rate / Appreciation Rate)
                if (type in listOf("FD", "Real Estate")) {
                    OutlinedTextField(
                        value = interestRate,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) interestRate = it
                        },
                        label = {
                            Text(
                                if (type ==
                                    "FD"
                                ) {
                                    stringResource(R.string.interest_rate_percent)
                                } else {
                                    stringResource(R.string.appreciation_rate_percent)
                                },
                            )
                        },
                        placeholder = { Text(stringResource(R.string.eg_75)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Percent, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        isError = isInterestRateError,
                        supportingText =
                            if (isInterestRateError) {
                                {
                                    val text =
                                        when {
                                            interestRateVal == null -> "Invalid format"
                                            interestRateVal < 0.0 -> stringResource(R.string.appreciation_rate_error_desc)
                                            else -> "Rate cannot exceed 100%"
                                        }
                                    Text(text)
                                }
                            } else {
                                null
                            },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Save Button
                Button(
                    onClick = {
                        val investedValue = invested.toDoubleOrNull() ?: 0.0
                        val finalSymbol = if (type in listOf("Stock", "Mutual Fund", "Crypto", "Gold")) symbol else null
                        val finalQuantity =
                            if (type in
                                listOf("Stock", "Mutual Fund", "Crypto", "Gold")
                            ) {
                                quantity.toDoubleOrNull()
                            } else {
                                null
                            }
                        val finalRate = if (type in listOf("FD", "Real Estate")) interestRate.toDoubleOrNull() else null
                        val finalStart = startDate

                        val computedCurrent =
                            if (type in listOf("FD", "Real Estate")) {
                                prasad.vennam.moneypilot.util.FinancePriceFetcher.calculateCompoundedValue(
                                    investedAmount = investedValue,
                                    annualRate = finalRate ?: 0.0,
                                    startDate = finalStart,
                                )
                            } else {
                                initialInvestment?.currentValue?.inRupees ?: investedValue
                            }

                        onSave(
                            name,
                            type,
                            investedValue,
                            computedCurrent,
                            finalSymbol,
                            finalQuantity,
                            finalRate,
                            finalStart,
                        )
                    },
                    enabled = isFormValid,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Rounded.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.save_investment), style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        if (showTypeBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTypeBottomSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text(
                        stringResource(R.string.select_asset_type),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(24.dp),
                    )
                    types.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(getLocalizedAssetType(t)) },
                            leadingIcon = {
                                AssetTypeIcon(
                                    type = t,
                                    size = 32.dp,
                                    iconSize = 18.dp,
                                )
                            },
                            onClick = {
                                type = t
                                showTypeBottomSheet = false
                            },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }

        if (showStartDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { startDate = it }
                        showStartDatePicker = false
                    }) { Text(stringResource(R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showStartDatePicker = false }) { Text(stringResource(R.string.cancel)) }
                },
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
