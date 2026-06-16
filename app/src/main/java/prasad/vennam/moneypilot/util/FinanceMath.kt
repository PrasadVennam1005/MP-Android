package prasad.vennam.moneypilot.util

import kotlin.math.pow

object FinanceMath {
    fun calculateCompoundedValue(
        investedAmount: Double,
        annualRate: Double,
        startDate: Long,
    ): Double {
        val elapsedMs = System.currentTimeMillis() - startDate
        if (elapsedMs <= 0) return investedAmount
        val years = elapsedMs.toDouble() / (1000.0 * 60 * 60 * 24 * 365.25)
        return investedAmount * (1.0 + annualRate / 100.0).pow(years)
    }
}
