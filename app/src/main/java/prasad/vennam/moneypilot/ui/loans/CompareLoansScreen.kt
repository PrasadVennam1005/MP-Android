package prasad.vennam.moneypilot.ui.loans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.ui.loans.components.InterestRateInput
import prasad.vennam.moneypilot.ui.loans.components.LoanAmountInput
import prasad.vennam.moneypilot.ui.loans.components.LoanComparison
import prasad.vennam.moneypilot.ui.loans.components.TenureInput
import prasad.vennam.moneypilot.ui.viewmodel.CompareLoansViewModel
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import java.util.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareLoansScreen(
    onBack: () -> Unit,
    viewModel: CompareLoansViewModel = hiltViewModel()
) {
    val currencyCode = LocalCurrencyCode.current
    val currencySymbol = remember(currencyCode) {
        try { Currency.getInstance(currencyCode).symbol } catch (_: Exception) { "$" }
    }
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.loan_comparison),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Loan A
            LoanInputSection(
                title = "Loan A",
                amount = state.amountA,
                rate = state.rateA,
                tenure = state.tenureA,
                currencySymbol = currencySymbol,
                onAmountChange = { viewModel.updateAmountA(it) },
                onRateChange = { viewModel.updateRateA(it) },
                onTenureChange = { viewModel.updateTenureA(it) }
            )

            // Loan B
            LoanInputSection(
                title = "Loan B",
                amount = state.amountB,
                rate = state.rateB,
                tenure = state.tenureB,
                currencySymbol = currencySymbol,
                onAmountChange = { viewModel.updateAmountB(it) },
                onRateChange = { viewModel.updateRateB(it) },
                onTenureChange = { viewModel.updateTenureB(it) }
            )

            LoanComparison(
                result = state.comparisonResult,
                currencyCode = currencyCode
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LoanInputSection(
    title: String,
    amount: String,
    rate: String,
    tenure: String,
    currencySymbol: String,
    onAmountChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onTenureChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            
            LoanAmountInput(
                amountInput = amount,
                onAmountChange = onAmountChange,
                currencySymbol = currencySymbol,
                minAmount = 1000f,
                maxAmount = 100000000f
            )
            
            InterestRateInput(
                rateInput = rate,
                onRateChange = onRateChange
            )
            
            TenureInput(
                tenureInput = tenure,
                onTenureChange = onTenureChange,
                isYears = true,
                onUnitToggle = {} // Fixed to years for comparison simplicity
            )
        }
    }
}
