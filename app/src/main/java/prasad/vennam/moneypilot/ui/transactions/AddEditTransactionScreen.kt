package prasad.vennam.moneypilot.ui.transactions

import prasad.vennam.moneypilot.ui.components.BaseBottomSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.ui.budget.utils.getCategoryIcon
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import prasad.vennam.moneypilot.util.TrackScreen
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: Long? = null,
    initialType: TransactionType = TransactionType.EXPENSE,
    viewModel: TransactionViewModel,
    analyticsHelper: AnalyticsHelper,
    interstitialAdManager: prasad.vennam.moneypilot.ads.InterstitialAdManager,
    isPremium: Boolean,
    onNavigateBack: () -> Unit,
) {
    TrackScreen(
        analyticsHelper,
        if (transactionId == null) AnalyticsConstants.Screen.ADD_TRANSACTION else AnalyticsConstants.Screen.EDIT_TRANSACTION,
    )
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    val currencyCode = LocalCurrencyCode.current

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showPaymentMenu by remember { mutableStateOf(false) }

    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val filteredCategories =
        remember(categories, formState.type) {
            categories.filter { it.isExpense == (formState.type == TransactionType.EXPENSE) }
        }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val locale = androidx.compose.ui.platform.LocalLocale.current.platformLocale
    val dateFormatter = remember(locale) { SimpleDateFormat("dd MMM, yyyy", locale) }

    LaunchedEffect(transactionId) {
        if (transactionId != null && transactionId != 0L) {
            viewModel.loadTransactionForEdit(transactionId)
        } else {
            viewModel.resetFormState()
            viewModel.updateType(initialType)
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (transactionId == null) {
                            stringResource(
                                R.string.add_transaction_type,
                                formState.type.name
                                    .lowercase()
                                    .replaceFirstChar { it.uppercase() },
                            )
                        } else {
                            stringResource(R.string.edit_entry)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Type Toggle
            if (formState.loanPaymentId != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "This transaction is linked to a loan payment. Some fields are restricted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            } else {
                PremiumToggle(
                    selectedType = formState.type,
                    onTypeSelected = {
                        viewModel.updateType(it)
                    },
                )
            }

            val amountVal = formState.amount.toDoubleOrNull()
            val isAmountError = formState.amount.isNotEmpty() && (amountVal == null || amountVal <= 0.0 || amountVal > 100000000.0)

            PremiumAmountField(
                value = formState.amount,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        viewModel.updateAmount(input)
                    }
                },
                color =
                    if (formState.type ==
                        TransactionType.INCOME
                    ) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                isError = isAmountError,
                supportingText =
                    if (isAmountError) {
                        {
                            val text =
                                when {
                                    amountVal == null -> context.getString(R.string.invalid_format)
                                    amountVal <= 0.0 -> stringResource(R.string.amount_error_desc)
                                    else -> context.getString(R.string.amount_cannot_exceed)
                                }
                            Text(text)
                        }
                    } else {
                        null
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
            )

            // Date Picker Field
            PremiumReadOnlyField(
                label = stringResource(R.string.date),
                value = dateFormatter.format(Date(formState.timestamp)),
                icon = Icons.Rounded.CalendarToday,
                onClick = { showDatePicker = true },
            )

            // Category Field
            PremiumReadOnlyField(
                label = stringResource(R.string.category),
                value = categories.find { it.id == formState.categoryId }?.name ?: stringResource(R.string.select_category),
                icon = Icons.Rounded.Category,
                onClick = { showCategoryMenu = true },
            )

            PremiumTextField(
                label = stringResource(R.string.sub_category),
                value = formState.subCategory,
                onValueChange = { viewModel.updateSubCategory(it) },
                icon = Icons.Rounded.Subtitles,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
            )

            // Payment Mode Field
            PremiumReadOnlyField(
                label = stringResource(R.string.payment_mode),
                value = formState.paymentMode,
                icon = Icons.Rounded.Payments,
                onClick = { showPaymentMenu = true },
            )

            PremiumTextField(
                label = stringResource(R.string.notes),
                value = formState.note,
                onValueChange = { viewModel.updateNote(it) },
                icon = Icons.Rounded.EditNote,
                singleLine = false,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            )

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = {
                    val amountValue = formState.amount.toDoubleOrNull() ?: return@Button
                    if (amountValue <= 0) return@Button

                    val categoryName = categories.find { it.id == formState.categoryId }?.name ?: context.getString(R.string.unknown)

                    // Analytics: Track successful add/edit only after validation passes
                    analyticsHelper.logEvent(
                        AnalyticsConstants.Event.TRANSACTION_ADDED,
                        mapOf(
                            AnalyticsConstants.Param.TYPE to formState.type.name,
                            AnalyticsConstants.Param.CATEGORY to categoryName,
                            AnalyticsConstants.Param.PAYMENT_MODE to formState.paymentMode,
                            AnalyticsConstants.Param.IS_EDIT to (transactionId != null),
                        ),
                    )

                    val transaction =
                        Transaction(
                            id = transactionId ?: 0L,
                            amount = (amountValue * 100).toLong(),
                            note = formState.note,
                            timestamp = formState.timestamp,
                            type = formState.type,
                            categoryId = formState.categoryId,
                            subCategory = formState.subCategory,
                            paymentMode = formState.paymentMode,
                            currencyCode = currencyCode,
                        )
                    viewModel.saveTransaction(transaction)

                    val activity = context as? android.app.Activity
                    if (activity != null) {
                        interstitialAdManager.showAdIfNeeded(activity, isPremium) {
                            onNavigateBack()
                        }
                    } else {
                        onNavigateBack()
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                enabled = formState.amount.isNotBlank() && !isAmountError && formState.categoryId != null,
                shape = MaterialTheme.shapes.extraLarge,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.save_transaction), style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // Modals
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = formState.timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateTimestamp(it) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showCategoryMenu) {
        BaseBottomSheet(
            onDismissRequest = { showCategoryMenu = false },
            title = stringResource(R.string.select_category),
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                items(filteredCategories.size) { index ->
                    val category = filteredCategories[index]
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text(category.name) },
                        leadingContent = { CategoryIcon(category) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateCategory(category.id)
                                showCategoryMenu = false
                            }
                    )
                }
            }
        }
    }

    if (showPaymentMenu) {
        val modes = prasad.vennam.moneypilot.util.PaymentModes.ALL_MODES

        BaseBottomSheet(onDismissRequest = { showPaymentMenu = false }, title = stringResource(R.string.payment_mode)) {
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                items(modes.size) { index ->
                    val mode = modes[index]
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text(mode.name) },
                        leadingContent = { Icon(mode.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updatePaymentMode(mode.name)
                                showPaymentMenu = false
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumToggle(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selectedType == TransactionType.EXPENSE,
            onClick = { onTypeSelected(TransactionType.EXPENSE) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            colors =
                SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.errorContainer,
                    activeContentColor = MaterialTheme.colorScheme.error,
                ),
        ) { Text(stringResource(R.string.expense)) }
        SegmentedButton(
            selected = selectedType == TransactionType.INCOME,
            onClick = { onTypeSelected(TransactionType.INCOME) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors =
                SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.primary,
                ),
        ) { Text(stringResource(R.string.income)) }
    }
}

@Composable
fun PremiumAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    color: Color,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val displayColor = if (isError) MaterialTheme.colorScheme.error else color
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = displayColor.copy(alpha = 0.05f)),
        shape = MaterialTheme.shapes.extraLarge,
        border = androidx.compose.foundation.BorderStroke(1.dp, displayColor.copy(alpha = 0.2f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.amount), style = MaterialTheme.typography.labelMedium, color = displayColor.copy(alpha = 0.6f))
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("0.00", color = displayColor.copy(alpha = 0.3f)) },
                textStyle =
                    MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = displayColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    ),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                isError = isError,
                supportingText = supportingText,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                modifier = Modifier.fillMaxWidth(),
                prefix = {
                    Text(
                        java.util.Currency
                            .getInstance(LocalCurrencyCode.current)
                            .symbol,
                        style = MaterialTheme.typography.displayMedium.copy(color = displayColor),
                    )
                },
            )
        }
    }
}

@Composable
fun PremiumReadOnlyField(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun PremiumTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors =
            OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            ),
    )
}

@Composable
fun CategoryIcon(category: prasad.vennam.moneypilot.data.entity.Category) {
    Surface(
        color = Color(category.color).copy(alpha = 0.2f),
        shape = CircleShape,
        modifier = Modifier.size(32.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            val icon = getCategoryIcon(category.iconName)
            Icon(icon, contentDescription = null, tint = Color(category.color), modifier = Modifier.size(16.dp))
        }
    }
}
