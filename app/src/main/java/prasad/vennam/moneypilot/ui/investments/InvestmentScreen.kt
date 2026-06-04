package prasad.vennam.moneypilot.ui.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.ui.viewmodel.AutoFillState
import prasad.vennam.moneypilot.ui.viewmodel.InvestmentViewModel
import prasad.vennam.moneypilot.util.FinancePriceFetcher
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentScreen(
    viewModel: InvestmentViewModel,
) {
    val investments by viewModel.allInvestments.collectAsState()
    var showFormSheet by remember { mutableStateOf(false) }
    var investmentToEdit by remember { mutableStateOf<Investment?>(null) }

    val totalInvested = remember(investments) { investments.sumOf { it.investedAmount } }
    val totalCurrent = remember(investments) { investments.sumOf { it.currentValue } }
    val totalGain = remember(totalCurrent, totalInvested) { totalCurrent - totalInvested }
    val gainPercent = remember(totalGain, totalInvested) {
        if (totalInvested > 0) (totalGain / totalInvested) * 100 else 0.0
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Investments",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    val isRefreshing by viewModel.isRefreshing.collectAsState()
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(onClick = { viewModel.refreshAllPrices() }) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh prices"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    investmentToEdit = null
                    showFormSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Investment")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InvestmentSummaryCard(totalCurrent, totalGain, gainPercent)
            }

            item {
                Text(
                    "Your Portfolio",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (investments.isEmpty()) {
                item {
                    EmptyInvestmentState()
                }
            } else {
                items(investments, key = { it.id }) { investment ->
                    SwipeableInvestmentCard(
                        investment = investment,
                        onEdit = {
                            investmentToEdit = investment
                            showFormSheet = true
                        },
                        onDelete = { viewModel.deleteInvestment(investment) }
                    )
                }
            }
        }

        if (showFormSheet) {
            InvestmentFormBottomSheet(
                initialInvestment = investmentToEdit,
                viewModel = viewModel,
                onDismiss = {
                    viewModel.clearSymbolSearch()
                    showFormSheet = false
                    investmentToEdit = null
                },
                onSave = { name, type, invested, current, symbol, qty, rate, start ->
                    if (investmentToEdit == null) {
                        viewModel.saveInvestment(
                            Investment(
                                name = name,
                                type = type,
                                investedAmount = invested,
                                currentValue = current,
                                symbol = symbol,
                                quantity = qty,
                                interestRate = rate,
                                startDate = start
                            )
                        )
                    } else {
                        viewModel.saveInvestment(
                            investmentToEdit!!.copy(
                                name = name,
                                type = type,
                                investedAmount = invested,
                                currentValue = current,
                                symbol = symbol,
                                quantity = qty,
                                interestRate = rate,
                                startDate = start
                            )
                        )
                    }
                    viewModel.clearSymbolSearch()
                    viewModel.refreshAllPrices()
                    showFormSheet = false
                    investmentToEdit = null
                }
            )
        }
    }
}

@Composable
fun InvestmentSummaryCard(totalValue: Double, gain: Double, percent: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "TOTAL PORTFOLIO VALUE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
            Text(
                "₹${String.format(LocalLocale.current.platformLocale, "%,.0f", totalValue)}",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Rounded.TrendingUp,
                    contentDescription = null,
                    tint = if (gain >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${if (gain >= 0) "+" else ""}₹${
                        String.format(
                            LocalLocale.current.platformLocale,
                            "%,.0f",
                            gain
                        )
                    } (${String.format(LocalLocale.current.platformLocale, "%.1f", percent)}%)",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (gain >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableInvestmentCard(
    investment: Investment,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete()
                dismissState.reset()
            }

            SwipeToDismissBoxValue.StartToEnd -> {
                onEdit()
                dismissState.reset()
            }

            else -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Rounded.Edit
                SwipeToDismissBoxValue.EndToStart -> Icons.Rounded.Delete
                else -> Icons.Rounded.Delete
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                Icon(icon, contentDescription = null)
            }
        },
        content = {
            InvestmentItem(investment = investment)
        }
    )
}

@Composable
fun InvestmentItem(investment: Investment) {
    val gain = investment.currentValue - investment.investedAmount
    val gainPercent =
        if (investment.investedAmount > 0) (gain / investment.investedAmount) * 100 else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                AssetTypeIcon(
                    type = investment.type,
                    size = 48.dp,
                    iconSize = 24.dp,
                    shape = MaterialTheme.shapes.large
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        investment.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        investment.type.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₹${
                        String.format(
                            LocalLocale.current.platformLocale,
                            "%,.0f",
                            investment.currentValue
                        )
                    }",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${if (gain >= 0) "+" else ""}${
                        String.format(
                            LocalLocale.current.platformLocale,
                            "%.1f",
                            gainPercent
                        )
                    }%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (gain >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

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
    var invested by remember { mutableStateOf(initialInvestment?.investedAmount?.toString() ?: "") }
    
    // Tracking fields
    var symbol by remember { mutableStateOf(initialInvestment?.symbol ?: "") }
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
            quantity = String.format("%.4f", s.quantity)
                .trimEnd('0').trimEnd('.')
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isFormValid = when (type) {
        "Stock", "Mutual Fund", "Crypto", "Gold" -> {
            name.isNotBlank() && symbol.isNotBlank() && quantity.toDoubleOrNull() != null && invested.toDoubleOrNull() != null
        }
        "FD", "Real Estate" -> {
            name.isNotBlank() && interestRate.toDoubleOrNull() != null && invested.toDoubleOrNull() != null
        }
        else -> {
            name.isNotBlank() && invested.toDoubleOrNull() != null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Close Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialInvestment == null) "Add Investment" else "Edit Investment",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Investment Name") },
                    placeholder = { Text("e.g. Bitcoin, Apple Stocks") },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Rounded.Label,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                )

                // Type Field
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Asset Type") },
                        leadingIcon = {
                            AssetTypeIcon(
                                type = type,
                                size = 30.dp,
                                iconSize = 16.dp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeBottomSheet) }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showTypeBottomSheet = true })
                }

                // Invested Amount & Date Picker Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = invested,
                        onValueChange = {
                            if (it.isEmpty() || it.toDoubleOrNull() != null) invested = it
                        },
                        label = {
                            Text(
                                when (type) {
                                    "FD" -> "Principal Invested (₹)"
                                    "Real Estate" -> "Purchase Price (₹)"
                                    else -> "Invested (₹)"
                                }
                            )
                        },
                        leadingIcon = {
                            Text(
                                "₹",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f)
                    )

                    Box(modifier = Modifier.weight(1.2f)) {
                        OutlinedTextField(
                            value = dateFormatter.format(Date(startDate)),
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    when (type) {
                                        "FD" -> "Start Date"
                                        "Real Estate" -> "Purchase Date"
                                        else -> "Invested Date"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                            },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showStartDatePicker = true }
                        )
                    }
                }

                // Conditional Fields: Stocks, Crypto, Gold, Mutual Funds (Symbol + Quantity)
                if (type in listOf("Stock", "Crypto", "Gold", "Mutual Fund")) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        // ── Symbol search field ──────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = symbol,
                                onValueChange = { newVal ->
                                    symbol = newVal
                                    viewModel.searchSymbols(newVal, type)
                                },
                                label = {
                                    Text(
                                        if (type == "Mutual Fund") "Search Fund / Scheme Code" else "Search Symbol"
                                    )
                                },
                                placeholder = {
                                    Text(
                                        when (type) {
                                            "Crypto" -> "e.g. BTC-USD"
                                            "Mutual Fund" -> "e.g. HDFC Top 100"
                                            "Gold" -> "Tap to see options"
                                            else -> "e.g. RELIANCE.NS"
                                        }
                                    )
                                },
                                leadingIcon = {
                                    if (isSearching) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            Icons.Rounded.Search,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary
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
                                                contentDescription = "Clear",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    autoCorrectEnabled = false,
                                    imeAction = ImeAction.Done
                                ),
                                singleLine = true,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clickable(enabled = type == "Gold") {
                                        viewModel.searchSymbols("", type)
                                    }
                            )

                            OutlinedTextField(
                                value = quantity,
                                onValueChange = {
                                    if (it.isEmpty() || it.toDoubleOrNull() != null) quantity = it
                                },
                                label = { Text(if (type == "Mutual Fund") "Units" else "Quantity") },
                                placeholder = { Text("e.g. 1.5") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.weight(0.8f)
                            )
                        }

                        // ── Autocomplete Dropdown ─────────────────────────────
                        if (symbolResults.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                                            }
                                        )
                                        if (index < symbolResults.lastIndex) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Auto-fill quantity button ─────────────────────────
                        // Only show when: autocomplete is closed + symbol & amount are ready
                        val canAutoFill = symbolResults.isEmpty() &&
                            symbol.isNotBlank() &&
                            invested.toDoubleOrNull() != null &&
                            invested.toDoubleOrNull()!! > 0.0

                        if (canAutoFill) {
                            when (val state = autoFillState) {
                                AutoFillState.Idle -> {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.fetchQuantityForDate(
                                                symbol = symbol,
                                                assetType = type,
                                                investedAmount = invested.toDouble(),
                                                dateMs = startDate
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.large
                                    ) {
                                        Icon(
                                            Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Auto-calculate quantity from invested date price")
                                    }
                                }
                                AutoFillState.Loading -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            "Fetching price on ${dateFormatter.format(Date(startDate))}…",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                is AutoFillState.Success -> {
                                    val priceStr = String.format("%.2f", state.priceUsed)
                                    val dateStr = dateFormatter.format(Date(startDate))
                                    Surface(
                                        color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                                        shape = MaterialTheme.shapes.large,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                "Quantity filled · price on $dateStr was $priceStr",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF4CAF50)
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            IconButton(
                                                onClick = { viewModel.clearAutoFill() },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    null,
                                                    tint = Color(0xFF4CAF50),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                AutoFillState.Error -> {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        shape = MaterialTheme.shapes.large,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.ErrorOutline,
                                                null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                "Price not available for this symbol/date. Enter quantity manually.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
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
                            if (it.isEmpty() || it.toDoubleOrNull() != null) interestRate = it
                        },
                        label = {
                            Text(
                                if (type == "FD") "Interest Rate (% Annual)" else "Appreciation Rate (% Year)"
                            )
                        },
                        placeholder = { Text("e.g. 7.5") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Percent, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Save Button
                Button(
                    onClick = {
                        val investedValue = invested.toDoubleOrNull() ?: 0.0
                        val finalSymbol = if (type in listOf("Stock", "Mutual Fund", "Crypto", "Gold")) symbol else null
                        val finalQuantity = if (type in listOf("Stock", "Mutual Fund", "Crypto", "Gold")) quantity.toDoubleOrNull() else null
                        val finalRate = if (type in listOf("FD", "Real Estate")) interestRate.toDoubleOrNull() else null
                        val finalStart = startDate

                        val computedCurrent = if (type in listOf("FD", "Real Estate")) {
                            prasad.vennam.moneypilot.util.FinancePriceFetcher.calculateCompoundedValue(
                                investedAmount = investedValue,
                                annualRate = finalRate ?: 0.0,
                                startDate = finalStart
                            )
                        } else {
                            initialInvestment?.currentValue ?: investedValue
                        }

                        onSave(
                            name,
                            type,
                            investedValue,
                            computedCurrent,
                            finalSymbol,
                            finalQuantity,
                            finalRate,
                            finalStart
                        )
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Rounded.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Investment", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        if (showTypeBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTypeBottomSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text(
                        "Select Asset Type",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(24.dp)
                    )
                    types.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t) },
                            leadingIcon = {
                                AssetTypeIcon(
                                    type = t,
                                    size = 32.dp,
                                    iconSize = 18.dp
                                )
                            },
                            onClick = {
                                type = t
                                showTypeBottomSheet = false
                            },
                            modifier = Modifier.padding(horizontal = 8.dp)
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
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
fun EmptyInvestmentState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Analytics,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Portfolio is empty",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            "Tap + to add your first investment",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun AssetTypeIcon(
    type: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 32.dp,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp,
    shape: androidx.compose.ui.graphics.Shape = CircleShape
) {
    val icon = when (type) {
        "Stock" -> Icons.AutoMirrored.Rounded.ShowChart
        "Mutual Fund" -> Icons.Rounded.AccountBalance
        "Crypto" -> Icons.Rounded.CurrencyBitcoin
        "Real Estate" -> Icons.Rounded.HomeWork
        "Gold" -> Icons.Rounded.Savings
        "FD" -> Icons.Rounded.LockClock
        else -> Icons.Rounded.AccountBalanceWallet
    }

    val color = when (type) {
        "Stock" -> Color(0xFF2196F3)
        "Mutual Fund" -> Color(0xFF673AB7)
        "Crypto" -> Color(0xFFFF9800)
        "Real Estate" -> Color(0xFF795548)
        "Gold" -> Color(0xFFFFC107)
        "FD" -> Color(0xFF4CAF50)
        else -> Color.Gray
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = shape,
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun SymbolResultRow(
    result: FinancePriceFetcher.SymbolResult,
    assetType: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Small icon circle matching asset type colour
        AssetTypeIcon(type = assetType, size = 36.dp, iconSize = 18.dp)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.symbol,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = result.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        if (result.exchange.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = result.exchange,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
