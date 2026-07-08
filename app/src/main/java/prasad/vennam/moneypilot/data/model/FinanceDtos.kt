package prasad.vennam.moneypilot.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YahooChartResponse(
    val chart: YahooChartData,
)

@JsonClass(generateAdapter = true)
data class YahooChartData(
    val result: List<YahooChartResult>?,
)

@JsonClass(generateAdapter = true)
data class YahooChartResult(
    val meta: YahooChartMeta,
)

@JsonClass(generateAdapter = true)
data class YahooChartMeta(
    val regularMarketPrice: Double,
    val currency: String? = null,
)

@JsonClass(generateAdapter = true)
data class AmfiResponse(
    val meta: AmfiMeta,
    val data: List<AmfiData>?,
)

@JsonClass(generateAdapter = true)
data class AmfiMeta(
    @param:Json(name = "scheme_name") val schemeName: String,
)

@JsonClass(generateAdapter = true)
data class AmfiData(
    val date: String,
    val nav: String,
)

@JsonClass(generateAdapter = true)
data class YahooHistChart(
    val chart: YahooHistChartData,
)

@JsonClass(generateAdapter = true)
data class YahooHistChartData(
    val result: List<YahooHistResult>?,
)

@JsonClass(generateAdapter = true)
data class YahooHistResult(
    val timestamp: List<Long>?,
    val indicators: YahooHistIndicators?,
)

@JsonClass(generateAdapter = true)
data class YahooHistIndicators(
    val quote: List<YahooOhlcv>?,
)

@JsonClass(generateAdapter = true)
data class YahooOhlcv(
    val close: List<Double?>?,
)

@JsonClass(generateAdapter = true)
data class YahooSearchResponse(
    val quotes: List<YahooSearchQuote>?,
)

@JsonClass(generateAdapter = true)
data class YahooSearchQuote(
    val symbol: String?,
    val shortname: String?,
    val longname: String?,
    val exchange: String?,
    val exchDisp: String?,
    val quoteType: String?,
)

@JsonClass(generateAdapter = true)
data class AmfiSearchItem(
    val schemeCode: Int,
    val schemeName: String,
)

@JsonClass(generateAdapter = true)
data class ExchangeRateResponse(
    val result: String,
    @param:Json(name = "time_last_update_unix") val timeLastUpdateUnix: Long,
    val rates: Map<String, Double>,
)
