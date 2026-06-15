package prasad.vennam.moneypilot.ui.investments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import prasad.vennam.moneypilot.util.inPaisa

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentScreen(
    viewModel: InvestmentViewModel,
    userData: UserPreferences.UserData?,
    syncState: SyncState?,
    onProfileClick: () -> Unit,
) {
    val investments by viewModel.allInvestments.collectAsState()
    var showFormSheet by remember { mutableStateOf(false) }
    var investmentToEdit by remember { mutableStateOf<Investment?>(null) }
    val currencyCode = LocalCurrencyCode.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                        IconButton(onClick = { viewModel.refreshAllPrices() }) {
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
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
                                investedAmount = invested.inPaisa,
                                currentValue = current.inPaisa,
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
                                investedAmount = invested.inPaisa,
                                currentValue = current.inPaisa,
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
