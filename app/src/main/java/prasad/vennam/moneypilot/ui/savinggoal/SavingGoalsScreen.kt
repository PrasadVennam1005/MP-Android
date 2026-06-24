package prasad.vennam.moneypilot.ui.savinggoal

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.SavingGoal
import prasad.vennam.moneypilot.ui.viewmodel.SavingGoalViewModel
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.TrackScreen
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import prasad.vennam.moneypilot.util.inRupees
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingGoalsScreen(
    onBackClick: () -> Unit,
    analyticsHelper: AnalyticsHelper,
    viewModel: SavingGoalViewModel = hiltViewModel()
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.SAVING_GOALS)
    val goals by viewModel.allSavingGoals.collectAsState()
    val currencyCode = LocalCurrencyCode.current

    var showFormSheet by remember { mutableStateOf(false) }
    var selectedGoalForEdit by remember { mutableStateOf<SavingGoal?>(null) }
    
    var showGoalDetailSheet by remember { mutableStateOf(false) }
    var selectedGoalForDetail by remember { mutableStateOf<SavingGoal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Savings Goals",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedGoalForEdit = null
                    showFormSheet = true
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Goal")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (goals.isEmpty()) {
                EmptyGoalsState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(goals, key = { it.id }) { goal ->
                        SavingGoalCard(
                            goal = goal,
                            currencyCode = currencyCode,
                            onClick = {
                                selectedGoalForDetail = goal
                                showGoalDetailSheet = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFormSheet) {
        SavingGoalFormBottomSheet(
            goal = selectedGoalForEdit,
            onDismiss = { showFormSheet = false },
            onSave = { name, target, currentSaved, deadline, color, icon ->
                val id = selectedGoalForEdit?.id ?: 0L
                val eventName = if (id == 0L) {
                    AnalyticsConstants.Event.SAVING_GOAL_ADDED
                } else {
                    AnalyticsConstants.Event.SAVING_GOAL_UPDATED
                }
                analyticsHelper.logEvent(eventName, mapOf(
                    AnalyticsConstants.Param.GOAL_NAME to name,
                    AnalyticsConstants.Param.GOAL_TARGET to target.inRupees,
                    AnalyticsConstants.Param.GOAL_SAVED to currentSaved.inRupees,
                    AnalyticsConstants.Param.GOAL_DEADLINE to deadline
                ))
                val updated = SavingGoal(
                    id = id,
                    name = name,
                    targetAmount = target,
                    currentSavedAmount = currentSaved,
                    deadline = deadline,
                    colorHex = color,
                    iconName = icon
                )
                viewModel.saveSavingGoal(updated)
                showFormSheet = false
            }
        )
    }

    if (showGoalDetailSheet && selectedGoalForDetail != null) {
        // Re-fetch current state of goal from the list in case of updates
        val currentGoal = goals.find { it.id == selectedGoalForDetail!!.id } ?: selectedGoalForDetail!!
        
        SavingGoalDetailBottomSheet(
            goal = currentGoal,
            currencyCode = currencyCode,
            onDismiss = { showGoalDetailSheet = false },
            onEdit = {
                selectedGoalForEdit = currentGoal
                showGoalDetailSheet = false
                showFormSheet = true
            },
            onDelete = {
                analyticsHelper.logEvent(
                    AnalyticsConstants.Event.SAVING_GOAL_DELETED,
                    mapOf(
                        AnalyticsConstants.Param.GOAL_NAME to currentGoal.name,
                        AnalyticsConstants.Param.GOAL_TARGET to currentGoal.targetAmount.inRupees
                    )
                )
                viewModel.deleteSavingGoal(currentGoal)
                showGoalDetailSheet = false
            },
            onDeposit = { amount ->
                analyticsHelper.logEvent(
                    AnalyticsConstants.Event.SAVING_GOAL_DEPOSIT,
                    mapOf(
                        AnalyticsConstants.Param.GOAL_NAME to currentGoal.name,
                        AnalyticsConstants.Param.DEPOSIT_AMOUNT to amount.inRupees
                    )
                )
                val newSaved = currentGoal.currentSavedAmount + amount
                if (newSaved >= currentGoal.targetAmount && !currentGoal.isCompleted) {
                    analyticsHelper.logEvent(
                        AnalyticsConstants.Event.SAVING_GOAL_COMPLETED,
                        mapOf(
                            AnalyticsConstants.Param.GOAL_NAME to currentGoal.name,
                            AnalyticsConstants.Param.GOAL_TARGET to currentGoal.targetAmount.inRupees
                        )
                    )
                }
                viewModel.depositToGoal(currentGoal, amount)
            },
            onWithdraw = { amount ->
                analyticsHelper.logEvent(
                    AnalyticsConstants.Event.SAVING_GOAL_WITHDRAWAL,
                    mapOf(
                        AnalyticsConstants.Param.GOAL_NAME to currentGoal.name,
                        AnalyticsConstants.Param.WITHDRAWAL_AMOUNT to amount.inRupees
                    )
                )
                viewModel.withdrawFromGoal(currentGoal, amount)
            }
        )
    }
}

@Composable
fun EmptyGoalsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.TrackChanges,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Create Savings Goals",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Save up for a new laptop, dream vacation, or property milestones. Track your progress with visual progress meters.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun SavingGoalCard(
    goal: SavingGoal,
    currencyCode: String,
    onClick: () -> Unit
) {
    val themeColor = remember(goal.colorHex) {
        try { Color(android.graphics.Color.parseColor(goal.colorHex)) } catch (e: Exception) { Color(0xFF3F51B5) }
    }
    val icon = remember(goal.iconName) {
        when (goal.iconName) {
            "Home" -> Icons.Rounded.Home
            "Car" -> Icons.Rounded.DirectionsCar
            "Flight" -> Icons.Rounded.Flight
            "Laptop" -> Icons.Rounded.Laptop
            "Savings" -> Icons.Rounded.Savings
            else -> Icons.Rounded.Savings
        }
    }
    val percentAchieved = remember(goal.currentSavedAmount, goal.targetAmount) {
        if (goal.targetAmount > 0) {
            ((goal.currentSavedAmount.toDouble() / goal.targetAmount.toDouble()) * 100).toFloat().coerceAtMost(100f)
        } else 0f
    }
    
    val currentSavedFormatted = remember(goal.currentSavedAmount, currencyCode) {
        CurrencyFormatter.format(goal.currentSavedAmount.inRupees, currencyCode)
    }
    val targetGoalFormatted = remember(goal.targetAmount, currencyCode) {
        CurrencyFormatter.format(goal.targetAmount.inRupees, currencyCode)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$currentSavedFormatted saved of $targetGoalFormatted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    color = if (goal.isCompleted) Color(0xFF2E7D32).copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (goal.isCompleted) "Completed" else "${percentAchieved.toInt()}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (goal.isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Linear Progress Bar
            val animatedPercent by animateFloatAsState(
                targetValue = percentAchieved / 100f,
                animationSpec = tween(durationMillis = 800)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedPercent)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(themeColor.copy(alpha = 0.7f), themeColor)
                            )
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingGoalFormBottomSheet(
    goal: SavingGoal?,
    onDismiss: () -> Unit,
    onSave: (name: String, target: Long, currentSaved: Long, deadline: Long, color: String, icon: String) -> Unit
) {
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var targetStr by remember {
        mutableStateOf(if (goal != null) (goal.targetAmount / 100.0).toString() else "")
    }
    var currentSavedStr by remember {
        mutableStateOf(if (goal != null) (goal.currentSavedAmount / 100.0).toString() else "0")
    }
    var deadline by remember { mutableStateOf(goal?.deadline ?: (System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000L)) }
    var selectedColorHex by remember { mutableStateOf(goal?.colorHex ?: "#3F51B5") }
    var selectedIconName by remember { mutableStateOf(goal?.iconName ?: "Savings") }

    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()) }

    val colors = listOf("#3F51B5", "#2E7D32", "#FF9800", "#9C27B0", "#009688", "#E91E63")
    val icons = listOf("Savings", "Home", "Car", "Flight", "Laptop")

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (goal == null) "Create Savings Goal" else "Edit Savings Goal",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Goal Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = { Text("Target Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = currentSavedStr,
                    onValueChange = { currentSavedStr = it },
                    label = { Text("Initially Saved (₹)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // Deadline Selection
            Text("Target Deadline", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Surface(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        MaterialTheme.shapes.large
                    ),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = dateFormatter.format(Date(deadline)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Color Selection
            Text("Goal Theme Color", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                colors.forEach { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val selected = selectedColorHex == hex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedColorHex = hex }
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Icon Selection
            Text("Goal Icon", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                icons.forEach { iconName ->
                    val icon = when (iconName) {
                        "Home" -> Icons.Rounded.Home
                        "Car" -> Icons.Rounded.DirectionsCar
                        "Flight" -> Icons.Rounded.Flight
                        "Laptop" -> Icons.Rounded.Laptop
                        "Savings" -> Icons.Rounded.Savings
                        else -> Icons.Rounded.Savings
                    }
                    val selected = selectedIconName == iconName
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedIconName = iconName },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val targetVal = (targetStr.toDoubleOrNull() ?: 0.0) * 100
                    val currentVal = (currentSavedStr.toDoubleOrNull() ?: 0.0) * 100
                    if (name.isNotBlank() && targetVal > 0) {
                        onSave(
                            name,
                            targetVal.toLong(),
                            currentVal.toLong(),
                            deadline,
                            selectedColorHex,
                            selectedIconName
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(top = 8.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Create Goal")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = deadline)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadline = datePickerState.selectedDateMillis ?: deadline
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingGoalDetailBottomSheet(
    goal: SavingGoal,
    currencyCode: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeposit: (Long) -> Unit,
    onWithdraw: (Long) -> Unit
) {
    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    
    val themeColor = remember(goal.colorHex) {
        try { Color(android.graphics.Color.parseColor(goal.colorHex)) } catch (e: Exception) { Color(0xFF3F51B5) }
    }
    val percentAchieved = remember(goal.currentSavedAmount, goal.targetAmount) {
        if (goal.targetAmount > 0) {
            ((goal.currentSavedAmount.toDouble() / goal.targetAmount.toDouble()) * 100).toFloat().coerceAtMost(100f)
        } else 0f
    }
    val remaining = remember(goal.targetAmount, goal.currentSavedAmount) {
        (goal.targetAmount - goal.currentSavedAmount).coerceAtLeast(0L)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Title row with Edit and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Gauge / Circular visualizer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                val animatedPercent by animateFloatAsState(
                    targetValue = percentAchieved,
                    animationSpec = tween(durationMillis = 1000)
                )

                Canvas(modifier = Modifier.size(150.dp)) {
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(themeColor.copy(alpha = 0.5f), themeColor)),
                        startAngle = -90f,
                        sweepAngle = (animatedPercent / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${percentAchieved.toInt()}%",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 34.sp),
                        color = themeColor
                    )
                    Text(
                        text = "achieved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Celebratory Banner if 100%
            if (goal.isCompleted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Celebration,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Congratulations! You have fully funded this goal!",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Progress stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Saved", style = MaterialTheme.typography.labelMedium)
                        Text(
                            CurrencyFormatter.format(goal.currentSavedAmount.inRupees, currencyCode),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = themeColor
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Target", style = MaterialTheme.typography.labelMedium)
                        Text(
                            CurrencyFormatter.format(goal.targetAmount.inRupees, currencyCode),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            if (remaining > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Remaining", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                            Text(
                                CurrencyFormatter.format(remaining.inRupees, currencyCode),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons (Deposit / Withdraw)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { showWithdrawDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Withdraw")
                }
                Button(
                    onClick = { showDepositDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Deposit")
                }
            }
        }
    }

    if (showDepositDialog) {
        FundGoalDialog(
            isDeposit = true,
            themeColor = themeColor,
            onDismiss = { showDepositDialog = false },
            onConfirm = { amount ->
                onDeposit(amount)
                showDepositDialog = false
            }
        )
    }

    if (showWithdrawDialog) {
        FundGoalDialog(
            isDeposit = false,
            themeColor = themeColor,
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { amount ->
                onWithdraw(amount)
                showWithdrawDialog = false
            }
        )
    }
}

@Composable
fun FundGoalDialog(
    isDeposit: Boolean,
    themeColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isDeposit) "Deposit Funds" else "Withdraw Funds", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isDeposit) "Enter the amount you want to virtually add to this goal."
                    else "Enter the amount you want to virtually retrieve from this goal."
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = (amountStr.toDoubleOrNull() ?: 0.0) * 100
                    if (amountVal > 0) {
                        onConfirm(amountVal.toLong())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
