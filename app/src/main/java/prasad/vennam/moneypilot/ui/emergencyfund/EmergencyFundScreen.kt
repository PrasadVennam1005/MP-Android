package prasad.vennam.moneypilot.ui.emergencyfund

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.window.core.layout.WindowWidthSizeClass
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.ui.components.BaseBottomSheet
import prasad.vennam.moneypilot.ui.emergencyfund.components.EmergencyFundActionRow
import prasad.vennam.moneypilot.ui.emergencyfund.components.EmergencyFundDetailStatsRow
import prasad.vennam.moneypilot.ui.emergencyfund.components.EmergencyFundGaugeCard
import prasad.vennam.moneypilot.ui.emergencyfund.components.EmergencyFundHeaderSection
import prasad.vennam.moneypilot.ui.emergencyfund.components.EmergencyFundRemainingStatusCard
import prasad.vennam.moneypilot.ui.emergencyfund.components.EmptyEmergencyFundCard
import prasad.vennam.moneypilot.ui.viewmodel.EmergencyFundViewModel
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import prasad.vennam.moneypilot.util.TrackScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyFundScreen(
    userPreferences: UserPreferences,
    analyticsHelper: AnalyticsHelper,
    onNavigateBack: () -> Unit,
    viewModel: EmergencyFundViewModel = hiltViewModel(),
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.EMERGENCY_FUND)
    val coroutineScope = rememberCoroutineScope()
    val currencyCode = LocalCurrencyCode.current

    // Observe state from Room database via ViewModel
    val emergencyFundState by viewModel.emergencyFund.collectAsState()
    val monthlyExpenses = emergencyFundState?.monthlyExpenses ?: 0.0
    val targetMonths = emergencyFundState?.targetMonths ?: 6
    val currentSaved = emergencyFundState?.currentSaved ?: 0.0

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

    var showInfoSheet by remember { mutableStateOf(false) }
    var showSetupForm by remember { mutableStateOf(false) }
    var showDepositSheet by remember { mutableStateOf(false) }
    var showWithdrawSheet by remember { mutableStateOf(false) }

    val isConfigured = remember(monthlyExpenses) { monthlyExpenses > 0.0 }
    val targetGoal = remember(monthlyExpenses, targetMonths) { monthlyExpenses * targetMonths }
    val percentAchieved =
        remember(currentSaved, targetGoal) {
            if (targetGoal > 0.0) ((currentSaved / targetGoal) * 100).toFloat().coerceAtMost(100f) else 0f
        }
    val remainingToSave =
        remember(targetGoal, currentSaved) {
            (targetGoal - currentSaved).coerceAtLeast(0.0)
        }
    val coverageMonths =
        remember(currentSaved, monthlyExpenses) {
            if (monthlyExpenses > 0.0) currentSaved / monthlyExpenses else 0.0
        }

    val currentSavedFormatted =
        remember(currentSaved, currencyCode) {
            CurrencyFormatter.format(currentSaved, currencyCode)
        }
    val targetGoalFormatted =
        remember(targetGoal, currencyCode) {
            CurrencyFormatter.format(targetGoal, currencyCode)
        }
    val remainingToSaveFormatted =
        remember(remainingToSave, currencyCode) {
            CurrencyFormatter.format(remainingToSave, currencyCode)
        }
    val currencySymbol =
        remember(currencyCode) {
            try {
                java.util.Currency
                    .getInstance(currencyCode)
                    .symbol
            } catch (e: Exception) {
                "$"
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                actions = {
                    if (isConfigured) {
                        TextButton(
                            onClick = { showSetupForm = true },
                            colors =
                                ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ),
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.update_details))
                        }
                    } else {
                        Button(
                            onClick = { showSetupForm = true },
                            shape = MaterialTheme.shapes.large,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary,
                                ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.set_up),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
        ) {

            // Header Section
            EmergencyFundHeaderSection(onInfoClick = { showInfoSheet = true })

            Spacer(modifier = Modifier.height(32.dp))

            if (!isConfigured) {
                EmptyEmergencyFundCard(onSetUpClick = { showSetupForm = true })
            } else {
                // Premium Progress Visual Dashboard
                if (isExpanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Left Pane: Gauge visualizer card + action buttons
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            EmergencyFundGaugeCard(
                                percentAchieved = percentAchieved,
                                coverageMonths = coverageMonths,
                                targetMonths = targetMonths
                            )

                            EmergencyFundActionRow(
                                onWithdrawClick = { showWithdrawSheet = true },
                                onDepositClick = { showDepositSheet = true }
                            )
                        }

                        // Right Pane: Detail statistics cards + remaining target card
                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            EmergencyFundDetailStatsRow(
                                currentSavedFormatted = currentSavedFormatted,
                                targetGoalFormatted = targetGoalFormatted
                            )

                            EmergencyFundRemainingStatusCard(
                                remainingToSave = remainingToSave,
                                remainingToSaveFormatted = remainingToSaveFormatted
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        EmergencyFundGaugeCard(
                            percentAchieved = percentAchieved,
                            coverageMonths = coverageMonths,
                            targetMonths = targetMonths
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        EmergencyFundDetailStatsRow(
                            currentSavedFormatted = currentSavedFormatted,
                            targetGoalFormatted = targetGoalFormatted
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        EmergencyFundRemainingStatusCard(
                            remainingToSave = remainingToSave,
                            remainingToSaveFormatted = remainingToSaveFormatted
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        EmergencyFundActionRow(
                            onWithdrawClick = { showWithdrawSheet = true },
                            onDepositClick = { showDepositSheet = true }
                        )
                    }
                }
            }
        }
    }


    // Modal Bottom Sheet: info
    if (showInfoSheet) {
        BaseBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            title = stringResource(R.string.safety_net_info_title),
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.safety_net_info_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showInfoSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                ) {
                    Text(stringResource(R.string.dismiss))
                }
            }
        }
    }

    // Custom setup/calculator bottom sheet
    if (showSetupForm) {
        var expensesStr by remember {
            mutableStateOf(
                if (monthlyExpenses > 0.0) monthlyExpenses.toInt().toString() else "",
            )
        }
        var currentSavedStr by remember {
            mutableStateOf(
                if (currentSaved > 0.0) currentSaved.toInt().toString() else "0",
            )
        }
        var selectedPeriodIndex by remember {
            mutableStateOf(
                when (targetMonths) {
                    3 -> 0
                    6 -> 1
                    9 -> 2
                    12 -> 3
                    else -> 1
                },
            )
        }
        val periods = remember { listOf(3, 6, 9, 12) }
        BaseBottomSheet(
            onDismissRequest = { showSetupForm = false },
            title = stringResource(R.string.emergency_fund_setup),
        ) {
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState()),
            ) {

                val expensesVal = expensesStr.toDoubleOrNull()
                val currentSavedVal = currentSavedStr.toDoubleOrNull()
                val isExpensesError = expensesStr.isNotEmpty() && (expensesVal == null || expensesVal <= 0.0 || expensesVal > 100000000.0)
                val isCurrentSavedError =
                    currentSavedStr.isNotEmpty() && (currentSavedVal == null || currentSavedVal < 0.0 || currentSavedVal > 100000000.0)

                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Monthly Expenses Input
                    OutlinedTextField(
                        value = expensesStr,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                expensesStr = it
                            }
                        },
                        label = { Text(stringResource(R.string.monthly_expenses)) },
                        prefix = {
                            Text(
                                text = currencySymbol,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        isError = isExpensesError,
                        supportingText =
                            if (isExpensesError) {
                                {
                                    val text =
                                        when {
                                            expensesVal == null -> "Invalid format"
                                            expensesVal <= 0.0 -> "Expenses must be greater than 0"
                                            else -> "Expenses cannot exceed 100,000,000"
                                        }
                                    Text(text)
                                }
                            } else {
                                null
                            },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Current Saved Input
                    OutlinedTextField(
                        value = currentSavedStr,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                currentSavedStr = it
                            }
                        },
                        label = { Text(stringResource(R.string.current_savings)) },
                        prefix = {
                            Text(
                                text = currencySymbol,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        isError = isCurrentSavedError,
                        supportingText =
                            if (isCurrentSavedError) {
                                {
                                    val text =
                                        when {
                                            currentSavedVal == null -> "Invalid format"
                                            currentSavedVal < 0.0 -> "Savings cannot be negative"
                                            else -> "Savings cannot exceed 100,000,000"
                                        }
                                    Text(text)
                                }
                            } else {
                                null
                            },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }),
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Target Months Selector
                    Text(
                        text = stringResource(R.string.target_months),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        periods.forEachIndexed { index, months ->
                            val isSelected = selectedPeriodIndex == index
                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.secondary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.5f,
                                                )
                                            },
                                        )
                                        .clickable { selectedPeriodIndex = index }
                                        .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "$months M",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showSetupForm = false },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                val monthly = expensesStr.toDoubleOrNull() ?: 0.0
                                val saved = currentSavedStr.toDoubleOrNull() ?: 0.0
                                val months = periods[selectedPeriodIndex]
                                if (monthly > 0.0 && saved >= 0.0) {
                                    viewModel.saveEmergencyFund(monthly, months, saved)
                                    showSetupForm = false
                                }
                            },
                            shape = MaterialTheme.shapes.large,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary,
                                ),
                            modifier = Modifier.weight(1f),
                            enabled = expensesStr.isNotBlank() && !isExpensesError && currentSavedStr.isNotBlank() && !isCurrentSavedError,
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    }

    // Deposit Bottom Sheet
    if (showDepositSheet) {
        var addAmountStr by remember { mutableStateOf("") }

        BaseBottomSheet(
            onDismissRequest = { showDepositSheet = false },
            title = stringResource(R.string.deposit_title),
        ) {
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState()),
            ) {

                val addAmountVal = addAmountStr.toDoubleOrNull()
                val isDepositError =
                    addAmountStr.isNotEmpty() && (addAmountVal == null || addAmountVal <= 0.0 || addAmountVal > 100000000.0)

                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    OutlinedTextField(
                        value = addAmountStr,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                addAmountStr = it
                            }
                        },
                        placeholder = { Text(stringResource(R.string.enter_amount)) },
                        prefix = {
                            Text(
                                text = currencySymbol,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        isError = isDepositError,
                        supportingText =
                            if (isDepositError) {
                                {
                                    val text =
                                        when {
                                            addAmountVal == null -> "Invalid format"
                                            addAmountVal <= 0.0 -> "Deposit amount must be greater than 0"
                                            else -> "Deposit cannot exceed 100,000,000"
                                        }
                                    Text(text)
                                }
                            } else {
                                null
                            },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }),
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showDepositSheet = false },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                val additional = addAmountStr.toDoubleOrNull() ?: 0.0
                                if (additional > 0.0) {
                                    analyticsHelper.logEvent(
                                        AnalyticsConstants.Event.EMERGENCY_FUND_DEPOSIT,
                                        mapOf(AnalyticsConstants.Param.AMOUNT to additional),
                                    )
                                    viewModel.updateEmergencySaved(currentSaved + additional)
                                    showDepositSheet = false
                                }
                            },
                            shape = MaterialTheme.shapes.large,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary,
                                ),
                            modifier = Modifier.weight(1f),
                            enabled = addAmountStr.isNotBlank() && !isDepositError,
                        ) {
                            Text(stringResource(R.string.deposit))
                        }
                    }
                }
            }
        }
    }

    // Withdraw Bottom Sheet
    if (showWithdrawSheet) {
        var withdrawAmountStr by remember { mutableStateOf("") }

        BaseBottomSheet(
            onDismissRequest = { showWithdrawSheet = false },
            title = stringResource(R.string.withdraw_title),
        ) {
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState()),
            ) {

                val withdrawAmountVal = withdrawAmountStr.toDoubleOrNull()
                val withdrawErrorText =
                    when {
                        withdrawAmountStr.isEmpty() -> null
                        withdrawAmountVal == null || withdrawAmountVal <= 0.0 -> "Withdrawal amount must be greater than 0"
                        withdrawAmountVal > 100000000.0 -> "Withdrawal cannot exceed 100,000,000"
                        withdrawAmountVal > currentSaved -> "Withdrawal amount cannot exceed current savings"
                        else -> null
                    }

                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    OutlinedTextField(
                        value = withdrawAmountStr,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                withdrawAmountStr = it
                            }
                        },
                        placeholder = { Text(stringResource(R.string.enter_amount)) },
                        prefix = {
                            Text(
                                text = currencySymbol,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        isError = withdrawErrorText != null,
                        supportingText =
                            if (withdrawErrorText != null) {
                                { Text(withdrawErrorText) }
                            } else {
                                null
                            },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }),
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showWithdrawSheet = false },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                val amount = withdrawAmountStr.toDoubleOrNull() ?: 0.0
                                if (amount > 0.0 && amount <= currentSaved) {
                                    analyticsHelper.logEvent(
                                        AnalyticsConstants.Event.EMERGENCY_FUND_WITHDRAW,
                                        mapOf(AnalyticsConstants.Param.AMOUNT to amount),
                                    )
                                    viewModel.updateEmergencySaved(
                                        (currentSaved - amount).coerceAtLeast(
                                            0.0,
                                        ),
                                    )
                                    showWithdrawSheet = false
                                }
                            },
                            shape = MaterialTheme.shapes.large,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                            modifier = Modifier.weight(1f),
                            enabled = withdrawAmountStr.isNotBlank() && withdrawErrorText == null,
                        ) {
                            Text(stringResource(R.string.withdraw))
                        }
                    }
                }
            }
        }
    }
}
