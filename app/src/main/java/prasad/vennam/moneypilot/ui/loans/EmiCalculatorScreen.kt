package prasad.vennam.moneypilot.ui.loans

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.ui.loans.components.AffordabilityCalculator
import prasad.vennam.moneypilot.ui.loans.components.DetailedReportScreen
import prasad.vennam.moneypilot.ui.loans.components.EmiResultCard
import prasad.vennam.moneypilot.ui.loans.components.InterestRateInput
import prasad.vennam.moneypilot.ui.loans.components.LoanAmountInput
import prasad.vennam.moneypilot.ui.loans.components.LoanTypeSelector
import prasad.vennam.moneypilot.ui.loans.components.PaymentBreakdownCard
import prasad.vennam.moneypilot.ui.loans.components.PrepaymentCalculator
import prasad.vennam.moneypilot.ui.loans.components.TenureInput
import prasad.vennam.moneypilot.ui.viewmodel.EmiCalculatorUiState
import prasad.vennam.moneypilot.ui.viewmodel.EmiCalculatorViewModel
import prasad.vennam.moneypilot.util.CurrencyFormatter
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import java.util.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmiCalculatorScreen(
    onBack: () -> Unit,
    onNavigateToSaveLoan: (Double, Double, Int, Double) -> Unit,
    onNavigateToCompare: () -> Unit,
    viewModel: EmiCalculatorViewModel = hiltViewModel(),
) {
    val currencyCode = LocalCurrencyCode.current
    val context = LocalContext.current
    val currencySymbol = remember(currencyCode) {
        try {
            Currency.getInstance(currencyCode).symbol
        } catch (_: Exception) {
            "$"
        }
    }

    LaunchedEffect(currencyCode) {
        viewModel.initializeDefaults(currencyCode)
    }

    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.showDetailedReport) stringResource(R.string.payment_breakdown) else stringResource(R.string.emi_calculator),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.showDetailedReport) {
                                viewModel.updateShowDetailedReport(false)
                            } else {
                                onBack()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        AnimatedContent(
            targetState = state.showDetailedReport,
            transitionSpec = {
                if (targetState) {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                } else {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                }
            },
            label = "EmiCalculatorContentTransition"
        ) { isDetailed ->
            Box(modifier = Modifier.padding(padding)) {
                if (!isDetailed) {
                    CalculatorMainView(
                        state = state,
                        viewModel = viewModel,
                        currencySymbol = currencySymbol,
                        currencyCode = currencyCode,
                        onSaveLoan = {
                            onNavigateToSaveLoan(
                                state.amountInput.toDoubleOrNull() ?: 0.0,
                                state.rateInput.toDoubleOrNull() ?: 0.0,
                                if (state.isTenureInYears) (state.tenureInput.toIntOrNull() ?: 0) * 12
                                else (state.tenureInput.toIntOrNull() ?: 0),
                                state.emiResult.monthlyEmi
                            )
                        },
                        onShare = {

                            val shareText =
                                ShareFormatter.buildLoanCalculationShareText(
                                    currencySymbol = currencySymbol,
                                    amount = state.amountInput,
                                    interestRate = state.rateInput,
                                    tenure = state.tenureInput,
                                    isTenureInYears = state.isTenureInYears,
                                    monthlyEmi = state.formattedMonthlyEmi,
                                    totalInterest = state.formattedTotalInterest,
                                    totalPayable = state.formattedTotalPayable,
                                    loanEndDate = state.emiResult.loanEndDate,
                                )

                            ShareHelper.shareText(
                                context = context,
                                subject = "MoneyPilot Loan EMI Report",
                                text = shareText,
                            )
                        },
                        onCompare = onNavigateToCompare
                    )
                } else {
                    DetailedReportScreen(
                        state = state,
                        viewModel = viewModel,
                        currencyCode = currencyCode
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculatorMainView(
    state: EmiCalculatorUiState,
    viewModel: EmiCalculatorViewModel,
    currencySymbol: String,
    currencyCode: String,
    onSaveLoan: () -> Unit,
    onShare: () -> Unit,
    onCompare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.loan_emi_calculator),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.emi_calculator_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.width(240.dp)
                )
            }
        }

        LoanTypeSelector(
            selectedTabIndex = state.selectedTab,
            onTabSelected = { viewModel.updateTab(it, currencyCode) }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                val maxAmount = when (state.selectedTab) {
                    0 -> if (currencyCode == "INR") 100_000_000f else 3_000_000f
                    1 -> if (currencyCode == "INR") 10_000_000f else 150_000f
                    2 -> if (currencyCode == "INR") 5_000_000f else 100_000f
                    else -> if (currencyCode == "INR") 20_000_000f else 500_000f
                }
                val minAmount = when (state.selectedTab) {
                    0 -> if (currencyCode == "INR") 500_000f else 10_000f
                    1 -> if (currencyCode == "INR") 100_000f else 5_000f
                    2 -> if (currencyCode == "INR") 20_000f else 1_000f
                    else -> if (currencyCode == "INR") 10_000f else 500f
                }

                LoanAmountInput(
                    amountInput = state.amountInput,
                    onAmountChange = { viewModel.updateAmount(it) },
                    currencySymbol = currencySymbol,
                    minAmount = minAmount,
                    maxAmount = maxAmount
                )

                InterestRateInput(
                    rateInput = state.rateInput,
                    onRateChange = { viewModel.updateRate(it) }
                )

                TenureInput(
                    tenureInput = state.tenureInput,
                    onTenureChange = { viewModel.updateTenure(it) },
                    isYears = state.isTenureInYears,
                    onUnitToggle = { viewModel.updateTenureUnit(it) }
                )
            }
        }

        EmiResultCard(
            monthlyEmi = state.formattedMonthlyEmi,
            totalInterest = state.formattedTotalInterest,
            totalPayable = state.formattedTotalPayable,
            loanEndDate = state.emiResult.loanEndDate,
            onSaveLoan = onSaveLoan,
            onShare = onShare
        )

        if (state.emiResult.totalPayable > 0.0) {
            PaymentBreakdownCard(
                principal = state.amountInput.toDoubleOrNull() ?: 0.0,
                totalInterest = state.emiResult.totalInterest,
                totalPayable = state.emiResult.totalPayable,
                formattedPrincipal = CurrencyFormatter.format(state.amountInput.toDoubleOrNull() ?: 0.0, currencyCode),
                formattedInterest = state.formattedTotalInterest,
                onViewReport = { viewModel.updateShowDetailedReport(true) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Advanced Tools",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )

        AffordabilityCalculator(
            monthlyIncome = state.monthlyIncomeInput,
            onIncomeChange = { viewModel.updateMonthlyIncome(it) },
            result = state.affordabilityResult,
            currencyCode = currencyCode
        )

        PrepaymentCalculator(
            prepaymentAmount = state.prepaymentAmount,
            onAmountChange = { viewModel.updatePrepaymentAmount(it) },
            isMonthly = state.isMonthlyPrepayment,
            onTypeToggle = { viewModel.updateIsMonthlyPrepayment(it) },
            result = state.prepaymentResult,
            currencyCode = currencyCode
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}
