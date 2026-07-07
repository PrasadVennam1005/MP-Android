package prasad.vennam.moneypilot.ui.cosplit

import androidx.compose.ui.res.stringResource
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.ui.components.BaseBottomSheet
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitExpense
import prasad.vennam.moneypilot.feature.cosplit.ui.CoSplitViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoSplitGroupDetailScreen(
    viewModel: CoSplitViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddExpense: () -> Unit
) {
    val group by viewModel.selectedGroup.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val settlements by viewModel.simplifiedSettlements.collectAsState()
    val aiExplanation by viewModel.aiSettlementExplanation.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val email by viewModel.userEmail.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.error.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Expenses, 1: Balances
    var showSettleDialog by remember { mutableStateOf(false) }

    // AI Log Command State
    var aiCommandText by remember { mutableStateOf("") }
    
    var expenseToDelete by remember { mutableStateOf<CoSplitExpense?>(null) }
    var showAiResultDialog by remember { mutableStateOf(false) }
    var parsedAiSplit by remember { mutableStateOf<prasad.vennam.moneypilot.feature.cosplit.util.AiParsedSplit?>(null) }

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditGroupDialog by remember { mutableStateOf(false) }

    if (group == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group!!.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            try {
                                val inviteUrl = "https://prasadvennam1005.github.io/cosplit/join?groupId=${group!!.id}&name=${java.net.URLEncoder.encode(group!!.name, "UTF-8")}"
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Join my CoSplit group '${group!!.name}' on MoneyPilot to split bills and track expenses together!\n\nClick this link to join: $inviteUrl")
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, "Invite friends to group")
                                context.startActivity(shareIntent)
                            } catch (e: Exception) {
                                // handle error
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share Group Invitation")
                    }
                    IconButton(onClick = onNavigateToAddExpense) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Expense")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(androidx.compose.material.icons.Icons.Rounded.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit_members)) },
                                onClick = {
                                    showMenu = false
                                    showEditGroupDialog = true
                                },
                                leadingIcon = {
                                    Icon(androidx.compose.material.icons.Icons.Rounded.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_group), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirmDialog = true
                                },
                                leadingIcon = {
                                    Icon(androidx.compose.material.icons.Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Selector
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Expenses", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Balances", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (selectedTab == 0) {
                // Expenses Tab
                Column(modifier = Modifier.weight(1f)) {
                    // AI Quick Logging Box
                    AiLogBox(
                        value = aiCommandText,
                        onValueChange = { aiCommandText = it },
                        isLoading = isAiLoading,
                        onSend = {
                            viewModel.parseSplitCommand(aiCommandText) { parsed ->
                                parsedAiSplit = parsed
                                showAiResultDialog = parsed != null
                                if (parsed != null) {
                                    aiCommandText = ""
                                }
                            }
                        }
                    )

                    if (expenses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No expenses logged",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Log bills using the + button or the AI logger above.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(expenses) { expense ->
                                ExpenseItem(
                                    expense = expense,
                                    currentUserEmail = email,
                                    memberNames = group!!.memberNames,
                                    onDelete = {
                                        expenseToDelete = expense
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Balances Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Balances", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                group!!.balances.forEach { (memberEmail, balance) ->
                                    val isCurrentUser = memberEmail == email
                                    val displayName = group?.memberNames?.get(memberEmail) ?: memberEmail
                                    val color = when {
                                        balance > 0.01 -> Color(0xFF2E7D32)
                                        balance < -0.01 -> Color(0xFFC62828)
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = displayName,
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = when {
                                                    isCurrentUser && balance > 0.01 -> "You get back"
                                                    isCurrentUser && balance < -0.01 -> "You owe"
                                                    isCurrentUser -> "Settled up"
                                                    balance > 0.01 -> "Gets back"
                                                    balance < -0.01 -> "Owes"
                                                    else -> "Settled up"
                                                },
                                                fontSize = 12.sp,
                                                color = color
                                            )
                                            if (kotlin.math.abs(balance) > 0.01) {
                                                Text(
                                                    text = "₹${String.format("%.2f", kotlin.math.abs(balance))}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = color
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Simplified Settle Up Routes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    IconButton(
                                        onClick = { viewModel.explainSimplifiedSettlements() },
                                        enabled = settlements.isNotEmpty() && !isAiLoading
                                    ) {
                                        if (isAiLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        } else {
                                            Icon(
                                                Icons.Rounded.AutoAwesome,
                                                contentDescription = "AI Explanation",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                if (settlements.isEmpty()) {
                                    Text(
                                        "All settled up! No payment routes needed.",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                    )
                                } else {
                                    settlements.forEach { route ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Rounded.ArrowForward,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(route, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }

                                    if (aiExplanation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(modifier = Modifier.padding(12.dp)) {
                                                Icon(
                                                    Icons.Rounded.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    aiExplanation,
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { showSettleDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = settlements.isNotEmpty()
                                ) {
                                    Text("Settle Up Payments")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete this expense? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExpense(expenseToDelete!!.id)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSettleDialog) {
        SettleUpDialog(
            members = group!!.members,
            onDismiss = { showSettleDialog = false },
            onSettle = { payer, receiver, amount ->
                viewModel.settleUp(payer, receiver, amount) { success ->
                    if (success) {
                        showSettleDialog = false
                    }
                }
            }
        )
    }

    if (showAiResultDialog && parsedAiSplit != null) {
        AiSplitPreviewDialog(
            parsed = parsedAiSplit!!,
            onDismiss = { showAiResultDialog = false },
            onConfirm = { desc, amt, paidBy, splitType, splits ->
                viewModel.addExpense(desc, amt, paidBy, splitType, splits) { success ->
                    if (success) {
                        showAiResultDialog = false
                    }
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.delete_group_title)) },
            text = { Text(stringResource(R.string.delete_group_desc, group!!.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGroup(group!!.id) { success ->
                            if (success) {
                                onNavigateBack()
                            }
                        }
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showEditGroupDialog) {
        var isSaving by remember { mutableStateOf(false) }
        val currentUserEmail by viewModel.userEmail.collectAsState()

        ManageGroupBottomSheet(
            isCreateMode = false,
            initialMembers = group!!.members,
            initialMemberNamesMap = group!!.memberNames,
            currentUserEmail = currentUserEmail,
            onFetchUserProfile = { email -> viewModel.getUserProfile(email) },
            onDismiss = { showEditGroupDialog = false },
            onSave = { _, members, memberNamesMap ->
                isSaving = true
                viewModel.updateGroupMembers(group!!.id, members, memberNamesMap) { success ->
                    isSaving = false
                    if (success) {
                        showEditGroupDialog = false
                        Toast.makeText(context, context.getString(R.string.members_updated), Toast.LENGTH_SHORT).show()
                    }
                }
            },
            isSaving = isSaving
        )
    }
}

@Composable
fun AiLogBox(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Log with AI e.g. Taxi 300 paid by me...", fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                maxLines = 2
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        Icons.Rounded.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseItem(
    expense: CoSplitExpense,
    currentUserEmail: String,
    memberNames: Map<String, String>,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (expense.description.contains("Settlement")) {
                                Icons.Rounded.DoneAll
                            } else {
                                Icons.Rounded.ReceiptLong
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = expense.description,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        val paidByName = memberNames[expense.paidBy] ?: expense.paidBy
                        Text(
                            text = "Paid by: $paidByName",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${String.format("%.2f", expense.amount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Split details (${expense.splitType}):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    expense.splitDetails.forEach { (member, weight) ->
                        val shareText = when (expense.splitType) {
                            "EQUAL" -> "Equal share"
                            "EXACT" -> "₹${String.format("%.2f", weight)}"
                            "PERCENT" -> "$weight%"
                            else -> "Share"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = member,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = shareText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettleUpDialog(
    members: List<String>,
    onDismiss: () -> Unit,
    onSettle: (String, String, Double) -> Unit
) {
    var payer by remember { mutableStateOf(members.firstOrNull() ?: "") }
    var receiver by remember { mutableStateOf(members.getOrNull(1) ?: "") }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Settlement", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Who paid?", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                DropdownSelector(
                    options = members,
                    selected = payer,
                    onSelected = { payer = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Who received?", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                DropdownSelector(
                    options = members.filter { it != payer },
                    selected = receiver,
                    onSelected = { receiver = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0.0) {
                        onSettle(payer, receiver, amount)
                    }
                },
                enabled = amountText.toDoubleOrNull() != null,
                shape = RoundedCornerShape(10.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    options: List<String>,
    selected: String,
    displayMap: Map<String, String> = emptyMap(),
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = displayMap[selected] ?: selected,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayMap[option] ?: option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AiSplitPreviewDialog(
    parsed: prasad.vennam.moneypilot.feature.cosplit.util.AiParsedSplit,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, Map<String, Double>) -> Unit
) {
    var description by remember { mutableStateOf(parsed.description) }
    var amount by remember { mutableStateOf(parsed.amount.toString()) }
    var paidBy by remember { mutableStateOf(parsed.paidBy) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm AI Split", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = paidBy,
                    onValueChange = { paidBy = it },
                    label = { Text("Paid By (Email)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Split shares identified:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                parsed.splitDetails.forEach { (email, weight) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        val displayValue = if (parsed.splitType == "PERCENT") "$weight%" else "₹$weight"
                        Text(displayValue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amtDouble = amount.toDoubleOrNull() ?: parsed.amount
                    onConfirm(description, amtDouble, paidBy, parsed.splitType, parsed.splitDetails)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Log Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
