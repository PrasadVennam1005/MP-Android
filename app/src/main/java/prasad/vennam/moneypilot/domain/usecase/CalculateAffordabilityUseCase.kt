package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.domain.model.AffordabilityResult
import javax.inject.Inject
import kotlin.math.pow

class CalculateAffordabilityUseCase @Inject constructor() {
    operator fun invoke(
        monthlyIncome: Double,
        annualRate: Double,
        tenureYears: Int
    ): AffordabilityResult {
        if (monthlyIncome <= 0) return AffordabilityResult(0.0, 0.0, false)

        val recommendedEmi = monthlyIncome * 0.30 // 30% rule
        val monthlyRate = annualRate / 12 / 100
        val tenureMonths = (tenureYears * 12).toDouble()

        val maxLoanAmount = if (annualRate > 0) {
            recommendedEmi * ((1 + monthlyRate).pow(tenureMonths) - 1) /
                    (monthlyRate * (1 + monthlyRate).pow(tenureMonths))
        } else {
            recommendedEmi * tenureMonths
        }

        return AffordabilityResult(
            maxLoanAmount = maxLoanAmount,
            recommendedEmi = recommendedEmi,
            isAffordable = true
        )
    }
}
