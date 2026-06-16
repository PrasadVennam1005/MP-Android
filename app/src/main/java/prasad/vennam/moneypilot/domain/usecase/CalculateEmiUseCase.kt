package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.domain.model.EmiResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.math.pow

class CalculateEmiUseCase @Inject constructor() {
    operator fun invoke(
        principal: Double,
        annualRate: Double,
        tenure: Int,
        isTenureInYears: Boolean,
        processingFeePercent: Double = 0.0
    ): EmiResult {
        if (principal <= 0) return EmiResult(0.0, 0.0, 0.0, "", 0.0)

        val tenureMonths = if (isTenureInYears) tenure * 12 else tenure
        if (tenureMonths <= 0) return EmiResult(0.0, 0.0, principal, "", 0.0)

        val monthlyRate = annualRate / 12 / 100
        
        val emi = if (annualRate > 0) {
            (principal * monthlyRate * (1 + monthlyRate).pow(tenureMonths.toDouble())) /
                    ((1 + monthlyRate).pow(tenureMonths.toDouble()) - 1)
        } else {
            principal / tenureMonths
        }

        val totalPayable = emi * tenureMonths
        val totalInterest = maxOf(0.0, totalPayable - principal)
        
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, tenureMonths)
        val endDate = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)

        val processingFeeAmount = principal * (processingFeePercent / 100)
        val totalCostWithFee = totalPayable + processingFeeAmount
        
        // Effective interest rate calculation (simplified)
        val effectiveRate = annualRate // For V1, keeping it same as annual rate, but could be XIRR in future

        return EmiResult(
            monthlyEmi = emi,
            totalInterest = totalInterest,
            totalPayable = totalPayable,
            loanEndDate = endDate,
            effectiveInterestRate = effectiveRate,
            processingFeeAmount = processingFeeAmount,
            totalCostWithFee = totalCostWithFee
        )
    }
}
