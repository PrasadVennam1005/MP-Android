package prasad.vennam.moneypilot.ui.investments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.ui.components.ProfileIconButton
import prasad.vennam.moneypilot.ui.dashboard.SyncState
import prasad.vennam.moneypilot.ui.dashboard.SyncStatusIndicator
import prasad.vennam.moneypilot.ui.investments.components.*
import prasad.vennam.moneypilot.ui.viewmodel.InvestmentViewModel
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import prasad.vennam.moneypilot.util.TrackScreen
import prasad.vennam.moneypilot.util.toMinorUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentScreen(
    viewModel: InvestmentViewModel,
    userData: UserPreferences.UserData?,
    syncState: SyncState?,
    isPremium: Boolean,
    analyticsHelper: AnalyticsHelper,
    onProfileClick: () -> Unit,
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.INVESTMENTS)
    val investments by viewModel.allInvestments.collectAsState()
    val allocationDetails by viewModel.allocationDetails.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val symbolResults by viewModel.symbolResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val autoFillState by viewModel.autoFillState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Holdings", "Allocation")
    var showFormSheet by remember { mutableStateOf(false) }
    var investmentToEdit by remember { mutableStateOf<Investment?>(null) }
    val currencyCode = LocalCurrencyCode.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    var isFabVisible by remember { mutableStateOf(true) }
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset) {
        val currentIndex = lazyListState.firstVisibleItemIndex
        val currentOffset = lazyListState.firstVisibleItemScrollOffset
        if (currentIndex == 0 && currentOffset == 0) {
            isFabVisible = true
        } else if (currentIndex > previousIndex || (currentIndex == previousIndex && currentOffset > previousOffset)) {
            isFabVisible = false
        } else if (currentIndex < previousIndex || (currentIndex == previousIndex && currentOffset < previousOffset)) {
            isFabVisible = true
        }
        previousIndex = currentIndex
        previousOffset = currentOffset
    }

    val investmentSummary by viewModel.investmentSummary.collectAsState()
    val totalInvested = investmentSummary.totalInvested
    val totalCurrent = investmentSummary.totalCurrent
    val totalGain = remember(totalCurrent, totalInvested) { totalCurrent - totalInvested }
    val gainPercent =
        remember(totalGain, totalInvested) {
            if (totalInvested > 0) (totalGain / totalInvested) * 100 else 0.0
        }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.investments),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                actions = {
                    if (syncState != null) {
                        SyncStatusIndicator(syncState)
                    }
                    AnimatedVisibility(
                        visible = !isFabVisible,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        IconButton(onClick = {
                            investmentToEdit = null
                            showFormSheet = true
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.add_investment),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    val isRefreshing by viewModel.isRefreshing.collectAsState()
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier
                                    .padding(end = 16.dp)
                                    .size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        IconButton(onClick = {
                            analyticsHelper.logEvent(AnalyticsConstants.Event.INVESTMENTS_REFRESH_CLICKED)
                            viewModel.refreshAllPrices()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.refresh_prices),
                            )
                        }
                    }
                    ProfileIconButton(userData = userData, onClick = onProfileClick)
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = Color.Unspecified,
                        navigationIconContentColor = Color.Unspecified,
                        titleContentColor = Color.Unspecified,
                        actionIconContentColor = Color.Unspecified,
                    ),
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedTab == 0 && isFabVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                FloatingActionButton(
                    onClick = {
                        investmentToEdit = null
                        showFormSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_investment))
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            analyticsHelper.logEvent(
                                AnalyticsConstants.Event.INVESTMENTS_TAB_SWITCHED,
                                mapOf(AnalyticsConstants.Param.TAB to title),
                            )
                            selectedTab = index
                        },
                        text = { Text(title, fontWeight = FontWeight.Bold) },
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            prasad.vennam.moneypilot.ui.components.AdBannerView(
                                isPremium = isPremium,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        item {
                            InvestmentSummaryCard(totalCurrent, totalGain, gainPercent)
                        }

                        item {
                            Text(
                                stringResource(R.string.your_portfolio),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }

                        if (investments.isEmpty()) {
                            item {
                                EmptyInvestmentState()
                            }
                        } else {
                            items(investments, key = { it.id }) { investment ->
                                val deletedMessage = stringResource(R.string.investment_deleted)
                                val undoLabel = stringResource(R.string.undo)
                                SwipeableInvestmentCard(
                                    investment = investment,
                                    onEdit = {
                                        investmentToEdit = investment
                                        showFormSheet = true
                                    },
                                    onDelete = {
                                        val investmentCopy = investment
                                        viewModel.deleteInvestment(investment)
                                        scope.launch {
                                            val result =
                                                snackbarHostState.showSnackbar(
                                                    message = deletedMessage,
                                                    actionLabel = undoLabel,
                                                    duration = SnackbarDuration.Short,
                                                )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.saveInvestment(investmentCopy)
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            AssetAllocationCard(
                                allocationDetails = allocationDetails,
                                selectedProfile = selectedProfile,
                                onProfileSelected = { viewModel.selectProfile(it) },
                                currencyCode = currencyCode,
                            )
                        }
                    }
                }
            }
        }

        if (showFormSheet) {
            InvestmentFormBottomSheet(
                initialInvestment = investmentToEdit,
                symbolResults = symbolResults,
                isSearching = isSearching,
                autoFillState = autoFillState,
                onSearchSymbols = { query, assetType -> viewModel.searchSymbols(query, assetType) },
                onClearSymbolSearch = { viewModel.clearSymbolSearch() },
                onFetchQuantityForDate = { symbol, assetType, investedAmount, dateMs ->
                    viewModel.fetchQuantityForDate(symbol, assetType, investedAmount, dateMs)
                },
                onClearAutoFill = { viewModel.clearAutoFill() },
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
                                investedAmount = invested.toMinorUnit,
                                currentValue = current.toMinorUnit,
                                symbol = symbol,
                                quantity = qty,
                                interestRate = rate,
                                startDate = start,
                                currencyCode = currencyCode,
                            ),
                        )
                    } else {
                        viewModel.saveInvestment(
                            investmentToEdit!!.copy(
                                name = name,
                                type = type,
                                investedAmount = invested.toMinorUnit,
                                currentValue = current.toMinorUnit,
                                symbol = symbol,
                                quantity = qty,
                                interestRate = rate,
                                startDate = start,
                            ),
                        )
                    }
                    viewModel.clearSymbolSearch()
                    viewModel.refreshAllPrices()
                    showFormSheet = false
                    investmentToEdit = null
                },
            )
        }
    }
}
