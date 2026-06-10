package prasad.vennam.moneypilot.ui.loans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.ui.dashboard.SyncState
import prasad.vennam.moneypilot.ui.dashboard.components.DashboardTopBar
import prasad.vennam.moneypilot.ui.viewmodel.DashboardViewModel
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import prasad.vennam.moneypilot.util.inRupees
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    userData: UserPreferences.UserData?,
    syncState: SyncState?,
    onProfileClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var selectedLoan by remember { mutableStateOf<Loan?>(null) }
    var showAddLoanSheet by remember { mutableStateOf(false) }
    var loanToDelete by remember { mutableStateOf<Loan?>(null) }

    Scaffold(
        topBar = {
            DashboardTopBar(
                userData = userData,
                syncState = syncState,
                unreadCount = 0,
                onProfileClick = onProfileClick,
                onNotificationClick = {},
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedLoan = null
                    showAddLoanSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_loan))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = stringResource(R.string.my_loans),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (state.loans.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.no_loans_tracked_yet),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.add_loan_to_monitor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.loans) { loan ->
                        FullWidthLoanCard(
                            loan = loan,
                            currencyCode = loan.currencyCode,
                            onEditClick = {
                                selectedLoan = loan
                                showAddLoanSheet = true
                            },
                            onDeleteClick = {
                                loanToDelete = loan
                            }
                        )
                    }
                }
            }
        }
    }

    if (loanToDelete != null) {
        AlertDialog(
            onDismissRequest = { loanToDelete = null },
            title = {
                Text(
                    text = stringResource(R.string.delete_loan),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_loan_confirm, loanToDelete!!.name),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLoan(loanToDelete!!)
                        loanToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { loanToDelete = null }) {
                    Text(stringResource(R.string.cancel), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    if (showAddLoanSheet) {
        LoanFormBottomSheet(
            initialLoan = selectedLoan,
            onDismiss = { showAddLoanSheet = false },
            onConfirm = { name, total, outstanding, emi, lenderName, interestRate, tenureMonths, dueDayOfMonth, isNotificationEnabled ->
                if (selectedLoan == null) {
                    viewModel.addLoan(
                        name = name,
                        total = total,
                        outstanding = outstanding,
                        emi = emi,
                        currencyCode = state.categories.firstOrNull()?.let { "INR" } ?: "INR",
                        lenderName = lenderName,
                        interestRate = interestRate,
                        tenureMonths = tenureMonths,
                        dueDayOfMonth = dueDayOfMonth,
                        isNotificationEnabled = isNotificationEnabled
                    )
                } else {
                    viewModel.updateLoan(
                        selectedLoan!!.copy(
                            name = name,
                            totalAmount = total,
                            outstandingAmount = outstanding,
                            emiAmount = emi,
                            lenderName = lenderName,
                            interestRate = interestRate,
                            tenureMonths = tenureMonths,
                            dueDayOfMonth = dueDayOfMonth,
                            isNotificationEnabled = isNotificationEnabled
                        )
                    )
                }
                showAddLoanSheet = false
            },
            onDelete = {
                selectedLoan?.let { viewModel.deleteLoan(it) }
                showAddLoanSheet = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullWidthLoanCard(
    loan: Loan,
    currencyCode: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val progress = if (loan.totalAmount > 0) {
        (1f - (loan.outstandingAmount.toFloat() / loan.totalAmount.toFloat())).coerceIn(0f, 1f)
    } else {
        0f
    }
    val percentPaid = (progress * 100).toInt()
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onEditClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loan.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (loan.lenderName.isNotBlank()) {
                        Text(
                            text = loan.lenderName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.percent_paid, percentPaid),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.edit_details),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit_details)) },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_loan), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Badges Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loan.interestRate > 0.0) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.interest_rate_pa, loan.interestRate.toString())) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                if (loan.tenureMonths > 0) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.tenure_months_format, loan.tenureMonths)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                val reminderIcon = if (loan.isNotificationEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff
                val reminderText = if (loan.isNotificationEnabled) stringResource(R.string.reminders_on) else stringResource(R.string.reminders_off)
                SuggestionChip(
                    onClick = {},
                    label = { Text(reminderText) },
                    icon = { Icon(reminderIcon, null, modifier = Modifier.size(14.dp)) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.paid_amount_format, (loan.totalAmount - loan.outstandingAmount).inRupees, currencyCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.total_amount_format, loan.totalAmount.inRupees, currencyCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.monthly_emi),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${loan.emiAmount.inRupees} $currencyCode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.outstanding_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${loan.outstandingAmount.inRupees} $currencyCode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.next_emi_date_format, dateFormatter.format(Date(loan.nextEmiDate)), loan.dueDayOfMonth),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanFormBottomSheet(
    initialLoan: Loan? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long, Long, String, Double, Int, Int, Boolean) -> Unit,
    onDelete: () -> Unit = {},
) {
    var name by remember { mutableStateOf(initialLoan?.name ?: "") }
    var total by remember {
        mutableStateOf(
            initialLoan?.totalAmount?.inRupees?.toString()?.removeSuffix(".0") ?: ""
        )
    }
    var outstanding by remember {
        mutableStateOf(
            initialLoan?.outstandingAmount?.inRupees?.toString()?.removeSuffix(".0") ?: ""
        )
    }
    var emi by remember {
        mutableStateOf(
            initialLoan?.emiAmount?.inRupees?.toString()?.removeSuffix(".0") ?: ""
        )
    }
    var lenderName by remember { mutableStateOf(initialLoan?.lenderName ?: "") }
    var interestRate by remember { mutableStateOf(initialLoan?.interestRate?.toString()?.removeSuffix(".0") ?: "") }
    var tenureMonths by remember { mutableStateOf(initialLoan?.tenureMonths?.toString() ?: "") }
    var dueDayOfMonth by remember { mutableStateOf(initialLoan?.dueDayOfMonth?.toString() ?: "1") }
    var isNotificationEnabled by remember { mutableStateOf(initialLoan?.isNotificationEnabled ?: true) }

    val currencyCode = LocalCurrencyCode.current
    val currencySymbol = remember(currencyCode) { Currency.getInstance(currencyCode).symbol }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val totalVal = total.toDoubleOrNull()
    val outstandingVal = outstanding.toDoubleOrNull()
    val emiVal = emi.toDoubleOrNull()
    val interestRateVal = interestRate.toDoubleOrNull()
    val tenureMonthsVal = tenureMonths.toIntOrNull()
    val dueDayVal = dueDayOfMonth.toIntOrNull()

    val isTotalError = total.isNotEmpty() && (totalVal == null || totalVal <= 0.0)
    val isOutstandingError = outstanding.isNotEmpty() && (outstandingVal == null || outstandingVal < 0.0 || (totalVal != null && outstandingVal > totalVal))
    val isEmiError = emi.isNotEmpty() && (emiVal == null || emiVal <= 0.0)
    val isInterestError = interestRate.isNotEmpty() && (interestRateVal == null || interestRateVal < 0.0)
    val isTenureError = tenureMonths.isNotEmpty() && (tenureMonthsVal == null || tenureMonthsVal <= 0)
    val isDueDayError = dueDayOfMonth.isNotEmpty() && (dueDayVal == null || dueDayVal !in 1..28)

    val isFormValid = name.isNotBlank() &&
            total.isNotBlank() && !isTotalError &&
            outstanding.isNotBlank() && !isOutstandingError &&
            emi.isNotBlank() && !isEmiError &&
            !isInterestError &&
            !isTenureError &&
            dueDayOfMonth.isNotBlank() && !isDueDayError

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header with Close Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (initialLoan == null) stringResource(R.string.add_new_loan) else stringResource(R.string.edit_loan),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
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

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Lender / Bank Name
                OutlinedTextField(
                    value = lenderName,
                    onValueChange = { lenderName = it },
                    label = { Text(stringResource(R.string.lender_bank_name)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Business,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    placeholder = { Text(stringResource(R.string.lender_bank_placeholder)) },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Loan Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.loan_reference_name)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Badge,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    placeholder = { Text(stringResource(R.string.loan_reference_placeholder)) },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Total Amount
                OutlinedTextField(
                    value = total,
                    onValueChange = {
                        if (it.isEmpty() || it.toDoubleOrNull() != null) total = it
                    },
                    label = { Text(stringResource(R.string.total_loan_amount)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.AccountBalance,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    prefix = {
                        Text(
                            currencySymbol,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    isError = isTotalError,
                    supportingText = if (isTotalError) {
                        { Text(stringResource(R.string.total_error_desc)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Outstanding Amount
                OutlinedTextField(
                    value = outstanding,
                    onValueChange = {
                        if (it.isEmpty() || it.toDoubleOrNull() != null) outstanding = it
                    },
                    label = { Text(stringResource(R.string.outstanding_amount)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.AccountBalanceWallet,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    prefix = {
                        Text(
                            currencySymbol,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    isError = isOutstandingError,
                    supportingText = if (isOutstandingError) {
                        { Text(stringResource(R.string.outstanding_error_desc)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Monthly EMI
                OutlinedTextField(
                    value = emi,
                    onValueChange = {
                        if (it.isEmpty() || it.toDoubleOrNull() != null) emi = it
                    },
                    label = { Text(stringResource(R.string.monthly_emi)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.CreditCard,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    prefix = {
                        Text(
                            currencySymbol,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    isError = isEmiError,
                    supportingText = if (isEmiError) {
                        { Text(stringResource(R.string.emi_error_desc)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Row for Interest Rate and Tenure
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = interestRate,
                        onValueChange = {
                            if (it.isEmpty() || it.toDoubleOrNull() != null) interestRate = it
                        },
                        label = { Text(stringResource(R.string.interest_rate_pa_label)) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.TrendingUp,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        placeholder = { Text(stringResource(R.string.interest_rate_placeholder)) },
                        isError = isInterestError,
                        supportingText = if (isInterestError) {
                            { Text(stringResource(R.string.interest_error_desc)) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f),
                    )

                    OutlinedTextField(
                        value = tenureMonths,
                        onValueChange = {
                            if (it.isEmpty() || it.toIntOrNull() != null) tenureMonths = it
                        },
                        label = { Text(stringResource(R.string.tenure_months)) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.HourglassEmpty,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        placeholder = { Text(stringResource(R.string.tenure_placeholder)) },
                        isError = isTenureError,
                        supportingText = if (isTenureError) {
                            { Text(stringResource(R.string.tenure_error_desc)) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Due Day of Month
                OutlinedTextField(
                    value = dueDayOfMonth,
                    onValueChange = {
                        val intVal = it.toIntOrNull()
                        if (it.isEmpty() || (intVal != null && intVal in 1..28)) {
                            dueDayOfMonth = it
                        }
                    },
                    label = { Text(stringResource(R.string.emi_due_day_label)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.EventNote,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    placeholder = { Text(stringResource(R.string.emi_due_day_placeholder)) },
                    isError = isDueDayError,
                    supportingText = if (isDueDayError) {
                        { Text(stringResource(R.string.due_day_error_desc)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Reminders Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.payment_reminders),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.payment_reminders_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isNotificationEnabled,
                        onCheckedChange = { isNotificationEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Button(
                    onClick = {
                        if (isFormValid) {
                            val totalValParsed = total.toDoubleOrNull() ?: 0.0
                            val outstandingValParsed = outstanding.toDoubleOrNull() ?: 0.0
                            val emiValParsed = emi.toDoubleOrNull() ?: 0.0
                            val rateVal = interestRate.toDoubleOrNull() ?: 0.0
                            val tenureVal = tenureMonths.toIntOrNull() ?: 12
                            val dueDayVal = dueDayOfMonth.toIntOrNull() ?: 1

                            onConfirm(
                                name,
                                (totalValParsed * 100).toLong(),
                                (outstandingValParsed * 100).toLong(),
                                (emiValParsed * 100).toLong(),
                                lenderName,
                                rateVal,
                                tenureVal,
                                dueDayVal,
                                isNotificationEnabled
                            )
                        }
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Rounded.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (initialLoan == null) stringResource(R.string.add_loan) else stringResource(R.string.save_changes),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (initialLoan != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Icon(Icons.Rounded.Delete, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.delete_loan), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
