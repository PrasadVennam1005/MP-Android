package prasad.vennam.moneypilot.ui.currency

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.model.RateAlert
import prasad.vennam.moneypilot.ui.theme.MoneyPilotTheme
import prasad.vennam.moneypilot.ui.theme.PremiumShapes
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.TrackScreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(
    onNavigateBack: () -> Unit,
    analyticsHelper: AnalyticsHelper,
    viewModel: CurrencyConverterViewModel = hiltViewModel(),
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.CURRENCY_CONVERTER)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectingFromCurrency by remember { mutableStateOf(true) } // true = From, false = To
    var selectingBasketCurrency by remember { mutableStateOf(false) }
    var showRateAlertSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    var rotationAngle by remember { mutableStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "SwapRotation",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(prasad.vennam.moneypilot.R.string.currency_converter),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(prasad.vennam.moneypilot.R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            analyticsHelper.logEvent(AnalyticsConstants.Event.CURRENCY_REFRESH_CLICKED)
                            viewModel.refreshRates()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(prasad.vennam.moneypilot.R.string.refresh_rates),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
        ) {
            if (uiState.isLoading && uiState.currencies.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Mode Selector Tabs
                    item {
                        TabRow(
                            selectedTabIndex = if (uiState.isComparisonMode) 1 else 0,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(PremiumShapes.large)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (uiState.isComparisonMode) 1 else 0]),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            },
                            divider = {},
                        ) {
                            Tab(
                                selected = !uiState.isComparisonMode,
                                onClick = {
                                    analyticsHelper.logEvent(
                                        AnalyticsConstants.Event.CURRENCY_MODE_SWITCHED,
                                        mapOf(AnalyticsConstants.Param.MODE to "standard"),
                                    )
                                    viewModel.setComparisonMode(false)
                                },
                                text = { Text(stringResource(prasad.vennam.moneypilot.R.string.standard_mode), fontWeight = FontWeight.Bold) },
                                icon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null) },
                            )
                            Tab(
                                selected = uiState.isComparisonMode,
                                onClick = {
                                    analyticsHelper.logEvent(
                                        AnalyticsConstants.Event.CURRENCY_MODE_SWITCHED,
                                        mapOf(AnalyticsConstants.Param.MODE to "basket"),
                                    )
                                    viewModel.setComparisonMode(true)
                                },
                                text = { Text(stringResource(prasad.vennam.moneypilot.R.string.comparison_basket), fontWeight = FontWeight.Bold) },
                                icon = { Icon(Icons.Rounded.GridView, contentDescription = null) },
                            )
                        }
                    }

                    // Main Input card layouts
                    item {
                        if (uiState.isComparisonMode) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AmountInputCard(
                                    title = stringResource(prasad.vennam.moneypilot.R.string.base_amount),
                                    amount = uiState.amount,
                                    currencyCode = uiState.fromCurrency,
                                    currencies = uiState.currencies,
                                    onAmountChange = { viewModel.setAmount(it) },
                                    onCurrencyClick = {
                                        selectingFromCurrency = true
                                        selectingBasketCurrency = false
                                        searchQuery = ""
                                        showBottomSheet = true
                                    },
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                TravelModeLocationButton(
                                    isLoading = uiState.isLocationLoading,
                                    detectedCountry = uiState.detectedCountry,
                                    onDetectClick = { viewModel.detectLocationByIp() },
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (isTablet) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            AmountInputCard(
                                                title = stringResource(prasad.vennam.moneypilot.R.string.from_label),
                                                amount = uiState.amount,
                                                currencyCode = uiState.fromCurrency,
                                                currencies = uiState.currencies,
                                                onAmountChange = { viewModel.setAmount(it) },
                                                onCurrencyClick = {
                                                    selectingFromCurrency = true
                                                    selectingBasketCurrency = false
                                                    searchQuery = ""
                                                    showBottomSheet = true
                                                },
                                            )
                                        }

                                        SwapCurrencyButton(
                                            rotationZ = animatedRotation,
                                            isVertical = false,
                                            onClick = {
                                                analyticsHelper.logEvent(AnalyticsConstants.Event.CURRENCY_SWAPPED)
                                                rotationAngle += 180f
                                                viewModel.swapCurrencies()
                                            },
                                        )

                                        Box(modifier = Modifier.weight(1f)) {
                                            AmountInputCard(
                                                title = stringResource(prasad.vennam.moneypilot.R.string.to_label),
                                                amount = uiState.convertedAmount,
                                                currencyCode = uiState.toCurrency,
                                                currencies = uiState.currencies,
                                                onAmountChange = {},
                                                readOnly = true,
                                                onCurrencyClick = {
                                                    selectingFromCurrency = false
                                                    selectingBasketCurrency = false
                                                    searchQuery = ""
                                                    showBottomSheet = true
                                                },
                                            )
                                        }
                                    }
                                } else {
                                    AmountInputCard(
                                        title = stringResource(prasad.vennam.moneypilot.R.string.from_label),
                                        amount = uiState.amount,
                                        currencyCode = uiState.fromCurrency,
                                        currencies = uiState.currencies,
                                        onAmountChange = { viewModel.setAmount(it) },
                                        onCurrencyClick = {
                                            selectingFromCurrency = true
                                            selectingBasketCurrency = false
                                            searchQuery = ""
                                            showBottomSheet = true
                                        },
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    SwapCurrencyButton(
                                        rotationZ = animatedRotation,
                                        isVertical = true,
                                        onClick = {
                                            rotationAngle += 180f
                                            viewModel.swapCurrencies()
                                        },
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    AmountInputCard(
                                        title = stringResource(prasad.vennam.moneypilot.R.string.to_label),
                                        amount = uiState.convertedAmount,
                                        currencyCode = uiState.toCurrency,
                                        currencies = uiState.currencies,
                                        onAmountChange = {},
                                        readOnly = true,
                                        onCurrencyClick = {
                                            selectingFromCurrency = false
                                            selectingBasketCurrency = false
                                            searchQuery = ""
                                            showBottomSheet = true
                                        },
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                TravelModeLocationButton(
                                    isLoading = uiState.isLocationLoading,
                                    detectedCountry = uiState.detectedCountry,
                                    onDetectClick = { viewModel.detectLocationByIp() },
                                )
                            }
                        }
                    }

                    // Content based on Mode
                    if (uiState.isComparisonMode) {
                        item {
                            BasketComparisonList(
                                amount = uiState.amount,
                                fromCurrency = uiState.fromCurrency,
                                basketCurrencies = uiState.basketCurrencies,
                                basketConversions = uiState.basketConversions,
                                currencies = uiState.currencies,
                                onRemoveCurrency = { viewModel.removeFromBasket(it) },
                                onAddCurrencyClick = {
                                    selectingBasketCurrency = true
                                    selectingFromCurrency = false
                                    searchQuery = ""
                                    showBottomSheet = true
                                },
                            )
                        }
                    } else {
                        item {
                            ConversionResultCard(
                                amount = uiState.amount,
                                fromCurrency = uiState.fromCurrency,
                                convertedAmount = uiState.convertedAmount,
                                toCurrency = uiState.toCurrency,
                                exchangeRateText = uiState.exchangeRateText,
                            )
                        }

                        item {
                            QuickActionsRow(
                                isFavorite = uiState.isFavorite,
                                onFavoriteClick = { viewModel.toggleFavorite() },
                                onCopyClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Conversion Result", "${uiState.amount} ${uiState.fromCurrency} = ${uiState.convertedAmount} ${uiState.toCurrency}")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, context.getString(prasad.vennam.moneypilot.R.string.copied_result_to_clipboard), Toast.LENGTH_SHORT).show()
                                },
                                onShareClick = {
                                    val shareIntent =
                                        Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                context.getString(
                                                    prasad.vennam.moneypilot.R.string.share_conversion_template,
                                                    uiState.amount,
                                                    uiState.fromCurrency,
                                                    uiState.convertedAmount,
                                                    uiState.toCurrency,
                                                    uiState.exchangeRateText,
                                                ),
                                            )
                                            type = "text/plain"
                                        }
                                    context.startActivity(Intent.createChooser(shareIntent, context.getString(prasad.vennam.moneypilot.R.string.share_conversion_title)))
                                },
                                onAlertClick = { showRateAlertSheet = true },
                            )
                        }

                        item {
                            SparklineChart(
                                rates = uiState.historicalRates,
                                dates = uiState.historicalDates,
                                fromCurrency = uiState.fromCurrency,
                                toCurrency = uiState.toCurrency,
                            )
                        }
                    }

                    if (uiState.favoriteConversions.isNotEmpty()) {
                        item {
                            SectionHeader(title = stringResource(prasad.vennam.moneypilot.R.string.favorite_pairs))
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(uiState.favoriteConversions) { pair ->
                                    RecentConversionChip(
                                        from = pair.first,
                                        to = pair.second,
                                        isFavorite = true,
                                        onClick = { viewModel.selectPair(pair.first, pair.second) },
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.recentConversions.isNotEmpty()) {
                        item {
                            SectionHeader(title = stringResource(prasad.vennam.moneypilot.R.string.recent_searches))
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(uiState.recentConversions) { pair ->
                                    RecentConversionChip(
                                        from = pair.first,
                                        to = pair.second,
                                        isFavorite = false,
                                        onClick = { viewModel.selectPair(pair.first, pair.second) },
                                    )
                                }
                            }
                        }
                    }

                    item {
                        ExchangeRateInfoCard(
                            lastUpdated = uiState.lastUpdated,
                        )
                    }
                }
            }

            uiState.error?.let { err ->
                Snackbar(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(prasad.vennam.moneypilot.R.string.dismiss), color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    },
                ) {
                    Text(err)
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxHeight(0.85f)
                        .padding(horizontal = 16.dp),
            ) {
                Text(
                    text =
                        if (selectingFromCurrency) {
                            stringResource(prasad.vennam.moneypilot.R.string.select_source_currency)
                        } else if (selectingBasketCurrency) {
                            stringResource(prasad.vennam.moneypilot.R.string.add_currency_to_comparison_basket)
                        } else {
                            stringResource(prasad.vennam.moneypilot.R.string.select_target_currency)
                        },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(prasad.vennam.moneypilot.R.string.search_currency)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    shape = MaterialTheme.shapes.large,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        ),
                    singleLine = true,
                )

                val filteredCurrencies =
                    remember(uiState.currencies, searchQuery) {
                        uiState.currencies.filter {
                            it.code.contains(searchQuery, ignoreCase = true) ||
                                it.name.contains(searchQuery, ignoreCase = true)
                        }
                    }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filteredCurrencies) { item ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        if (selectingFromCurrency) {
                                            viewModel.selectFromCurrency(item.code)
                                        } else if (selectingBasketCurrency) {
                                            viewModel.addToBasket(item.code)
                                        } else {
                                            viewModel.selectToCurrency(item.code)
                                        }
                                        showBottomSheet = false
                                    }.padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.flag,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.code,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = item.symbol,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRateAlertSheet) {
        val currentRateVal =
            if (uiState.convertedAmount.isNotEmpty() && uiState.amount.toDoubleOrNull() ?: 0.0 > 0.0) {
                (uiState.convertedAmount.replace(",", "").toDoubleOrNull() ?: 0.0) / (uiState.amount.toDoubleOrNull() ?: 1.0)
            } else {
                1.0
            }

        RateAlertSheet(
            onDismiss = { showRateAlertSheet = false },
            currentRate = currentRateVal,
            fromCurrency = uiState.fromCurrency,
            toCurrency = uiState.toCurrency,
            activeAlerts = uiState.activeAlerts,
            onAddAlert = { rate, isAbove -> viewModel.addRateAlert(rate, isAbove) },
            onRemoveAlert = { alert -> viewModel.removeRateAlert(alert) },
        )
    }
}

@Composable
fun AmountInputCard(
    title: String,
    amount: String,
    currencyCode: String,
    currencies: List<CurrencyItem>,
    onAmountChange: (String) -> Unit,
    readOnly: Boolean = false,
    onCurrencyClick: () -> Unit,
) {
    val currencyItem =
        remember(currencies, currencyCode) {
            currencies.find { it.code == currencyCode } ?: CurrencyItem(currencyCode, currencyCode, "🏳", "")
        }

    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = if (isFocused && !readOnly) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = PremiumShapes.extraLarge,
                ),
        shape = PremiumShapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))

                androidx.compose.foundation.text.BasicTextField(
                    value = amount,
                    onValueChange = { newVal ->
                        if (newVal.isEmpty() || newVal.toDoubleOrNull() != null || newVal == ".") {
                            onAmountChange(newVal)
                        }
                    },
                    readOnly = readOnly,
                    textStyle =
                        MaterialTheme.typography.headlineLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused },
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (amount.isEmpty()) {
                            Text(
                                stringResource(prasad.vennam.moneypilot.R.string.enter_amount),
                                style =
                                    MaterialTheme.typography.headlineLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    ),
                            )
                        }
                        innerTextField()
                    },
                )
            }

            Box(
                modifier =
                    Modifier
                        .padding(horizontal = 12.dp)
                        .width(1.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            )

            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCurrencyClick() }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = currencyItem.flag,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(
                    text = currencyCode,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = stringResource(prasad.vennam.moneypilot.R.string.select_currency),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
fun SwapCurrencyButton(
    rotationZ: Float,
    isVertical: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .size(48.dp)
                .graphicsLayer { this.rotationZ = rotationZ }
                .shadow(4.dp, CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = if (isVertical) Icons.Rounded.SwapVert else Icons.Rounded.SwapHoriz,
                contentDescription = stringResource(prasad.vennam.moneypilot.R.string.swap_currencies),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun ConversionResultCard(
    amount: String,
    fromCurrency: String,
    convertedAmount: String,
    toCurrency: String,
    exchangeRateText: String,
) {
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(convertedAmount) {
        launch {
            scale.animateTo(0.95f, animationSpec = tween(100))
            scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
        }
        launch {
            alpha.animateTo(0.6f, animationSpec = tween(100))
            alpha.animateTo(1f, animationSpec = tween(150))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = PremiumShapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${amount.ifEmpty { "0" }} $fromCurrency =",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$convertedAmount $toCurrency",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier.graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                    },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = exchangeRateText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun QuickActionsRow(
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onAlertClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        OutlinedIconButton(
            onClick = onFavoriteClick,
            colors =
                IconButtonDefaults.outlinedIconButtonColors(
                    contentColor = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = stringResource(prasad.vennam.moneypilot.R.string.favorite_pair),
            )
        }

        OutlinedIconButton(
            onClick = onCopyClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = stringResource(prasad.vennam.moneypilot.R.string.copy_result),
            )
        }

        OutlinedIconButton(
            onClick = onShareClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = stringResource(prasad.vennam.moneypilot.R.string.share_conversion),
            )
        }

        OutlinedIconButton(
            onClick = onAlertClick,
            colors =
                IconButtonDefaults.outlinedIconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = stringResource(prasad.vennam.moneypilot.R.string.set_rate_alert),
            )
        }
    }
}

@Composable
fun RecentConversionChip(
    from: String,
    to: String,
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource =
        remember {
            androidx.compose.foundation.interaction
                .MutableInteractionSource()
        }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ChipScale",
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier =
            Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        shape = MaterialTheme.shapes.medium,
        color = if (isFavorite) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border =
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isFavorite) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isFavorite) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .size(16.dp)
                            .padding(end = 4.dp),
                )
            }
            Text(
                text = "$from ➔ $to",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
fun ExchangeRateInfoCard(
    lastUpdated: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = PremiumShapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = stringResource(prasad.vennam.moneypilot.R.string.exchange_rates_sourced_from_frankfurter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                if (lastUpdated.isNotEmpty()) {
                    Text(
                        text = stringResource(prasad.vennam.moneypilot.R.string.last_updated, lastUpdated),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
    )
}

// ---------------- PREMIUM COMPOSABLES ----------------

@Composable
fun TravelModeLocationButton(
    isLoading: Boolean,
    detectedCountry: String?,
    onDetectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = PremiumShapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onDetectClick() }
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = "Travel Mode Location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(prasad.vennam.moneypilot.R.string.travel_mode_gps_prefill),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text =
                        if (detectedCountry != null) {
                            stringResource(prasad.vennam.moneypilot.R.string.detected_country_code, detectedCountry)
                        } else {
                            stringResource(prasad.vennam.moneypilot.R.string.travel_mode_gps_desc)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun BasketComparisonList(
    amount: String,
    fromCurrency: String,
    basketCurrencies: List<String>,
    basketConversions: Map<String, String>,
    currencies: List<CurrencyItem>,
    onRemoveCurrency: (String) -> Unit,
    onAddCurrencyClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = PremiumShapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(prasad.vennam.moneypilot.R.string.multi_currency_basket),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                TextButton(onClick = onAddCurrencyClick) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(prasad.vennam.moneypilot.R.string.add_currency), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(prasad.vennam.moneypilot.R.string.add_currency))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (basketCurrencies.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(prasad.vennam.moneypilot.R.string.basket_is_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                basketCurrencies.forEach { code ->
                    val currencyItem = currencies.find { it.code == code } ?: CurrencyItem(code, code, "🏳", "")
                    val convertedVal = basketConversions[code] ?: "0.00"

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(PremiumShapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(currencyItem.flag, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(code, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(currencyItem.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${currencyItem.symbol} $convertedVal",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { onRemoveCurrency(code) },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(prasad.vennam.moneypilot.R.string.remove),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SparklineChart(
    rates: List<Double>,
    dates: List<String>,
    fromCurrency: String,
    toCurrency: String,
    modifier: Modifier = Modifier,
) {
    if (rates.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = PremiumShapes.large,
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                ),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(prasad.vennam.moneypilot.R.string.historical_trend_loading_or_unavailable), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    var activeIndex by remember(rates) { mutableStateOf(-1) }
    val minRate = rates.minOrNull() ?: 0.0
    val maxRate = rates.maxOrNull() ?: 1.0
    val range = if (maxRate == minRate) 1.0 else maxRate - minRate

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = PremiumShapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(prasad.vennam.moneypilot.R.string.trend_30_day),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text =
                            if (activeIndex in rates.indices) {
                                stringResource(prasad.vennam.moneypilot.R.string.rate_on_date, dates[activeIndex], String.format(Locale.US, "%.5f", rates[activeIndex]))
                            } else {
                                stringResource(prasad.vennam.moneypilot.R.string.hold_drag_chart_to_inspect)
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (activeIndex != -1) primaryColor else onSurfaceVariant,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(prasad.vennam.moneypilot.R.string.max_rate_label, String.format(Locale.US, "%.4f", maxRate)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50),
                    )
                    Text(
                        text = stringResource(prasad.vennam.moneypilot.R.string.min_rate_label, String.format(Locale.US, "%.4f", minRate)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF44336),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .pointerInput(rates) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val xPercentage = offset.x / size.width
                                    activeIndex = (xPercentage * (rates.size - 1)).coerceIn(0f, (rates.size - 1).toFloat()).toInt()
                                },
                                onDragEnd = { activeIndex = -1 },
                                onDragCancel = { activeIndex = -1 },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val xPercentage = change.position.x / size.width
                                    activeIndex = (xPercentage * (rates.size - 1)).coerceIn(0f, (rates.size - 1).toFloat()).toInt()
                                },
                            )
                        },
            ) {
                val width = size.width
                val height = size.height
                val stepX = width / (rates.size - 1).coerceAtLeast(1)

                val points =
                    rates.mapIndexed { idx, rate ->
                        val x = idx * stepX
                        val y = height - 16.dp.toPx() - (((rate - minRate) / range) * (height - 32.dp.toPx())).toFloat()
                        Offset(x, y)
                    }

                val strokePath =
                    Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                val p1 = points[i - 1]
                                val p2 = points[i]
                                val controlX = (p1.x + p2.x) / 2
                                cubicTo(controlX, p1.y, controlX, p2.y, p2.x, p2.y)
                            }
                        }
                    }

                val fillPath =
                    Path().apply {
                        addPath(strokePath)
                        if (points.isNotEmpty()) {
                            lineTo(points.last().x, height)
                            lineTo(points.first().x, height)
                            close()
                        }
                    }

                // Draw Gradient Fill
                drawPath(
                    path = fillPath,
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                            startY = 0f,
                            endY = height,
                        ),
                )

                // Draw Line
                drawPath(
                    path = strokePath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )

                // Draw hover indicator line & dot
                if (activeIndex in points.indices) {
                    val activePoint = points[activeIndex]

                    // Vertical line
                    drawLine(
                        color = primaryColor.copy(alpha = 0.4f),
                        start = Offset(activePoint.x, 0f),
                        end = Offset(activePoint.x, height),
                        strokeWidth = 1.5.dp.toPx(),
                    )

                    // Highlight dot
                    drawCircle(
                        color = primaryColor,
                        radius = 6.dp.toPx(),
                        center = activePoint,
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = activePoint,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateAlertSheet(
    onDismiss: () -> Unit,
    currentRate: Double,
    fromCurrency: String,
    toCurrency: String,
    activeAlerts: List<RateAlert>,
    onAddAlert: (Double, Boolean) -> Unit,
    onRemoveAlert: (RateAlert) -> Unit,
) {
    var targetRateInput by remember { mutableStateOf(String.format(Locale.US, "%.4f", currentRate)) }
    var isAbove by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
        ) {
            Text(
                text = stringResource(prasad.vennam.moneypilot.R.string.set_exchange_rate_alert),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = stringResource(prasad.vennam.moneypilot.R.string.get_notified_on_match, fromCurrency),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            OutlinedTextField(
                value = targetRateInput,
                onValueChange = { targetRateInput = it },
                label = { Text(stringResource(prasad.vennam.moneypilot.R.string.target_rate_label, toCurrency)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = PremiumShapes.medium,
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(stringResource(prasad.vennam.moneypilot.R.string.trigger_alert_when_rate_is), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilterChip(
                    selected = isAbove,
                    onClick = { isAbove = true },
                    label = { Text(stringResource(prasad.vennam.moneypilot.R.string.above_target)) },
                    leadingIcon =
                        if (isAbove) {
                            { Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else {
                            null
                        },
                )
                FilterChip(
                    selected = !isAbove,
                    onClick = { isAbove = false },
                    label = { Text(stringResource(prasad.vennam.moneypilot.R.string.below_target)) },
                    leadingIcon =
                        if (!isAbove) {
                            { Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else {
                            null
                        },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val target = targetRateInput.toDoubleOrNull()
                    if (target != null && target > 0) {
                        onAddAlert(target, isAbove)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = PremiumShapes.medium,
            ) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(prasad.vennam.moneypilot.R.string.set_alert_trigger))
            }

            Spacer(modifier = Modifier.height(20.dp))

            val pairAlerts = activeAlerts.filter { it.from == fromCurrency && it.to == toCurrency }
            if (pairAlerts.isNotEmpty()) {
                Text(stringResource(prasad.vennam.moneypilot.R.string.active_alerts_for_pair, fromCurrency, toCurrency), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(pairAlerts) { alert ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(PremiumShapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (alert.isAbove) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                    contentDescription = null,
                                    tint = if (alert.isAbove) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                                Text(
                                    text =
                                        stringResource(
                                            prasad.vennam.moneypilot.R.string.alert_trigger_value,
                                            if (alert.isAbove) "≥" else "≤",
                                            alert.targetRate,
                                            toCurrency,
                                        ),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                )
                            }
                            IconButton(
                                onClick = { onRemoveAlert(alert) },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(prasad.vennam.moneypilot.R.string.cancel_alert),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAmountInputCard() {
    MoneyPilotTheme {
        AmountInputCard(
            title = "From",
            amount = "100.00",
            currencyCode = "USD",
            currencies = emptyList(),
            onAmountChange = {},
            onCurrencyClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewConversionResultCard() {
    MoneyPilotTheme {
        ConversionResultCard(
            amount = "100.00",
            fromCurrency = "USD",
            convertedAmount = "8,345.00",
            toCurrency = "INR",
            exchangeRateText = "1 USD = 83.45000 INR",
        )
    }
}
