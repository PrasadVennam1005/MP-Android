package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.domain.usecase.GetTransactionsUseCase
import javax.inject.Inject

data class SandboxDefaults(
    val avgMonthlyIncome: Double,
    val avgMonthlyExpense: Double,
    val currencyCode: String,
)

@HiltViewModel
class SandboxViewModel
    @Inject
    constructor(
        private val getTransactionsUseCase: GetTransactionsUseCase,
        private val userPreferences: UserPreferences,
    ) : ViewModel() {
        val defaults: StateFlow<SandboxDefaults> =
            combine(
                getTransactionsUseCase(),
                userPreferences.currency,
            ) { transactions, currency ->
                val now = System.currentTimeMillis()
                val cutoff = now - 60L * 24L * 60L * 60L * 1000L // last 60 days
                val recentTxs = transactions.filter { it.timestamp >= cutoff }

                val oldestTimestamp = recentTxs.minOfOrNull { it.timestamp }
                val monthsSpanned =
                    if (oldestTimestamp != null) {
                        val durationMs = now - oldestTimestamp
                        val months = durationMs.toDouble() / (30.0 * 24.0 * 60.0 * 60.0 * 1000.0)
                        months.coerceIn(1.0, 2.0)
                    } else {
                        1.0
                    }

                // Aggregate income and expense amounts
                val totalIncome = recentTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.toDouble() / 100.0 }
                val totalExpense = recentTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.toDouble() / 100.0 }

                // Sensible fallbacks if the user has no transaction history yet
                val defaultIncome = if (totalIncome > 0) totalIncome / monthsSpanned else 50000.0
                val defaultExpense = if (totalExpense > 0) totalExpense / monthsSpanned else 30000.0

                SandboxDefaults(
                    avgMonthlyIncome = defaultIncome,
                    avgMonthlyExpense = defaultExpense,
                    currencyCode = currency,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SandboxDefaults(50000.0, 30000.0, "INR"),
            )
    }
