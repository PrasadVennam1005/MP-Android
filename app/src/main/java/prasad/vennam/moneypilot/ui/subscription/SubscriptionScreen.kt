package prasad.vennam.moneypilot.ui.subscription

import prasad.vennam.moneypilot.ui.components.BaseBottomSheet
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Subscription
import prasad.vennam.moneypilot.ui.budget.utils.getCategoryIcon
import prasad.vennam.moneypilot.ui.viewmodel.SubscriptionViewModel
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import prasad.vennam.moneypilot.util.TrackScreen
import prasad.vennam.moneypilot.util.toMajorUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBackClick: () -> Unit,
    analyticsHelper: AnalyticsHelper,
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.SUBSCRIPTIONS)
    val subscriptions by viewModel.allSubscriptions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val currencyCode = LocalCurrencyCode.current

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    var isFabVisible by remember { mutableStateOf(true) }
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset, lazyGridState.firstVisibleItemIndex, lazyGridState.firstVisibleItemScrollOffset) {
        val currentIndex = if (isExpanded) lazyGridState.firstVisibleItemIndex else lazyListState.firstVisibleItemIndex
        val currentOffset = if (isExpanded) lazyGridState.firstVisibleItemScrollOffset else lazyListState.firstVisibleItemScrollOffset
        if (currentIndex == 0 && currentOffset == 0) {
            isFabVisible = true
        } else if (currentIndex > previousIndex || (currentIndex == previousIndex && currentOffset > previousOffset)) {
            isFabVisible = false
        } else if (currentIndex < previousIndex || (currentOffset < previousOffset)) {
            isFabVisible = true
        }
        previousIndex = currentIndex
        previousOffset = currentOffset
    }

    var showFormSheet by remember { mutableStateOf(false) }
    var selectedSubscriptionForEdit by remember { mutableStateOf<Subscription?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.subscriptions),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
        floatingActionButton = {
            if (isFabVisible) {
                FloatingActionButton(
                    onClick = {
                        selectedSubscriptionForEdit = null
                        showFormSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_subscription))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            if (subscriptions.isEmpty()) {
                EmptySubscriptionsState()
            } else if (isExpanded) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = lazyGridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(subscriptions, key = { it.id }) { subscription ->
                        SubscriptionCard(
                            subscription = subscription,
                            category = categories.find { it.id == subscription.categoryId },
                            currencyCode = currencyCode,
                            onEdit = {
                                selectedSubscriptionForEdit = subscription
                                showFormSheet = true
                            },
                            onDelete = {
                                analyticsHelper.logEvent(
                                    AnalyticsConstants.Event.SUBSCRIPTION_DELETED,
                                    mapOf(
                                        AnalyticsConstants.Param.SUBSCRIPTION_NAME to subscription.name,
                                        AnalyticsConstants.Param.SUBSCRIPTION_AMOUNT to subscription.amount.toMajorUnit,
                                    ),
                                )
                                viewModel.deleteSubscription(subscription)
                            },
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(subscriptions, key = { it.id }) { subscription ->
                        SubscriptionCard(
                            subscription = subscription,
                            category = categories.find { it.id == subscription.categoryId },
                            currencyCode = currencyCode,
                            onEdit = {
                                selectedSubscriptionForEdit = subscription
                                showFormSheet = true
                            },
                            onDelete = {
                                analyticsHelper.logEvent(
                                    AnalyticsConstants.Event.SUBSCRIPTION_DELETED,
                                    mapOf(
                                        AnalyticsConstants.Param.SUBSCRIPTION_NAME to subscription.name,
                                        AnalyticsConstants.Param.SUBSCRIPTION_AMOUNT to subscription.amount.toMajorUnit,
                                    ),
                                )
                                viewModel.deleteSubscription(subscription)
                            },
                        )
                    }
                }
            }
        }
    }

    if (showFormSheet) {
        SubscriptionFormBottomSheet(
            subscription = selectedSubscriptionForEdit,
            categories = categories.filter { it.isExpense },
            onDismiss = { showFormSheet = false },
            onSave = { name, amount, cycle, date, mode, categoryId, notify ->
                val id = selectedSubscriptionForEdit?.id ?: 0L
                val eventName =
                    if (id == 0L) {
                        AnalyticsConstants.Event.SUBSCRIPTION_ADDED
                    } else {
                        AnalyticsConstants.Event.SUBSCRIPTION_UPDATED
                    }
                analyticsHelper.logEvent(
                    eventName,
                    mapOf(
                        AnalyticsConstants.Param.SUBSCRIPTION_NAME to name,
                        AnalyticsConstants.Param.SUBSCRIPTION_AMOUNT to amount.toMajorUnit,
                        AnalyticsConstants.Param.SUBSCRIPTION_CYCLE to cycle,
                        AnalyticsConstants.Param.SUBSCRIPTION_MODE to mode,
                        AnalyticsConstants.Param.SUBSCRIPTION_NOTIFY to notify,
                    ),
                )
                val sub =
                    Subscription(
                        id = id,
                        name = name,
                        amount = amount,
                        billingCycle = cycle,
                        nextPaymentDate = date,
                        paymentMode = mode,
                        categoryId = categoryId,
                        isNotificationEnabled = notify,
                    )
                viewModel.saveSubscription(sub)
                showFormSheet = false
            },
        )
    }
}

@Composable
fun EmptySubscriptionsState() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.track_recurring_subscriptions),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.subscriptions_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
fun SubscriptionCard(
    subscription: Subscription,
    category: Category?,
    currencyCode: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
    val daysRemaining =
        remember(subscription.nextPaymentDate) {
            val diffMs = subscription.nextPaymentDate - System.currentTimeMillis()
            val days = (diffMs / (24 * 60 * 60 * 1000L)).toInt()
            if (days < 0) 0 else days
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Category Icon
            Surface(
                color = if (category != null) Color(category.color).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon =
                        remember(category?.iconName) {
                            getCategoryIcon(category?.iconName)
                        }
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (category != null) Color(category.color) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val cycleDisplay = when (subscription.billingCycle) {
                    "Weekly" -> stringResource(R.string.weekly)
                    "Monthly" -> stringResource(R.string.monthly)
                    "Yearly" -> stringResource(R.string.yearly)
                    else -> subscription.billingCycle
                }
                Text(
                    text = stringResource(R.string.next_payment, dateFormatter.format(Date(subscription.nextPaymentDate)), cycleDisplay),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            text = subscription.paymentMode,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }

                    val badgeColor = if (daysRemaining <= 2) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                    val badgeTextColor = if (daysRemaining <= 2) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    Surface(
                        color = badgeColor,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            text =
                                when (daysRemaining) {
                                    0 -> stringResource(R.string.due_today)
                                    1 -> stringResource(R.string.due_tomorrow)
                                    else -> stringResource(R.string.in_days, daysRemaining)
                                },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = badgeTextColor,
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val formattedAmount = CurrencyFormatter.format(subscription.amount.toMajorUnit, currencyCode)
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionFormBottomSheet(
    subscription: Subscription?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Long, cycle: String, date: Long, mode: String, categoryId: Long?, notify: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(subscription?.name ?: "") }
    var amountStr by remember {
        mutableStateOf(if (subscription != null) (subscription.amount / 100.0).toString() else "")
    }
    var billingCycle by remember { mutableStateOf(subscription?.billingCycle ?: "Monthly") }
    var nextPaymentDate by remember { mutableStateOf(subscription?.nextPaymentDate ?: System.currentTimeMillis()) }
    var paymentMode by remember { mutableStateOf(subscription?.paymentMode ?: "UPI") }
    var selectedCategoryId by remember { mutableStateOf(subscription?.categoryId) }
    var isNotificationEnabled by remember { mutableStateOf(subscription?.isNotificationEnabled ?: true) }

    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    BaseBottomSheet(
        onDismissRequest = onDismiss,
        title = if (subscription == null) stringResource(R.string.add_subscription) else stringResource(R.string.edit_subscription),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.subscription_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }),
            )

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text(stringResource(R.string.amount)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }),
            )

            // Billing Cycle Select
            Text(stringResource(R.string.billing_cycle), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf("Weekly", "Monthly", "Yearly").forEach { cycle ->
                    val selected = billingCycle == cycle
                    val cycleDisplay = when (cycle) {
                        "Weekly" -> stringResource(R.string.weekly)
                        "Monthly" -> stringResource(R.string.monthly)
                        "Yearly" -> stringResource(R.string.yearly)
                        else -> cycle
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { billingCycle = cycle },
                        label = { Text(cycleDisplay) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Next Due Date Selection Box
            Text(stringResource(R.string.next_payment_due_date), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Surface(
                onClick = { showDatePicker = true },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            MaterialTheme.shapes.large,
                        ),
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = dateFormatter.format(Date(nextPaymentDate)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Payment Mode Selection
            Text(stringResource(R.string.payment_mode), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val modes = prasad.vennam.moneypilot.util.PaymentModes.ALL
                items(modes) { mode ->
                    val selected = paymentMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { paymentMode = mode },
                        label = { Text(mode) },
                    )
                }
            }

            // Category Selection
            Text(stringResource(R.string.category_label), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(categories) { category ->
                    val selected = selectedCategoryId == category.id
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategoryId = category.id },
                        label = { Text(category.name) },
                    )
                }
            }

            // Notification Reminder Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.enable_alert_reminder), fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = isNotificationEnabled,
                    onCheckedChange = { isNotificationEnabled = it },
                )
            }

            Button(
                onClick = {
                    val amountVal = (amountStr.toDoubleOrNull() ?: 0.0) * 100
                    if (name.isNotBlank() && amountVal > 0) {
                        onSave(
                            name,
                            amountVal.toLong(),
                            billingCycle,
                            nextPaymentDate,
                            paymentMode,
                            selectedCategoryId,
                            isNotificationEnabled,
                        )
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(top = 8.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(stringResource(R.string.save_subscription))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = nextPaymentDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    nextPaymentDate = datePickerState.selectedDateMillis ?: nextPaymentDate
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
