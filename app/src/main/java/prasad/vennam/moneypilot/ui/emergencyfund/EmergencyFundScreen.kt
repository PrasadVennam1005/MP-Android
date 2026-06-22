package prasad.vennam.moneypilot.ui.emergencyfund

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.ui.viewmodel.EmergencyFundViewModel
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.LocalCurrencyCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyFundScreen(
    userPreferences: UserPreferences,
    onNavigateBack: () -> Unit,
    viewModel: EmergencyFundViewModel = hiltViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    val currencyCode = LocalCurrencyCode.current

    // Observe state from Room database via ViewModel
    val emergencyFundState by viewModel.emergencyFund.collectAsState()
    val monthlyExpenses = emergencyFundState?.monthlyExpenses ?: 0.0
    val targetMonths = emergencyFundState?.targetMonths ?: 6
    val currentSaved = emergencyFundState?.currentSaved ?: 0.0

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
            Text(
                text = stringResource(R.string.emergency_fund),
                style =
                    MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                    ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.safety_net_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(10.dp))

            // What should I add here? Row Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .clickable { showInfoSheet = true }
                        .padding(vertical = 4.dp),
            ) {
                // Mockup has a small solid rectangle bullet
                Box(
                    modifier =
                        Modifier
                            .size(height = 12.dp, width = 6.dp)
                            .background(
                                MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(2.dp),
                            ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.what_should_i_add_here),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                        ),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!isConfigured) {
                // Empty State Screen (Matches mockup exactly)
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Shield Icon Card
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.extraLarge,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.build_safety_net),
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.safety_net_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showSetupForm = true },
                        shape = MaterialTheme.shapes.large,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                            ),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.get_started),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            } else {
                // Premium Progress Visual Dashboard
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Gauge Visualizer Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(200.dp),
                            ) {
                                val animatedPercent by animateFloatAsState(
                                    targetValue = percentAchieved,
                                    animationSpec = tween(durationMillis = 1000),
                                )

                                val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                val sweepColor = MaterialTheme.colorScheme.secondary

                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val gaugeStroke = remember(density) {
                                    Stroke(width = with(density) { 16.dp.toPx() }, cap = StrokeCap.Round)
                                }
                                val sweepGradientBrush = remember(sweepColor) {
                                    Brush.sweepGradient(
                                        listOf(
                                            sweepColor.copy(alpha = 0.6f),
                                            sweepColor,
                                        )
                                    )
                                }

                                // Circular Gauge Canvas
                                Canvas(modifier = Modifier.size(170.dp)) {
                                    // Track circle
                                    drawCircle(
                                        color = trackColor,
                                        style = gaugeStroke,
                                    )
                                    // Progress sweep arc
                                    drawArc(
                                        brush = sweepGradientBrush,
                                        startAngle = -90f,
                                        sweepAngle = (animatedPercent / 100f) * 360f,
                                        useCenter = false,
                                        style = gaugeStroke,
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${percentAchieved.toInt()}%",
                                        style =
                                            MaterialTheme.typography.headlineLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 38.sp,
                                            ),
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.current_progress),
                                        style = MaterialTheme.typography.labelSmall,
                                        color =
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.6f,
                                            ),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text =
                                    stringResource(
                                        R.string.safety_net_achieved,
                                        coverageMonths,
                                    ),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text =
                                    stringResource(
                                        R.string.safety_net_months_target,
                                        targetMonths,
                                    ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detail statistics cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.saved_amount),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentSavedFormatted,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.target_amount),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = targetGoalFormatted,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Remaining card
                    if (remainingToSave > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                                ),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.remaining_target),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                    Text(
                                        text = remainingToSaveFormatted,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Safety Net fully funded! Amazing job.",
                                    style =
                                        MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        ),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showWithdrawSheet = true },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.weight(1f),
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            border =
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                ),
                        ) {
                            Text(
                                text = stringResource(R.string.withdraw),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        Button(
                            onClick = { showDepositSheet = true },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.weight(1f),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary,
                                ),
                        ) {
                            Text(
                                text = stringResource(R.string.deposit),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    // Modal Bottom Sheet: info
    if (showInfoSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.safety_net_info_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
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
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showSetupForm = false },
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
                        text = stringResource(R.string.emergency_fund_setup),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    IconButton(
                        onClick = { showSetupForm = false },
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

                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(bottom = 24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                                        ).clickable { selectedPeriodIndex = index }
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
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showDepositSheet = false },
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
                        text = stringResource(R.string.deposit_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    IconButton(
                        onClick = { showDepositSheet = false },
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

                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(bottom = 24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showWithdrawSheet = false },
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
                        text = stringResource(R.string.withdraw_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    IconButton(
                        onClick = { showWithdrawSheet = false },
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

                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(bottom = 24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
