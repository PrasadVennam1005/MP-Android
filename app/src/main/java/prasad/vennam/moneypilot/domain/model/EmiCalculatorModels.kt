package prasad.vennam.moneypilot.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class EmiResult(
    val monthlyEmi: Double,
    val totalInterest: Double,
    val totalPayable: Double,
    val loanEndDate: String,
    val effectiveInterestRate: Double,
    val processingFeeAmount: Double = 0.0,
    val totalCostWithFee: Double = 0.0,
)

@Immutable
data class AmortizationInstallment(
    val id: Int,
    val dateLabel: String,
    val principalPaid: Double,
    val interestPaid: Double,
    val remainingBalance: Double,
    val totalPayment: Double,
    val monthlyEmi: Double,
)

@Immutable
data class LoanComparisonResult(
    val emiDifference: Double,
    val interestDifference: Double,
    val totalSavings: Double,
    val winnerIndex: Int,
)

@Immutable
data class PrepaymentResult(
    val totalInterestSaved: Double,
    val monthsSaved: Int,
    val newCompletionDate: String,
)

@Immutable
data class AffordabilityResult(
    val maxLoanAmount: Double,
    val recommendedEmi: Double,
    val isAffordable: Boolean,
)
