package prasad.vennam.moneypilot.ui.currency

data class CurrencyConverterState(
    val amount: String = "1.00",
    val fromCurrency: String = "USD",
    val toCurrency: String = "INR",
    val convertedAmount: String = "",
    val exchangeRateText: String = "",
    val lastUpdated: String = "",
    val currencies: List<CurrencyItem> = emptyList(),
    val recentConversions: List<Pair<String, String>> = emptyList(),
    val favoriteConversions: List<Pair<String, String>> = emptyList(),
    val isFavorite: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Premium features state
    val isComparisonMode: Boolean = false,
    val basketCurrencies: List<String> = emptyList(),
    val basketConversions: Map<String, String> = emptyMap(),
    val historicalRates: List<Double> = emptyList(),
    val historicalDates: List<String> = emptyList(),
    val activeAlerts: List<prasad.vennam.moneypilot.data.model.RateAlert> = emptyList(),
    val isLocationLoading: Boolean = false,
    val detectedCountry: String? = null,
)

data class CurrencyItem(
    val code: String,
    val name: String,
    val flag: String,
    val symbol: String,
)
