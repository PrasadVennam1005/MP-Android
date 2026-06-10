package prasad.vennam.moneypilot.ui.transactions

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
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
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.ui.budget.utils.getCategoryIcon
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: Long? = null,
    initialType: TransactionType = TransactionType.EXPENSE,
    viewModel: TransactionViewModel,
    analyticsHelper: AnalyticsHelper,
    onNavigateBack: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(initialType) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var subCategory by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Cash") }
    var timestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    val currencyCode = LocalCurrencyCode.current

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showPaymentMenu by remember { mutableStateOf(false) }

    val categories by viewModel.allCategories.collectAsState()
    val filteredCategories =
        remember(categories, type) {
            categories.filter { it.isExpense == (type == TransactionType.EXPENSE) }
        }

    val locale = androidx.compose.ui.platform.LocalLocale.current.platformLocale
    val dateFormatter = remember(locale) { SimpleDateFormat("dd MMM, yyyy", locale) }

    LaunchedEffect(transactionId) {
        if (transactionId != null && transactionId != 0L) {
            val transaction = viewModel.getTransactionById(transactionId)
            transaction?.let {
                amount = (it.amount / 100.0).let { value -> if (value % 1 == 0.0) value.toLong().toString() else value.toString() }
                note = it.note
                type = it.type
                categoryId = it.categoryId
                subCategory = it.subCategory
                paymentMode = it.paymentMode
                timestamp = it.timestamp
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (transactionId == null) {
                            stringResource(R.string.add_transaction_type, type.name.lowercase().replaceFirstChar { it.uppercase() })
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
            PremiumToggle(
                selectedType = type,
                onTypeSelected = {
                    type = it
                    categoryId = null // Reset category when type changes
                },
            )

            // Amount Field
            PremiumAmountField(
                value = amount,
                onValueChange = { input ->
                if (input.isEmpty() || (input.toDoubleOrNull() != null && input.toDouble() >= 0)) {
                    amount = input
                }
            },
            color = if (type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )

            // Date Picker Field
            PremiumReadOnlyField(
                label = "Date",
                value = dateFormatter.format(Date(timestamp)),
                icon = Icons.Rounded.CalendarToday,
                onClick = { showDatePicker = true },
            )

            // Category Field
            PremiumReadOnlyField(
                label = "Category",
                value = categories.find { it.id == categoryId }?.name ?: stringResource(R.string.select_category),
                icon = Icons.Rounded.Category,
                onClick = { showCategoryMenu = true },
            )

            // Sub-Category Field (Optional)
            PremiumTextField(
                label = "Sub Category",
                value = subCategory,
                onValueChange = { subCategory = it },
                icon = Icons.Rounded.Subtitles,
            )

            // Payment Mode Field
            PremiumReadOnlyField(
                label = "Payment Mode",
                value = paymentMode,
                icon = Icons.Rounded.Payments,
                onClick = { showPaymentMenu = true },
            )

            // Notes Field
            PremiumTextField(
                label = "Notes",
                value = note,
                onValueChange = { note = it },
                icon = Icons.Rounded.EditNote,
                singleLine = false,
            )

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: return@Button
                    if (amountValue <= 0) return@Button

                    val categoryName = categories.find { it.id == categoryId }?.name ?: "unknown"

                    // Analytics: Track successful add/edit only after validation passes
                    analyticsHelper.logEvent(
                        "transaction_added",
                        mapOf(
                            "type" to type.name,
                            "category" to categoryName,
                            "payment_mode" to paymentMode,
                            "is_edit" to (transactionId != null),
                        ),
                    )

                    val transaction =
                        Transaction(
                            id = transactionId ?: 0L,
                            amount = (amountValue * 100).toLong(),
                            note = note,
                            timestamp = timestamp,
                            type = type,
                            categoryId = categoryId,
                            subCategory = subCategory,
                            paymentMode = paymentMode,
                            currencyCode = currencyCode,
                        )
                    viewModel.saveTransaction(transaction)
                    onNavigateBack()
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0 && categoryId != null,
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
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { timestamp = it }
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
        ModalBottomSheet(
            onDismissRequest = { showCategoryMenu = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    "Select Category",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(24.dp),
                )
                filteredCategories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        leadingIcon = { CategoryIcon(category) },
                        onClick = {
                            categoryId = category.id
                            showCategoryMenu = false
                        },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }

    if (showPaymentMenu) {
        val modes = prasad.vennam.moneypilot.util.PaymentModes.ALL
        ModalBottomSheet(
            onDismissRequest = { showPaymentMenu = false },
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    "Payment Mode",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(24.dp),
                )
                modes.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode) },
                        onClick = {
                            paymentMode = mode
                            showPaymentMenu = false
                        },
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
        ) { Text("Expense") }
        SegmentedButton(
            selected = selectedType == TransactionType.INCOME,
            onClick = { onTypeSelected(TransactionType.INCOME) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors =
                SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.primary,
                ),
        ) { Text("Income") }
    }
}

@Composable
fun PremiumAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    color: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        shape = MaterialTheme.shapes.extraLarge,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f)),
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Amount", style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.6f))
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("0.00", color = color.copy(alpha = 0.3f)) },
                textStyle =
                    MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = color,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                        style = MaterialTheme.typography.displayMedium.copy(color = color),
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
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        singleLine = singleLine,
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
