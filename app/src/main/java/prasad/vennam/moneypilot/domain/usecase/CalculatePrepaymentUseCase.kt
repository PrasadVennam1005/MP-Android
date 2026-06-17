package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.domain.model.PrepaymentResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class CalculatePrepaymentUseCase
    @Inject
    constructor() {
        operator fun invoke(
            principal: Double,
            annualRate: Double,
            tenureMonths: Int,
            monthlyEmi: Double,
            prepaymentAmount: Double,
            isMonthlyPrepayment: Boolean,
        ): PrepaymentResult {
            if (principal <= 0) return PrepaymentResult(0.0, 0, "")

            // Calculate original total interest
            val originalTotalPayable = monthlyEmi * tenureMonths
            val originalTotalInterest = originalTotalPayable - principal

            var balance = principal
            val monthlyRate = annualRate / 12 / 100
            var totalInterestPaid = 0.0
            var actualMonths = 0

            while (balance > 0 && actualMonths < 1000) { // Limit to avoid infinite loop
                actualMonths++
                val interest = balance * monthlyRate
                totalInterestPaid += interest

                val currentEmi = if (balance + interest < monthlyEmi) balance + interest else monthlyEmi
                var principalPaid = currentEmi - interest

                if (isMonthlyPrepayment) {
                    principalPaid += prepaymentAmount
                } else if (actualMonths == 1) {
                    principalPaid += prepaymentAmount
                }

                balance -= principalPaid
                if (balance <= 0) break
            }

            val interestSaved = originalTotalInterest - totalInterestPaid
            val monthsSaved = tenureMonths - actualMonths

            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, actualMonths)
            val newCompletionDate = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)

            return PrepaymentResult(
                totalInterestSaved = maxOf(0.0, interestSaved),
                monthsSaved = maxOf(0, monthsSaved),
                newCompletionDate = newCompletionDate,
            )
        }
    }
