package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.domain.model.AmortizationInstallment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class GenerateAmortizationScheduleUseCase
    @Inject
    constructor() {
        operator fun invoke(
            principal: Double,
            annualRate: Double,
            tenureMonths: Int,
            monthlyEmi: Double,
        ): List<AmortizationInstallment> {
            val schedule = mutableListOf<AmortizationInstallment>()
            var balance = principal
            val monthlyRate = annualRate / 12 / 100
            val cal = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

            for (i in 1..tenureMonths) {
                val interest = if (annualRate > 0) balance * monthlyRate else 0.0
                val principalPaid = minOf(balance, monthlyEmi - interest)
                balance -= principalPaid

                cal.add(Calendar.MONTH, 1)

                schedule.add(
                    AmortizationInstallment(
                        id = i,
                        dateLabel = dateFormat.format(cal.time),
                        principalPaid = principalPaid,
                        interestPaid = interest,
                        remainingBalance = maxOf(0.0, balance),
                        totalPayment = principalPaid + interest,
                        monthlyEmi = monthlyEmi,
                    ),
                )

                if (balance <= 0) break
            }
            return schedule
        }

        fun generateYearlySchedule(monthlySchedule: List<AmortizationInstallment>): List<AmortizationInstallment> {
            val grouped =
                monthlySchedule.groupBy { installment ->
                    installment.dateLabel.split(" ").last() // Group by year
                }

            return grouped.entries.mapIndexed { index, entry ->
                val year = entry.key
                val installments = entry.value

                AmortizationInstallment(
                    id = index + 1,
                    dateLabel = year,
                    principalPaid = installments.sumOf { it.principalPaid },
                    interestPaid = installments.sumOf { it.interestPaid },
                    remainingBalance = installments.last().remainingBalance,
                    totalPayment = installments.sumOf { it.totalPayment },
                    monthlyEmi = installments.sumOf { it.totalPayment } / installments.size, // Avg monthly EMI in that year
                )
            }
        }
    }
