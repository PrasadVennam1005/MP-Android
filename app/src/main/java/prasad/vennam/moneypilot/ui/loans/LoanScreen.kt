package prasad.vennam.moneypilot.ui.loans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.ui.dashboard.SyncState
import prasad.vennam.moneypilot.ui.dashboard.components.DashboardTopBar
import prasad.vennam.moneypilot.ui.dashboard.components.LoanCard
import prasad.vennam.moneypilot.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    userData: UserPreferences.UserData?,
    syncState: SyncState?,
    onProfileClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showAddLoanDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DashboardTopBar(
                userData = userData,
                syncState = syncState,
                unreadCount = 0,
                onProfileClick = onProfileClick,
                onNotificationClick = {}
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddLoanDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Loan")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                text = "My Loans",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (state.loans.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No loans tracked yet. Add one to get started!")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(state.loans) { loan ->
                        // Reusing LoanCard but making it full width
                        Box(modifier = Modifier.fillMaxWidth()) {
                            LoanCard(loan = loan, currencyCode = loan.currencyCode)
                        }
                    }
                }
            }
        }
    }

    if (showAddLoanDialog) {
        // Simple Add Loan Dialog for demonstration
        AddLoanDialog(
            onDismiss = { showAddLoanDialog = false },
            onConfirm = { name, total, outstanding, emi ->
                // viewModel.addLoan(...) - need to add this to ViewModel
                showAddLoanDialog = false
            }
        )
    }
}

@Composable
fun AddLoanDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var outstanding by remember { mutableStateOf("") }
    var emi by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Loan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Loan Name") })
                TextField(value = total, onValueChange = { total = it }, label = { Text("Total Amount") })
                TextField(value = outstanding, onValueChange = { outstanding = it }, label = { Text("Outstanding Amount") })
                TextField(value = emi, onValueChange = { emi = it }, label = { Text("Monthly EMI") })
            }
        },
        confirmButton = {
            Button(onClick = { 
                onConfirm(name, total.toLongOrNull() ?: 0L, outstanding.toLongOrNull() ?: 0L, emi.toLongOrNull() ?: 0L) 
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
