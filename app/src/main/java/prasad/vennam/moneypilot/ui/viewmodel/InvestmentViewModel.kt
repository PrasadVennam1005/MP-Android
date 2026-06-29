package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.repository.ExchangeRateRepository
import prasad.vennam.moneypilot.domain.model.SymbolResult
import prasad.vennam.moneypilot.domain.repository.FinanceRepository
import prasad.vennam.moneypilot.domain.usecase.DeleteInvestmentUseCase
import prasad.vennam.moneypilot.domain.usecase.GetInvestmentsUseCase
import prasad.vennam.moneypilot.domain.usecase.SaveInvestmentUseCase
import prasad.vennam.moneypilot.domain.usecase.UpdateInvestmentUseCase
import prasad.vennam.moneypilot.ui.viewmodel.state.*
import prasad.vennam.moneypilot.util.FinanceMath
import prasad.vennam.moneypilot.util.inPaisa
import prasad.vennam.moneypilot.util.inRupees
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class InvestmentViewModel
    @Inject
    constructor(
        private val getInvestmentsUseCase: GetInvestmentsUseCase,
        private val updateInvestmentUseCase: UpdateInvestmentUseCase,
        private val exchangeRateRepo: ExchangeRateRepository,
        private val userPreferences: UserPreferences,
        private val saveInvestmentUseCase: SaveInvestmentUseCase,
        private val deleteInvestmentUseCase: DeleteInvestmentUseCase,
        private val financeRepository: FinanceRepository,
    ) : ViewModel() {
        val allInvestments: StateFlow<List<Investment>> =
            getInvestmentsUseCase()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val investmentSummary: StateFlow<InvestmentSummary> =
            combine(
                getInvestmentsUseCase(),
                exchangeRateRepo.allRates,
                userPreferences.currency,
            ) { investments, rates, currentCurrency ->
                fun convertAmount(
                    amountInPaisa: Long,
                    fromCurrency: String,
                ): Double {
                    if (fromCurrency == currentCurrency) return amountInPaisa / 100.0
                    val rateFrom = rates[fromCurrency] ?: 1.0
                    val rateTo = rates[currentCurrency] ?: 1.0
                    val amountInUSD = (amountInPaisa / 100.0) / rateFrom
                    return amountInUSD * rateTo
                }

                val totalInvested = investments.sumOf { convertAmount(it.investedAmount, it.currencyCode) }
                val totalCurrent = investments.sumOf { convertAmount(it.currentValue, it.currencyCode) }

                InvestmentSummary(totalInvested, totalCurrent)
            }.flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InvestmentSummary())

        private val _selectedProfile = MutableStateFlow(AllocationProfile.BALANCED)
        val selectedProfile: StateFlow<AllocationProfile> = _selectedProfile.asStateFlow()

        fun selectProfile(profile: AllocationProfile) {
            _selectedProfile.value = profile
        }

        val allocationDetails: StateFlow<List<AllocationDetail>> =
            combine(
                getInvestmentsUseCase(),
                exchangeRateRepo.allRates,
                userPreferences.currency,
                _selectedProfile,
            ) { investments, rates, currentCurrency, profile ->
                fun convertAmount(
                    amountInPaisa: Long,
                    fromCurrency: String,
                ): Double {
                    if (fromCurrency == currentCurrency) return amountInPaisa / 100.0
                    val rateFrom = rates[fromCurrency] ?: 1.0
                    val rateTo = rates[currentCurrency] ?: 1.0
                    val amountInUSD = (amountInPaisa / 100.0) / rateFrom
                    return amountInUSD * rateTo
                }

                val totalCurrent = investments.sumOf { convertAmount(it.currentValue, it.currencyCode) }
                val grouped = investments.groupBy { it.type }

                val targetWeights =
                    when (profile) {
                        AllocationProfile.BALANCED ->
                            mapOf(
                                "Mutual Fund" to 35.0,
                                "Stock" to 25.0,
                                "Gold" to 10.0,
                                "FD" to 15.0,
                                "Crypto" to 5.0,
                                "Real Estate" to 10.0,
                            )
                        AllocationProfile.AGGRESSIVE ->
                            mapOf(
                                "Mutual Fund" to 30.0,
                                "Stock" to 45.0,
                                "Gold" to 5.0,
                                "FD" to 5.0,
                                "Crypto" to 10.0,
                                "Real Estate" to 5.0,
                            )
                        AllocationProfile.CONSERVATIVE ->
                            mapOf(
                                "Mutual Fund" to 25.0,
                                "Stock" to 10.0,
                                "Gold" to 15.0,
                                "FD" to 35.0,
                                "Crypto" to 0.0,
                                "Real Estate" to 15.0,
                            )
                    }

                targetWeights.map { (type, targetPct) ->
                    val currentAmt = grouped[type]?.sumOf { convertAmount(it.currentValue, it.currencyCode) } ?: 0.0
                    val currentPct = if (totalCurrent > 0.0) (currentAmt / totalCurrent) * 100.0 else 0.0

                    val targetAmt = totalCurrent * (targetPct / 100.0)
                    val diffAmt = targetAmt - currentAmt
                    val diffPct = targetPct - currentPct

                    AllocationDetail(
                        assetType = type,
                        currentAmount = currentAmt,
                        currentPercent = currentPct,
                        targetPercent = targetPct,
                        differenceAmount = diffAmt,
                        differencePercent = diffPct,
                    )
                }
            }.flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // ── Live price refresh ────────────────────────────────────────────────────
        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing = _isRefreshing.asStateFlow()

        // ── Symbol search (debounced) ─────────────────────────────────────────────
        private val _symbolResults = MutableStateFlow<List<SymbolResult>>(emptyList())
        val symbolResults: StateFlow<List<SymbolResult>> = _symbolResults.asStateFlow()

        private val _isSearching = MutableStateFlow(false)
        val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

        private var searchJob: Job? = null

        // ── Auto-fill quantity ────────────────────────────────────────────────────
        private val _autoFillState = MutableStateFlow<AutoFillState>(AutoFillState.Idle)
        val autoFillState: StateFlow<AutoFillState> = _autoFillState.asStateFlow()

        init {
            viewModelScope.launch {
                // Wait for the first Room database emission
                getInvestmentsUseCase().first()
                refreshAllPrices()
            }
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Quantity auto-fill: fetch historical closing price on the investment date
        // then compute quantity = investedAmount / priceOnDate
        // ──────────────────────────────────────────────────────────────────────────
        fun fetchQuantityForDate(
            symbol: String,
            assetType: String,
            investedAmount: Double,
            dateMs: Long,
        ) {
            viewModelScope.launch {
                _autoFillState.value = AutoFillState.Loading
                val price =
                    try {
                        when {
                            assetType == "Mutual Fund" && symbol.all { it.isDigit() } ->
                                financeRepository.fetchNavOnDate(symbol, dateMs)
                            else ->
                                financeRepository.fetchPriceOnDate(symbol, dateMs)
                        }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        null
                    }
                _autoFillState.value =
                    if (price != null && price > 0) {
                        AutoFillState.Success(
                            quantity = investedAmount / price,
                            priceUsed = price,
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
        fun searchSymbols(
            query: String,
            assetType: String,
        ) {
            searchJob?.cancel()
            if (assetType == "Gold") {
                _symbolResults.value = financeRepository.goldSuggestions()
                return
            }
            if (query.length < 2) {
                _symbolResults.value = emptyList()
                return
            }
            searchJob =
                viewModelScope.launch {
                    delay(350.milliseconds)
                    _isSearching.value = true
                    _symbolResults.value =
                        try {
                            when (assetType) {
                                "Mutual Fund" -> financeRepository.searchMutualFunds(query)
                                else -> financeRepository.searchStockSymbols(query)
                            }
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
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

        fun refreshAllPrices() {
            viewModelScope.launch {
                if (_isRefreshing.value) return@launch
                _isRefreshing.value = true
                android.util.Log.i("InvestmentViewModel", "refreshAllPrices: starting refresh")
                try {
                    val investments = getInvestmentsUseCase().first()
                    android.util.Log.i("InvestmentViewModel", "refreshAllPrices: fetched ${investments.size} investments from DB")
                    val rates = exchangeRateRepo.allRates.first()

                    // Fetch updates in parallel
                    val updates =
                        kotlinx.coroutines
                            .coroutineScope {
                                investments.map { investment ->
                                    async {
                                        android.util.Log.d(
                                            "InvestmentViewModel",
                                            "refreshAllPrices: processing ${investment.name} (symbol: ${investment.symbol}, type: ${investment.type})",
                                        )
                                        val updatedValue =
                                            when (investment.type) {
                                                "Stock", "Crypto", "Gold" -> {
                                                    val sym = investment.symbol
                                                    val qty = investment.quantity ?: 0.0
                                                    if (!sym.isNullOrBlank() && qty > 0.0) {
                                                        val priceInfo = financeRepository.fetchYahooPrice(sym)
                                                        if (priceInfo != null) {
                                                            val rateFrom = rates[priceInfo.currency] ?: 1.0
                                                            val rateTo = rates[investment.currencyCode] ?: 1.0
                                                            val priceInUSD = priceInfo.price / rateFrom
                                                            val convertedPrice = priceInUSD * rateTo
                                                            android.util.Log.i(
                                                                "InvestmentViewModel",
                                                                "refreshAllPrices: parsed ${priceInfo.price} ${priceInfo.currency} for $sym, converted to $convertedPrice ${investment.currencyCode}",
                                                            )
                                                            convertedPrice * qty
                                                        } else {
                                                            android.util.Log.e(
                                                                "InvestmentViewModel",
                                                                "refreshAllPrices: failed to fetch Yahoo price for symbol $sym",
                                                            )
                                                            null
                                                        }
                                                    } else {
                                                        null
                                                    }
                                                }
                                                "Mutual Fund" -> {
                                                    val sym = investment.symbol
                                                    val qty = investment.quantity ?: 0.0
                                                    if (!sym.isNullOrBlank() && qty > 0.0) {
                                                        val priceInAssetCurrency =
                                                            if (sym.all { it.isDigit() }) {
                                                                val nav = financeRepository.fetchAmfiNav(sym)
                                                                if (nav != null) nav to "INR" else null
                                                            } else {
                                                                val priceInfo = financeRepository.fetchYahooPrice(sym)
                                                                if (priceInfo != null) priceInfo.price to priceInfo.currency else null
                                                            }
                                                        if (priceInAssetCurrency != null) {
                                                            val (price, assetCurrency) = priceInAssetCurrency
                                                            val rateFrom = rates[assetCurrency] ?: 1.0
                                                            val rateTo = rates[investment.currencyCode] ?: 1.0
                                                            val priceInUSD = price / rateFrom
                                                            val convertedPrice = priceInUSD * rateTo
                                                            android.util.Log.i(
                                                                "InvestmentViewModel",
                                                                "refreshAllPrices: parsed Mutual Fund NAV $price $assetCurrency for $sym, converted to $convertedPrice ${investment.currencyCode}",
                                                            )
                                                            convertedPrice * qty
                                                        } else {
                                                            android.util.Log.e(
                                                                "InvestmentViewModel",
                                                                "refreshAllPrices: failed to fetch AMFI/Yahoo NAV for symbol $sym",
                                                            )
                                                            null
                                                        }
                                                    } else {
                                                        null
                                                    }
                                                }
                                                "FD", "Real Estate" -> {
                                                    val rate = investment.interestRate ?: 0.0
                                                    val start = investment.startDate ?: 0L
                                                    if (rate > 0.0 && start > 0L) {
                                                        FinanceMath.calculateCompoundedValue(
                                                            investedAmount = investment.investedAmount.inRupees,
                                                            annualRate = rate,
                                                            startDate = start,
                                                        )
                                                    } else {
                                                        null
                                                    }
                                                }
                                                else -> null
                                            }
                                        if (updatedValue != null && updatedValue.inPaisa != investment.currentValue) {
                                            investment.copy(currentValue = updatedValue.inPaisa)
                                        } else {
                                            null
                                        }
                                    }
                                }
                            }.mapNotNull { it.await() }

                    var updatedAny = false
                    if (updates.isNotEmpty()) {
                        updates.forEach { updatedInvestment ->
                            android.util.Log.i(
                                "InvestmentViewModel",
                                "refreshAllPrices: updating currentValue for ${updatedInvestment.name} from DB value to ${updatedInvestment.currentValue}",
                            )
                            updateInvestmentUseCase(updatedInvestment)
                        }
                        updatedAny = true
                    }
                    if (updatedAny) {
                        userPreferences.setSynced(false)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("InvestmentViewModel", "refreshAllPrices: exception occurred", e)
                } finally {
                    _isRefreshing.value = false
                    android.util.Log.i("InvestmentViewModel", "refreshAllPrices: complete")
                }
            }
        }

        // ──────────────────────────────────────────────────────────────────────────
        // CRUD
        // ──────────────────────────────────────────────────────────────────────────
        fun saveInvestment(investment: Investment) {
            viewModelScope.launch {
                try {
                    saveInvestmentUseCase(investment)
                } catch (e: Exception) {
                    android.util.Log.e("InvestmentViewModel", "Error saving investment", e)
                }
            }
        }

        fun deleteInvestment(investment: Investment) {
            viewModelScope.launch {
                try {
                    deleteInvestmentUseCase(investment)
                } catch (e: Exception) {
                    android.util.Log.e("InvestmentViewModel", "Error deleting investment", e)
                }
            }
        }
    }
