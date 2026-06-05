package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import prasad.vennam.moneypilot.util.FinancePriceFetcher
import prasad.vennam.moneypilot.util.inPaisa
import prasad.vennam.moneypilot.util.inRupees
import javax.inject.Inject
import prasad.vennam.moneypilot.data.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.combine

data class InvestmentSummary(
    val totalInvested: Double = 0.0,
    val totalCurrent: Double = 0.0
)

// ─── Auto-fill quantity state ─────────────────────────────────────────────────
sealed class AutoFillState {
    object Idle : AutoFillState()
    object Loading : AutoFillState()
    data class Success(val quantity: Double, val priceUsed: Double) : AutoFillState()
    object Error : AutoFillState()
}

@HiltViewModel
class InvestmentViewModel @Inject constructor(
    private val repository: MoneyPilotRepository,
    private val exchangeRateRepo: ExchangeRateRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val allInvestments: StateFlow<List<Investment>> = repository.allInvestments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val investmentSummary: StateFlow<InvestmentSummary> = combine(
        repository.allInvestments,
        exchangeRateRepo.allRates,
        userPreferences.currency
    ) { investments, rates, currentCurrency ->
        fun convertAmount(amountInPaisa: Long, fromCurrency: String): Double {
            if (fromCurrency == currentCurrency) return amountInPaisa / 100.0
            val rateFrom = rates[fromCurrency] ?: 1.0
            val rateTo = rates[currentCurrency] ?: 1.0
            val amountInUSD = (amountInPaisa / 100.0) / rateFrom
            return amountInUSD * rateTo
        }

        val totalInvested = investments.sumOf { convertAmount(it.investedAmount, it.currencyCode) }
        val totalCurrent = investments.sumOf { convertAmount(it.currentValue, it.currencyCode) }

        InvestmentSummary(totalInvested, totalCurrent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InvestmentSummary())

    // ── Live price refresh ────────────────────────────────────────────────────
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    // ── Symbol search (debounced) ─────────────────────────────────────────────
    private val _symbolResults = MutableStateFlow<List<FinancePriceFetcher.SymbolResult>>(emptyList())
    val symbolResults: StateFlow<List<FinancePriceFetcher.SymbolResult>> = _symbolResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    // ── Auto-fill quantity ────────────────────────────────────────────────────
    private val _autoFillState = MutableStateFlow<AutoFillState>(AutoFillState.Idle)
    val autoFillState: StateFlow<AutoFillState> = _autoFillState.asStateFlow()

    init {
        refreshAllPrices()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Quantity auto-fill: fetch historical closing price on the investment date
    // then compute quantity = investedAmount / priceOnDate
    // ──────────────────────────────────────────────────────────────────────────
    fun fetchQuantityForDate(
        symbol: String,
        assetType: String,
        investedAmount: Double,
        dateMs: Long
    ) {
        viewModelScope.launch {
            _autoFillState.value = AutoFillState.Loading
            val price = try {
                when {
                    assetType == "Mutual Fund" && symbol.all { it.isDigit() } ->
                        FinancePriceFetcher.fetchNavOnDate(symbol, dateMs)
                    else ->
                        FinancePriceFetcher.fetchPriceOnDate(symbol, dateMs)
                }
            } catch (e: Exception) {
                null
            }
            _autoFillState.value = if (price != null && price > 0) {
                AutoFillState.Success(
                    quantity = investedAmount / price,
                    priceUsed = price
                )
            } else {
                AutoFillState.Error
            }
        }
    }

    fun clearAutoFill() {
        _autoFillState.value = AutoFillState.Idle
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Symbol autocomplete search (debounced 350 ms)
    // ──────────────────────────────────────────────────────────────────────────
    fun searchSymbols(query: String, assetType: String) {
        searchJob?.cancel()
        if (assetType == "Gold") {
            _symbolResults.value = FinancePriceFetcher.goldSuggestions()
            return
        }
        if (query.length < 2) {
            _symbolResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _isSearching.value = true
            _symbolResults.value = try {
                when (assetType) {
                    "Mutual Fund" -> FinancePriceFetcher.searchMutualFunds(query)
                    else -> FinancePriceFetcher.searchStockSymbols(query)
                }
            } catch (e: Exception) {
                emptyList()
            }
            _isSearching.value = false
        }
    }

    fun clearSymbolSearch() {
        searchJob?.cancel()
        _symbolResults.value = emptyList()
        _isSearching.value = false
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Live price refresh for all portfolio investments
    // ──────────────────────────────────────────────────────────────────────────
    fun refreshAllPrices() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val investments = allInvestments.value
            var updatedAny = false
            for (investment in investments) {
                val updatedValue = when (investment.type) {
                    "Stock", "Crypto", "Gold" -> {
                        val sym = investment.symbol
                        val qty = investment.quantity ?: 0.0
                        if (!sym.isNullOrBlank() && qty > 0.0) {
                            FinancePriceFetcher.fetchYahooPrice(sym)?.times(qty)
                        } else null
                    }
                    "Mutual Fund" -> {
                        val sym = investment.symbol
                        val qty = investment.quantity ?: 0.0
                        if (!sym.isNullOrBlank() && qty > 0.0) {
                            val price = if (sym.all { it.isDigit() }) {
                                FinancePriceFetcher.fetchAmfiNav(sym)
                            } else {
                                FinancePriceFetcher.fetchYahooPrice(sym)
                            }
                            price?.times(qty)
                        } else null
                    }
                    "FD", "Real Estate" -> {
                        val rate = investment.interestRate ?: 0.0
                        val start = investment.startDate ?: 0L
                        if (rate > 0.0 && start > 0L) {
                            FinancePriceFetcher.calculateCompoundedValue(
                                investedAmount = investment.investedAmount.inRupees,
                                annualRate = rate,
                                startDate = start
                            )
                        } else null
                    }
                    else -> null
                }
                try {
                    if (updatedValue != null && updatedValue.inPaisa != investment.currentValue) {
                        repository.updateInvestment(investment.copy(currentValue = updatedValue.inPaisa))
                        updatedAny = true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("InvestmentViewModel", "Error updating investment", e)
                }
            }
            try {
                if (updatedAny) userPreferences.setSynced(false)
            } catch (e: Exception) {
                android.util.Log.e("InvestmentViewModel", "Error refreshing investments", e)
            }
            _isRefreshing.value = false
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CRUD
    // ──────────────────────────────────────────────────────────────────────────
    fun saveInvestment(investment: Investment) {
        viewModelScope.launch {
            try {
                userPreferences.setSynced(false)
                if (investment.id == 0L) {
                    repository.insertInvestment(investment)
                } else {
                    repository.updateInvestment(investment)
                }
            } catch (e: Exception) {
                android.util.Log.e("InvestmentViewModel", "Error saving investment", e)
            }
        }
    }

    fun deleteInvestment(investment: Investment) {
        viewModelScope.launch {
            try {
                userPreferences.setSynced(false)
                repository.deleteInvestment(investment)
            } catch (e: Exception) {
                android.util.Log.e("InvestmentViewModel", "Error deleting investment", e)
            }
        }
    }
}
