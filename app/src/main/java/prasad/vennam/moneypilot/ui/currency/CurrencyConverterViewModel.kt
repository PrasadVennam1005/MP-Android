package prasad.vennam.moneypilot.ui.currency

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.model.RateAlert
import prasad.vennam.moneypilot.data.repository.ExchangeRateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CurrencyConverterViewModel @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrencyConverterState())
    val uiState: StateFlow<CurrencyConverterState> = _uiState.asStateFlow()

    private val _rates = MutableStateFlow<Map<String, Double>>(emptyMap())

    init {
        viewModelScope.launch {
            exchangeRateRepository.allRates.collect { ratesMap ->
                _rates.value = ratesMap
                recalculate()
            }
        }

        viewModelScope.launch {
            combine(
                userPreferences.recentCurrencyPairs,
                userPreferences.favoriteCurrencyPairs
            ) { recents, favorites ->
                Pair(recents, favorites)
            }.collect { (recents, favorites) ->
                _uiState.update {
                    it.copy(
                        recentConversions = recents,
                        favoriteConversions = favorites,
                        isFavorite = favorites.contains(Pair(it.fromCurrency, it.toCurrency))
                    )
                }
            }
        }

        viewModelScope.launch {
            userPreferences.currencyBasket.collect { basket ->
                _uiState.update { it.copy(basketCurrencies = basket) }
                recalculate()
            }
        }

        viewModelScope.launch {
            userPreferences.rateAlerts.collect { alerts ->
                _uiState.update { it.copy(activeAlerts = alerts) }
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                exchangeRateRepository.syncRates()
                loadCurrencies()
                fetchHistoricalRates()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun loadCurrencies() {
        val fetched = exchangeRateRepository.fetchCurrencies()
        val mappedList = if (fetched.isNotEmpty()) {
            fetched.map { item ->
                CurrencyItem(
                    code = item.isoCode,
                    name = item.name,
                    symbol = item.symbol ?: getSymbolFallback(item.isoCode),
                    flag = getFlagEmojiForCurrency(item.isoCode)
                )
            }
        } else {
            getStaticCurrencies()
        }

        _uiState.update {
            it.copy(
                currencies = mappedList.sortedBy { c -> c.code }
            )
        }
        recalculate()
    }

    fun setAmount(amount: String) {
        _uiState.update { it.copy(amount = amount) }
        recalculate()
    }

    fun selectFromCurrency(code: String) {
        _uiState.update {
            val isFav = it.favoriteConversions.contains(Pair(code, it.toCurrency))
            it.copy(fromCurrency = code, isFavorite = isFav)
        }
        saveRecent(code, _uiState.value.toCurrency)
        recalculate()
        fetchHistoricalRates()
    }

    fun selectToCurrency(code: String) {
        _uiState.update {
            val isFav = it.favoriteConversions.contains(Pair(it.fromCurrency, code))
            it.copy(toCurrency = code, isFavorite = isFav)
        }
        saveRecent(_uiState.value.fromCurrency, code)
        recalculate()
        fetchHistoricalRates()
    }

    fun swapCurrencies() {
        _uiState.update {
            val nextFrom = it.toCurrency
            val nextTo = it.fromCurrency
            val isFav = it.favoriteConversions.contains(Pair(nextFrom, nextTo))
            it.copy(
                fromCurrency = nextFrom,
                toCurrency = nextTo,
                isFavorite = isFav
            )
        }
        saveRecent(_uiState.value.fromCurrency, _uiState.value.toCurrency)
        recalculate()
        fetchHistoricalRates()
    }

    fun toggleFavorite() {
        val from = _uiState.value.fromCurrency
        val to = _uiState.value.toCurrency
        viewModelScope.launch {
            userPreferences.toggleFavoriteCurrencyPair(from, to)
        }
    }

    fun selectPair(from: String, to: String) {
        _uiState.update {
            val isFav = it.favoriteConversions.contains(Pair(from, to))
            it.copy(
                fromCurrency = from,
                toCurrency = to,
                isFavorite = isFav
            )
        }
        saveRecent(from, to)
        recalculate()
        fetchHistoricalRates()
    }

    fun refreshRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                exchangeRateRepository.syncRates()
                loadCurrencies()
                fetchHistoricalRates()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun saveRecent(from: String, to: String) {
        viewModelScope.launch {
            userPreferences.saveRecentCurrencyPair(from, to)
        }
    }

    private fun recalculate() {
        val state = _uiState.value
        val rates = _rates.value

        if (rates.isEmpty()) {
            _uiState.update {
                it.copy(
                    convertedAmount = "",
                    exchangeRateText = context.getString(prasad.vennam.moneypilot.R.string.no_rates_available),
                    basketConversions = emptyMap()
                )
            }
            return
        }

        val amountVal = state.amount.toDoubleOrNull() ?: 0.0
        val rateFrom = rates[state.fromCurrency] ?: 1.0
        val rateTo = rates[state.toCurrency] ?: 1.0

        val converted = if (rateFrom > 0) amountVal * (rateTo / rateFrom) else 0.0
        val singleRate = if (rateFrom > 0) rateTo / rateFrom else 0.0

        val formattedResult = String.format(Locale.US, "%,.2f", converted)
        val formattedRate = String.format(Locale.US, "1 %s = %.5f %s", state.fromCurrency, singleRate, state.toCurrency)

        val updatedTime = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())

        val basketMap = state.basketCurrencies.associateWith { toCode ->
            val rateBasketTo = rates[toCode] ?: 1.0
            val basketConverted = if (rateFrom > 0) amountVal * (rateBasketTo / rateFrom) else 0.0
            String.format(Locale.US, "%.2f", basketConverted)
        }

        _uiState.update {
            it.copy(
                convertedAmount = formattedResult,
                exchangeRateText = formattedRate,
                lastUpdated = updatedTime,
                basketConversions = basketMap
            )
        }
    }

    private fun getSymbolFallback(code: String): String {
        return when (code.uppercase()) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "INR" -> "₹"
            "JPY" -> "¥"
            "AUD" -> "A$"
            "CAD" -> "C$"
            "CHF" -> "CHF"
            "CNY" -> "元"
            "NZD" -> "NZ$"
            "SGD" -> "S$"
            "HKD" -> "HK$"
            "SEK" -> "kr"
            "NOK" -> "kr"
            "KRW" -> "₩"
            "TRY" -> "₺"
            "RUB" -> "₽"
            "BRL" -> "R$"
            "ZAR" -> "R"
            "AED" -> "د.إ"
            "THB" -> "฿"
            "MYR" -> "RM"
            "IDR" -> "Rp"
            "PHP" -> "₱"
            "ILS" -> "₪"
            "PLN" -> "zł"
            "DKK" -> "kr"
            "MXN" -> "$"
            else -> code
        }
    }

    private fun getStaticCurrencies(): List<CurrencyItem> {
        val codes = listOf(
            "USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD", "CHF", "CNY", "NZD",
            "SGD", "HKD", "SEK", "NOK", "KRW", "TRY", "RUB", "BRL", "ZAR", "AED",
            "THB", "MYR", "IDR", "PHP", "ILS", "PLN", "DKK", "MXN"
        )
        val names = listOf(
            "United States Dollar", "Euro", "British Pound", "Indian Rupee", "Japanese Yen",
            "Australian Dollar", "Canadian Dollar", "Swiss Franc", "Chinese Yuan", "New Zealand Dollar",
            "Singapore Dollar", "Hong Kong Dollar", "Swedish Krona", "Norwegian Krone", "South Korean Won",
            "Turkish Lira", "Russian Ruble", "Brazilian Real", "South African Rand", "UAE Dirham",
            "Thai Baht", "Malaysian Ringgit", "Indonesian Rupiah", "Philippine Peso", "Israeli New Shekel",
            "Polish Zloty", "Danish Krone", "Mexican Peso"
        )
        return codes.indices.map { i ->
            CurrencyItem(
                code = codes[i],
                name = names[i],
                symbol = getSymbolFallback(codes[i]),
                flag = getFlagEmojiForCurrency(codes[i])
            )
        }
    }

    private fun getFlagEmojiForCurrency(currencyCode: String): String {
        val countryCode = when (currencyCode.uppercase()) {
            "EUR" -> "EU"
            "USD" -> "US"
            "GBP" -> "GB"
            "AUD" -> "AU"
            "CAD" -> "CA"
            "CHF" -> "CH"
            "CNY" -> "CN"
            "SEK" -> "SE"
            "NZD" -> "NZ"
            "MXN" -> "MX"
            "SGD" -> "SG"
            "HKD" -> "HK"
            "NOK" -> "NO"
            "KRW" -> "KR"
            "TRY" -> "TR"
            "RUB" -> "RU"
            "INR" -> "IN"
            "BRL" -> "BR"
            "ZAR" -> "ZA"
            "PHP" -> "PH"
            "CZK" -> "CZ"
            "PLN" -> "PL"
            "THB" -> "TH"
            "MYR" -> "MY"
            "HUF" -> "HU"
            "DKK" -> "DK"
            "ILS" -> "IL"
            "IDR" -> "ID"
            "RON" -> "RO"
            "ISK" -> "IS"
            "HRK" -> "HR"
            "BGN" -> "BG"
            "JPY" -> "JP"
            else -> currencyCode.take(2)
        }
        if (countryCode.length != 2) return "🏳"
        return try {
            val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
            val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
            String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
        } catch (e: Exception) {
            "🏳"
        }
    }

    fun setComparisonMode(enabled: Boolean) {
        _uiState.update { it.copy(isComparisonMode = enabled) }
        recalculate()
    }

    fun addToBasket(code: String) {
        viewModelScope.launch {
            val current = _uiState.value.basketCurrencies.toMutableList()
            if (!current.contains(code)) {
                current.add(code)
                userPreferences.saveCurrencyBasket(current)
            }
        }
    }

    fun removeFromBasket(code: String) {
        viewModelScope.launch {
            val current = _uiState.value.basketCurrencies.toMutableList()
            if (current.contains(code)) {
                current.remove(code)
                userPreferences.saveCurrencyBasket(current)
            }
        }
    }

    fun addRateAlert(targetRate: Double, isAbove: Boolean) {
        val state = _uiState.value
        viewModelScope.launch {
            val alert = RateAlert(
                from = state.fromCurrency,
                to = state.toCurrency,
                targetRate = targetRate,
                isAbove = isAbove
            )
            userPreferences.addRateAlert(alert)
        }
    }

    fun removeRateAlert(alert: RateAlert) {
        viewModelScope.launch {
            userPreferences.removeRateAlert(alert)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun detectLocationByIp() {
        _uiState.update { it.copy(isLocationLoading = true) }
        viewModelScope.launch {
            val countryCode = exchangeRateRepository.detectCountryByIp()
            if (countryCode != null) {
                val detectedCurrency = getCurrencyForCountryCode(countryCode)
                if (detectedCurrency != null) {
                    _uiState.update {
                        it.copy(
                            toCurrency = detectedCurrency,
                            detectedCountry = countryCode,
                            isLocationLoading = false
                        )
                    }
                    saveRecent(_uiState.value.fromCurrency, detectedCurrency)
                    recalculate()
                    fetchHistoricalRates()
                } else {
                    _uiState.update {
                        it.copy(
                            isLocationLoading = false,
                            error = context.getString(prasad.vennam.moneypilot.R.string.currency_not_supported, countryCode)
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLocationLoading = false,
                        error = context.getString(prasad.vennam.moneypilot.R.string.could_not_resolve_country)
                    )
                }
            }
        }
    }

    private fun getCurrencyForCountryCode(countryCode: String): String? {
        return when (countryCode.uppercase()) {
            "US" -> "USD"
            "IN" -> "INR"
            "GB", "UK" -> "GBP"
            "JP" -> "JPY"
            "AU" -> "AUD"
            "CA" -> "CAD"
            "CH" -> "CHF"
            "CN" -> "CNY"
            "NZ" -> "NZD"
            "SG" -> "SGD"
            "HK" -> "HKD"
            "SE" -> "SEK"
            "NO" -> "NOK"
            "KR" -> "KRW"
            "TR" -> "TRY"
            "RU" -> "RUB"
            "BR" -> "BRL"
            "ZA" -> "ZAR"
            "AE" -> "AED"
            "TH" -> "THB"
            "MY" -> "MYR"
            "ID" -> "IDR"
            "PH" -> "PHP"
            "IL" -> "ILS"
            "PL" -> "PLN"
            "DK" -> "DKK"
            "MX" -> "MXN"
            "AT", "BE", "CY", "EE", "FI", "FR", "DE", "GR", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PT", "SK", "SI", "ES" -> "EUR"
            else -> null
        }
    }

    private fun fetchHistoricalRates() {
        val state = _uiState.value
        val fromCode = state.fromCurrency
        val toCode = state.toCurrency

        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val toDate = sdf.format(Date())
            val fromCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -30)
            }
            val fromDate = sdf.format(fromCal.time)

            val historicalItems = exchangeRateRepository.fetchHistoricalRates(
                from = fromDate,
                to = toDate,
                base = fromCode,
                quote = toCode
            )

            if (historicalItems.isNotEmpty()) {
                val rates = historicalItems.map { it.rate }
                val dates = historicalItems.map { it.date }
                _uiState.update {
                    it.copy(
                        historicalRates = rates,
                        historicalDates = dates
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        historicalRates = emptyList(),
                        historicalDates = emptyList()
                    )
                }
            }
        }
    }
}
