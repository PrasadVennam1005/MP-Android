package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.domain.model.EmiResult
import prasad.vennam.moneypilot.domain.model.LoanComparisonResult
import javax.inject.Inject

class CompareLoansUseCase
    @Inject
    constructor() {
        operator fun invoke(
            loanA: EmiResult,
            loanB: EmiResult,
        ): LoanComparisonResult {
            val emiDiff = loanA.monthlyEmi - loanB.monthlyEmi
            val interestDiff = loanA.totalInterest - loanB.totalInterest
            val totalSavings = kotlin.math.abs(loanA.totalPayable - loanB.totalPayable)

            val winnerIndex = if (loanA.totalPayable < loanB.totalPayable) 0 else 1

            return LoanComparisonResult(
                emiDifference = emiDiff,
                interestDifference = interestDiff,
                totalSavings = totalSavings,
                winnerIndex = winnerIndex,
            )
        }
    }
