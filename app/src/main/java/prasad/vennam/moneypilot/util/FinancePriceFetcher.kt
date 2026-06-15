package prasad.vennam.moneypilot.util

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.pow

object FinancePriceFetcher {
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    private val moshi =
        Moshi
            .Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

    private val amfiDateFmt = SimpleDateFormat("dd-MM-yyyy", Locale.US)

    // ─── Live price response models ───────────────────────────────────────────
    data class YahooPriceInfo(
        val price: Double,
        val currency: String,
    )

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
        val scheme_name: String,
    )

    data class AmfiData(
        val date: String,
        val nav: String,
    )

    // ─── Historical price response models ─────────────────────────────────────
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

    // ─── Search result model ──────────────────────────────────────────────────
    data class SymbolResult(
        val symbol: String,
        val name: String,
        val exchange: String = "",
    )

    // ─── Yahoo Finance search models ──────────────────────────────────────────
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

    // ─── AMFI search model ────────────────────────────────────────────────────
    data class AmfiSearchItem(
        val schemeCode: Int,
        val schemeName: String,
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Live price: Yahoo Finance (Stocks, Crypto, Gold, ETFs)
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun fetchYahooPrice(symbol: String): YahooPriceInfo? =
        withContext(Dispatchers.IO) {
            runCatching {
                val req =
                    Request
                        .Builder()
                        .url("https://query1.finance.yahoo.com/v8/finance/chart/$symbol")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val meta =
                        moshi
                            .adapter(YahooChartResponse::class.java)
                            .fromJson(resp.body.string())
                            ?.chart
                            ?.result
                            ?.firstOrNull()
                            ?.meta ?: return@withContext null

                    val rawCurrency = meta.currency ?: "USD"
                    val (normalizedCurrency, normalizedPrice) =
                        if (rawCurrency.equals("GBp", ignoreCase = true)) {
                            "GBP" to meta.regularMarketPrice / 100.0
                        } else {
                            rawCurrency.uppercase() to meta.regularMarketPrice
                        }
                    YahooPriceInfo(normalizedPrice, normalizedCurrency)
                }
            }.onFailure {
                Log.e("FinancePriceFetcher", "fetchYahooPrice failed for $symbol", it)
            }.getOrNull()
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Live NAV: mfapi.in (Mutual Funds)
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun fetchAmfiNav(schemeCode: String): Double? =
        withContext(Dispatchers.IO) {
            runCatching {
                val req =
                    Request
                        .Builder()
                        .url("https://api.mfapi.in/mf/$schemeCode")
                        .header("User-Agent", "Mozilla/5.0")
                        .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    moshi
                        .adapter(AmfiResponse::class.java)
                        .fromJson(resp.body.string())
                        ?.data
                        ?.firstOrNull()
                        ?.nav
                        ?.toDoubleOrNull()
                }
            }.onFailure {
                Log.e("FinancePriceFetcher", "fetchAmfiNav failed for $schemeCode", it)
            }.getOrNull()
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Historical price: Yahoo Finance – closest trading day close on/before dateMs
    // Queries a 14-day window ending at dateMs+1day to handle weekends/holidays
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun fetchPriceOnDate(
        symbol: String,
        dateMs: Long,
    ): Double? =
        withContext(Dispatchers.IO) {
            runCatching {
                val targetSec = dateMs / 1000
                val period1 = targetSec - 14L * 86400L // 14 days back
                val period2 = targetSec + 86400L // 1 day forward (inclusive)

                val req =
                    Request
                        .Builder()
                        .url(
                            "https://query1.finance.yahoo.com/v8/finance/chart/$symbol" +
                                "?period1=$period1&period2=$period2&interval=1d",
                        ).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .build()

                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val histChart =
                        moshi
                            .adapter(YahooHistChart::class.java)
                            .fromJson(resp.body.string())
                    val result =
                        histChart?.chart?.result?.firstOrNull()
                            ?: return@withContext null

                    val timestamps = result.timestamp ?: return@withContext null
                    val closes =
                        result.indicators
                            ?.quote
                            ?.firstOrNull()
                            ?.close
                            ?: return@withContext null

                    // Walk backwards from the end of the array: pick the last
                    // close that belongs to a candle whose timestamp ≤ targetSec
                    // (Yahoo timestamps mark candle open, so +86400 for full day)
                    var bestPrice: Double? = null
                    for (i in timestamps.indices) {
                        val ts = timestamps[i]
                        val close = closes.getOrNull(i) ?: continue
                        if (close > 0 && ts <= targetSec + 86400L) {
                            bestPrice = close
                        }
                    }
                    bestPrice
                }
            }.onFailure {
                Log.e("FinancePriceFetcher", "fetchPriceOnDate failed for $symbol", it)
            }.getOrNull()
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Historical NAV: mfapi.in – closest NAV on/before dateMs
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun fetchNavOnDate(
        schemeCode: String,
        dateMs: Long,
    ): Double? =
        withContext(Dispatchers.IO) {
            runCatching {
                val req =
                    Request
                        .Builder()
                        .url("https://api.mfapi.in/mf/$schemeCode")
                        .header("User-Agent", "Mozilla/5.0")
                        .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val allData =
                        moshi
                            .adapter(AmfiResponse::class.java)
                            .fromJson(resp.body.string())
                            ?.data
                            ?: return@withContext null

                    val targetDate = Date(dateMs)
                    // data is sorted most-recent first; walk until we find a date ≤ target
                    for (entry in allData) {
                        val entryDate =
                            runCatching {
                                amfiDateFmt.parse(entry.date)
                            }.getOrNull() ?: continue
                        if (!entryDate.after(targetDate)) {
                            return@withContext entry.nav.toDoubleOrNull()
                        }
                    }
                    null
                }
            }.onFailure {
                Log.e("FinancePriceFetcher", "fetchNavOnDate failed for $schemeCode", it)
            }.getOrNull()
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Symbol search: Yahoo Finance
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun searchStockSymbols(query: String): List<SymbolResult> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            runCatching {
                val req =
                    Request
                        .Builder()
                        .url(
                            "https://query1.finance.yahoo.com/v1/finance/search" +
                                "?q=${query.trim()}&quotesCount=8&newsCount=0&listsCount=0",
                        ).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext emptyList()
                    moshi
                        .adapter(YahooSearchResponse::class.java)
                        .fromJson(resp.body.string())
                        ?.quotes
                        ?.filter {
                            it.symbol != null &&
                                it.quoteType in listOf("EQUITY", "CRYPTOCURRENCY", "ETF", "FUTURE")
                        }?.map { q ->
                            SymbolResult(
                                symbol = q.symbol ?: "",
                                name = q.longname ?: q.shortname ?: q.symbol ?: "",
                                exchange = q.exchDisp ?: q.exchange ?: "",
                            )
                        } ?: emptyList()
                }
            }.getOrElse { emptyList() }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Symbol search: mfapi.in (Mutual Funds)
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun searchMutualFunds(query: String): List<SymbolResult> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            runCatching {
                val req =
                    Request
                        .Builder()
                        .url("https://api.mfapi.in/mf/search?q=${query.trim().replace(" ", "+")}")
                        .header("User-Agent", "Mozilla/5.0")
                        .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext emptyList()
                    val listType =
                        moshi.adapter<List<AmfiSearchItem>>(
                            com.squareup.moshi.Types.newParameterizedType(
                                List::class.java,
                                AmfiSearchItem::class.java,
                            ),
                        )
                    listType
                        .fromJson(resp.body.string())
                        ?.take(8)
                        ?.map { SymbolResult(it.schemeCode.toString(), it.schemeName, "AMFI") }
                        ?: emptyList()
                }
            }.getOrElse { emptyList() }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Gold fixed suggestions
    // ─────────────────────────────────────────────────────────────────────────
    fun goldSuggestions(): List<SymbolResult> =
        listOf(
            SymbolResult("GC=F", "Gold Futures (COMEX) – USD/oz", "COMEX"),
            SymbolResult("GOLDBEES.NS", "Nippon India Gold BeES ETF", "NSE"),
            SymbolResult("HDFCGOLD.NS", "HDFC Gold Exchange Traded Fund", "NSE"),
        )

    // ─────────────────────────────────────────────────────────────────────────
    // Compound interest (FD & Real Estate – no API needed)
    // ─────────────────────────────────────────────────────────────────────────
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
