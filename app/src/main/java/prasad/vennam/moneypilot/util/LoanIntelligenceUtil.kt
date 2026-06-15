package prasad.vennam.moneypilot.util

import java.util.*
import kotlin.math.ln
import kotlin.math.pow

object LoanIntelligenceUtil {
    /**
     * Estimates the remaining months for a loan based on current outstanding, interest rate, and EMI.
     * Formula: n = -ln(1 - (PV * i) / PMT) / ln(1 + i)
     */
    fun estimateRemainingTenure(
        outstandingAmount: Long,
        annualInterestRate: Double,
        emiAmount: Long,
    ): Int = calculateTenure(outstandingAmount.toDouble(), emiAmount.toDouble(), annualInterestRate)

    /**
     * Calculates the missing interest rate given principal, EMI, and tenure.
     * Uses binary search for high stability and precision.
     */
    fun calculateInterestRate(
        principal: Long,
        emi: Long,
        tenureMonths: Int,
    ): Double = calculateInterestRate(principal.toDouble(), emi.toDouble(), tenureMonths)

    /**
     * Calculates monthly EMI given principal, interest rate, and tenure.
     */
    fun calculateEmi(
        principal: Double,
        interestRate: Double,
        tenureMonths: Int,
    ): Double {
        if (principal <= 0.0 || tenureMonths <= 0) return 0.0
        if (interestRate <= 0.0) return principal / tenureMonths

        val i = (interestRate / 100.0) / 12.0
        val common = (1.0 + i).pow(tenureMonths)
        return principal * i * common / (common - 1.0)
    }

    /**
     * Calculates principal given EMI, interest rate, and tenure.
     */
    fun calculatePrincipal(
        emi: Double,
        interestRate: Double,
        tenureMonths: Int,
    ): Double {
        if (emi <= 0.0 || tenureMonths <= 0) return 0.0
        if (interestRate <= 0.0) return emi * tenureMonths

        val i = (interestRate / 100.0) / 12.0
        val common = (1.0 + i).pow(tenureMonths)
        return emi * (common - 1.0) / (i * common)
    }

    /**
     * Calculates tenure in months given principal, EMI, and interest rate.
     */
    fun calculateTenure(
        principal: Double,
        emi: Double,
        interestRate: Double,
    ): Int {
        if (principal <= 0.0 || emi <= 0.0) return 0
        if (interestRate <= 0.0) {
            return kotlin.math
                .round(principal / emi)
                .toInt()
                .coerceIn(1, 1200)
        }

        val i = (interestRate / 100.0) / 12.0
        if (emi <= principal * i) return 1200 // Interest exceeds EMI, return maximum tenure

        val n = -ln(1.0 - (principal * i) / emi) / ln(1.0 + i)
        return kotlin.math
            .round(n)
            .toInt()
            .coerceIn(1, 1200)
    }

    /**
     * Calculates the missing interest rate given principal, EMI, and tenure.
     * Uses binary search to find rate r (annualized percent) such that calculateEmi(principal, r, tenureMonths) equals emi.
     */
    fun calculateInterestRate(
        principal: Double,
        emi: Double,
        tenureMonths: Int,
    ): Double {
        if (principal <= 0.0 || emi <= 0.0 || tenureMonths <= 0) return 0.0
        if (emi * tenureMonths <= principal) return 0.0

        var low = 0.0
        var high = 100.0
        var mid = 0.0
        for (iter in 0..40) {
            mid = (low + high) / 2
            val calcEmi = calculateEmi(principal, mid, tenureMonths)
            if (calcEmi > emi) {
                high = mid
            } else {
                low = mid
            }
        }
        return mid
    }

    /**
     * Predicts future payoff date based on current progress.
     */
    fun predictPayoffDate(
        outstandingAmount: Long,
        annualInterestRate: Double,
        emiAmount: Long,
    ): Long {
        val remainingMonths = estimateRemainingTenure(outstandingAmount, annualInterestRate, emiAmount)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, remainingMonths)
        return calendar.timeInMillis
    }
}
