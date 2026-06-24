package prasad.vennam.moneypilot.ui.sandbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.ui.viewmodel.SandboxViewModel
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.TrackScreen
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialSandboxScreen(
    onBack: () -> Unit,
    analyticsHelper: AnalyticsHelper,
    viewModel: SandboxViewModel = hiltViewModel(),
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.FINANCIAL_SANDBOX)
    val defaultsState by viewModel.defaults.collectAsState()

    var incomeInput by remember { mutableStateOf("") }
    var expenseInput by remember { mutableStateOf("") }

    // Initialize inputs when default values are loaded
    LaunchedEffect(defaultsState) {
        if (incomeInput.isEmpty() && defaultsState.avgMonthlyIncome > 0) {
            incomeInput = defaultsState.avgMonthlyIncome.roundToInt().toString()
        }
        if (expenseInput.isEmpty() && defaultsState.avgMonthlyExpense > 0) {
            expenseInput = defaultsState.avgMonthlyExpense.roundToInt().toString()
        }
    }

    val income = incomeInput.toDoubleOrNull() ?: 0.0
    val expense = expenseInput.toDoubleOrNull() ?: 0.0
    val currentSavings = (income - expense).coerceAtLeast(0.0)

    // Sliders
    var expenseCutPercent by remember { mutableFloatStateOf(15f) } // 0% to 50%
    var targetSavingsGoal by remember { mutableFloatStateOf(100000f) } // $5,000 to $500,000
    var expectedRoi by remember { mutableFloatStateOf(8f) } // 3% to 15%

    var isEditingBaseValues by remember { mutableStateOf(false) }

    val cutAmount = expense * (expenseCutPercent / 100.0)
    val optimizedSavings = currentSavings + cutAmount

    // Projections (project over 10 years / 120 months)
    val projectionMonths = 120
    val currentProjection =
        remember(currentSavings, expectedRoi) {
            calculateCompoundProjections(currentSavings, expectedRoi.toDouble(), projectionMonths)
        }
    val optimizedProjection =
        remember(optimizedSavings, expectedRoi) {
            calculateCompoundProjections(optimizedSavings, expectedRoi.toDouble(), projectionMonths)
        }

    val currentMonthsToGoal = currentProjection.indexOfFirst { it >= targetSavingsGoal }
    val optimizedMonthsToGoal = optimizedProjection.indexOfFirst { it >= targetSavingsGoal }

    val context = LocalContext.current
    val yearLabels =
        remember(context) {
            (0..5).map { i ->
                with(context) { getString(R.string.sandbox_year_suffix, i * 2) }
            }
        }

    val currencySymbol =
        remember(defaultsState.currencyCode) {
            try {
                java.util.Currency
                    .getInstance(defaultsState.currencyCode)
                    .symbol
            } catch (e: Exception) {
                "$"
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.sandbox_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { isEditingBaseValues = !isEditingBaseValues }) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.sandbox_edit_base_values))
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Edit Base Income / Expenses card (expandable)
            AnimatedVisibility(
                visible = isEditingBaseValues,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(200)),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(R.string.sandbox_base_inputs_title, defaultsState.currencyCode),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedTextField(
                                value = incomeInput,
                                onValueChange = { incomeInput = it },
                                label = { Text(stringResource(R.string.sandbox_monthly_income)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.large,
                            )
                            OutlinedTextField(
                                value = expenseInput,
                                onValueChange = { expenseInput = it },
                                label = { Text(stringResource(R.string.sandbox_monthly_expense)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.large,
                            )
                        }
                        Button(
                            onClick = { isEditingBaseValues = false },
                            modifier = Modifier.align(Alignment.End),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(stringResource(R.string.sandbox_done))
                        }
                    }
                }
            }

            // Overview/Base Savings summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BaseMetricCard(
                    title = stringResource(R.string.sandbox_monthly_savings),
                    value = "$currencySymbol${formatDouble(currentSavings)}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                BaseMetricCard(
                    title = stringResource(R.string.sandbox_optimized_savings),
                    value = "$currencySymbol${formatDouble(optimizedSavings)}",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
            }

            // Input Sliders section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Sliders Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.sandbox_scenario_params),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Slider 1: Expense Cut
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.expense_cut),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(
                                    R.string.sandbox_saved_per_month,
                                    "${expenseCutPercent.roundToInt()}%",
                                    "$currencySymbol${formatDouble(cutAmount)}",
                                ),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Slider(
                            value = expenseCutPercent,
                            onValueChange = { expenseCutPercent = it },
                            valueRange = 0f..50f,
                            steps = 9,
                            colors =
                                SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent,
                                ),
                        )
                    }

                    // Slider 2: Target Savings Goal
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.target_savings),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "$currencySymbol${formatDouble(targetSavingsGoal.toDouble())}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Slider(
                            value = targetSavingsGoal,
                            onValueChange = { targetSavingsGoal = it },
                            valueRange = 5000f..500000f,
                            steps = 99,
                            colors =
                                SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.secondary,
                                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent,
                                ),
                        )
                    }

                    // Slider 3: Expected ROI
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.annual_return),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${(expectedRoi * 10).roundToInt() / 10.0}%",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        Slider(
                            value = expectedRoi,
                            onValueChange = { expectedRoi = it },
                            valueRange = 3f..15f,
                            steps = 24,
                            colors =
                                SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.tertiary,
                                    activeTrackColor = MaterialTheme.colorScheme.tertiary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent,
                                ),
                        )
                    }
                }
            }

            // Highlights Results Card
            SandboxResultsCard(
                currentMonths = currentMonthsToGoal,
                optimizedMonths = optimizedMonthsToGoal,
                cutAmount = cutAmount,
                currencySymbol = currencySymbol,
            )

            // Projections Canvas Line Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        stringResource(R.string.sandbox_growth_projection),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    // Legend Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LegendItem(
                            label = stringResource(R.string.current_trajectory),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        LegendItem(
                            label = stringResource(R.string.optimized_trajectory),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Custom Line Chart Canvas
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val labelColorArgb = labelColor.toArgb()
                    val textPaintY = remember(density, labelColorArgb) {
                        android.graphics.Paint().apply {
                            color = labelColorArgb
                            textSize = with(density) { 10.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    }
                    val textPaintX = remember(density, labelColorArgb) {
                        android.graphics.Paint().apply {
                            color = labelColorArgb
                            textSize = with(density) { 10.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    }

                    val currentPath = remember { Path() }
                    val optimizedPath = remember { Path() }
                    val currentFillPath = remember { Path() }
                    val optimizedFillPath = remember { Path() }

                    val lineStroke = remember(density) {
                        Stroke(width = with(density) { 3.dp.toPx() }, cap = StrokeCap.Round)
                    }

                    val dashPathEffect = remember {
                        PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    }

                    Canvas(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                    ) {
                        val width = size.width
                        val height = size.height
                        val paddingLeft = 45.sp.toPx()
                        val paddingBottom = 24.sp.toPx()
                        val chartWidth = width - paddingLeft
                        val chartHeight = height - paddingBottom

                        // Find Max Value for scaling the Y axis
                        val maxVal = (optimizedProjection.maxOrNull() ?: 1.0).coerceAtLeast(targetSavingsGoal.toDouble()) * 1.1

                        // Draw Grid lines
                        val gridCount = 4
                        for (i in 0..gridCount) {
                            val y = chartHeight - (chartHeight * (i.toFloat() / gridCount))
                            drawLine(
                                color = gridColor,
                                start = Offset(paddingLeft, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }

                        // Draw Y Axis labels
                        for (i in 0..gridCount) {
                            val y = chartHeight - (chartHeight * (i.toFloat() / gridCount))
                            val value = maxVal * (i.toFloat() / gridCount)
                            val label = "$currencySymbol${formatCompact(value)}"
                            drawContext.canvas.nativeCanvas.drawText(
                                label,
                                paddingLeft - 6.dp.toPx(),
                                y + 4.dp.toPx(),
                                textPaintY,
                            )
                        }

                        // Draw X Axis labels (Years 0, 2, 4, 6, 8, 10)
                        val yearSteps = 5
                        for (i in 0..yearSteps) {
                            val year = i * 2
                            val x = paddingLeft + (chartWidth * (year.toFloat() / 10))
                            val label = yearLabels.getOrElse(i) { "${year}Y" }
                            drawContext.canvas.nativeCanvas.drawText(
                                label,
                                x,
                                height - 4.dp.toPx(),
                                textPaintX,
                            )
                        }

                        // Generate Paths for plotting
                        currentPath.reset()
                        optimizedPath.reset()

                        currentProjection.forEachIndexed { index, balance ->
                            val x = paddingLeft + (chartWidth * (index.toFloat() / (currentProjection.size - 1)))
                            val y = chartHeight - (chartHeight * (balance / maxVal).toFloat())
                            if (index == 0) {
                                currentPath.moveTo(x, y)
                            } else {
                                currentPath.lineTo(x, y)
                            }
                        }

                        optimizedProjection.forEachIndexed { index, balance ->
                            val x = paddingLeft + (chartWidth * (index.toFloat() / (optimizedProjection.size - 1)))
                            val y = chartHeight - (chartHeight * (balance / maxVal).toFloat())
                            if (index == 0) {
                                optimizedPath.moveTo(x, y)
                            } else {
                                optimizedPath.lineTo(x, y)
                            }
                        }

                        // 1. Draw gradient filled paths under curves (premium shading)
                        currentFillPath.reset()
                        currentFillPath.addPath(currentPath)
                        currentFillPath.lineTo(paddingLeft + chartWidth, chartHeight)
                        currentFillPath.lineTo(paddingLeft, chartHeight)
                        currentFillPath.close()

                        drawPath(
                            path = currentFillPath,
                            brush =
                                Brush.verticalGradient(
                                    colors = listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent),
                                    startY = 0f,
                                    endY = chartHeight,
                                ),
                        )

                        optimizedFillPath.reset()
                        optimizedFillPath.addPath(optimizedPath)
                        optimizedFillPath.lineTo(paddingLeft + chartWidth, chartHeight)
                        optimizedFillPath.lineTo(paddingLeft, chartHeight)
                        optimizedFillPath.close()

                        drawPath(
                            path = optimizedFillPath,
                            brush =
                                Brush.verticalGradient(
                                    colors = listOf(secondaryColor.copy(alpha = 0.25f), Color.Transparent),
                                    startY = 0f,
                                    endY = chartHeight,
                                ),
                        )

                        // 2. Draw lines on top of fills
                        drawPath(
                            path = currentPath,
                            color = primaryColor,
                            style = lineStroke,
                        )
                        drawPath(
                            path = optimizedPath,
                            color = secondaryColor,
                            style = lineStroke,
                        )

                        // Draw Target savings goal dashed line
                        val targetY = chartHeight - (chartHeight * (targetSavingsGoal / maxVal).toFloat())
                        drawLine(
                            color = Color(0xFFE5A93C),
                            start = Offset(paddingLeft, targetY),
                            end = Offset(width, targetY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = dashPathEffect,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BaseMetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.08f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color,
            )
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SandboxResultsCard(
    currentMonths: Int,
    optimizedMonths: Int,
    cutAmount: Double,
    currencySymbol: String,
) {
    val showBanner = currentMonths > 0 || optimizedMonths > 0

    if (!showBanner) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                ),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = stringResource(R.string.sandbox_goal_too_high),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF8E24AA),
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.sandbox_insights),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

            // Text output details
            val pathText =
                when {
                    optimizedMonths > 0 && currentMonths > 0 -> {
                        val savedMonths = currentMonths - optimizedMonths
                        if (savedMonths > 0) {
                            stringResource(
                                R.string.sandbox_summary,
                                formatMonths(savedMonths),
                                "$currencySymbol${formatDouble(cutAmount)}",
                            )
                        } else {
                            stringResource(R.string.sandbox_similar_timeframe)
                        }
                    }
                    optimizedMonths > 0 && currentMonths < 0 -> {
                        stringResource(R.string.sandbox_optimized_path_reaches, formatMonths(optimizedMonths))
                    }
                    else -> {
                        stringResource(R.string.sandbox_try_cutting_expenses)
                    }
                }

            Text(
                text = pathText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

private fun calculateCompoundProjections(
    monthlyContribution: Double,
    annualRate: Double,
    months: Int,
): List<Double> {
    val monthlyRate = annualRate / 12.0 / 100.0
    var balance = 0.0
    val points = mutableListOf<Double>()
    points.add(balance)

    for (m in 1..months) {
        balance = (balance + monthlyContribution) * (1.0 + monthlyRate)
        points.add(balance)
    }
    return points
}

@Composable
private fun formatMonths(months: Int): String =
    if (months >= 12) {
        val years = months / 12
        val remaining = months % 12
        val yearsStr =
            if (years == 1) {
                stringResource(R.string.year_singular)
            } else {
                stringResource(R.string.years_plural, years)
            }
        if (remaining > 0) {
            val monthsStr =
                if (remaining == 1) {
                    stringResource(R.string.month_singular)
                } else {
                    stringResource(R.string.months_plural, remaining)
                }
            stringResource(R.string.years_and_months, yearsStr, monthsStr)
        } else {
            yearsStr
        }
    } else {
        if (months == 1) {
            stringResource(R.string.month_singular)
        } else {
            stringResource(R.string.months_plural, months)
        }
    }

private fun formatDouble(value: Double): String = String.format(Locale.US, "%,.2f", value)

private fun formatCompact(value: Double): String =
    when {
        value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000)
        value >= 1_000 -> String.format(Locale.US, "%.0fK", value / 1_000)
        else -> String.format(Locale.US, "%.0f", value)
    }
