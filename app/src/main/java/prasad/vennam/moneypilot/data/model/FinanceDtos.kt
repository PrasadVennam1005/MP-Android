package prasad.vennam.moneypilot.data.model

import com.squareup.moshi.Json

data class YahooChartResponse(
    val chart: YahooChartData,
)

data class YahooChartData(
    val result: List<YahooChartResult>?,
)

data class YahooChartResult(
    val meta: YahooChartMeta,
)

data class YahooChartMeta(
    val regularMarketPrice: Double,
    val currency: String? = null,
)

data class AmfiResponse(
    val meta: AmfiMeta,
    val data: List<AmfiData>?,
)

data class AmfiMeta(
    @param:Json(name = "scheme_name") val schemeName: String,
)

data class AmfiData(
    val date: String,
    val nav: String,
)

data class YahooHistChart(
    val chart: YahooHistChartData,
)

data class YahooHistChartData(
    val result: List<YahooHistResult>?,
)

data class YahooHistResult(
    val timestamp: List<Long>?,
    val indicators: YahooHistIndicators?,
)

data class YahooHistIndicators(
    val quote: List<YahooOhlcv>?,
)

data class YahooOhlcv(
    val close: List<Double?>?,
)

data class YahooSearchResponse(
    val quotes: List<YahooSearchQuote>?,
)

data class YahooSearchQuote(
    val symbol: String?,
    val shortname: String?,
    val longname: String?,
    val exchange: String?,
    val exchDisp: String?,
    val quoteType: String?,
)

data class AmfiSearchItem(
    val schemeCode: Int,
    val schemeName: String,
)

data class ExchangeRateResponse(
    val result: String,
    @param:Json(name = "time_last_update_unix") val timeLastUpdateUnix: Long,
    val rates: Map<String, Double>,
)
