package prasad.vennam.moneypilot.data.repository

import android.util.Log
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import prasad.vennam.moneypilot.data.model.*
import prasad.vennam.moneypilot.domain.model.SymbolResult
import prasad.vennam.moneypilot.domain.model.YahooPriceInfo
import prasad.vennam.moneypilot.domain.repository.FinanceRepository
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class FinanceRepositoryImpl
    @Inject
    constructor(
        private val client: OkHttpClient,
        private val moshi: Moshi,
    ) : FinanceRepository {
        private val amfiDateFmt = SimpleDateFormat("dd-MM-yyyy", Locale.US)

        private suspend fun <T> executeWithRetry(
            times: Int = 3,
            initialDelayMs: Long = 1000L,
            block: suspend () -> T?,
        ): T? {
            var lastThrowable: Throwable? = null
            for (attempt in 1..times) {
                try {
                    return block()
                } catch (e: IOException) {
                    lastThrowable = e
                    if (attempt < times) {
                        delay((initialDelayMs * attempt).milliseconds)
                    }
                }
            }
            lastThrowable?.let { throw it }
            return null
        }

        override suspend fun fetchYahooPrice(symbol: String): YahooPriceInfo? =
            withContext(Dispatchers.IO) {
                runCatching {
                    executeWithRetry {
                        val req =
                            Request
                                .Builder()
                                .url("https://query1.finance.yahoo.com/v8/finance/chart/$symbol")
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                .build()
                        client.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) return@executeWithRetry null
                            val meta =
                                moshi
                                    .adapter(YahooChartResponse::class.java)
                                    .fromJson(resp.body.string())
                                    ?.chart
                                    ?.result
                                    ?.firstOrNull()
                                    ?.meta ?: return@executeWithRetry null

                            val rawCurrency = meta.currency ?: "USD"
                            val (normalizedCurrency, normalizedPrice) =
                                if (rawCurrency.equals("GBp", ignoreCase = true)) {
                                    "GBP" to meta.regularMarketPrice / 100.0
                                } else {
                                    rawCurrency.uppercase() to meta.regularMarketPrice
                                }
                            YahooPriceInfo(normalizedPrice, normalizedCurrency)
                        }
                    }
                }.onFailure {
                    Log.e("FinanceRepository", "fetchYahooPrice failed for $symbol", it)
                }.getOrNull()
            }

        override suspend fun fetchAmfiNav(schemeCode: String): Double? =
            withContext(Dispatchers.IO) {
                runCatching {
                    executeWithRetry {
                        val req =
                            Request
                                .Builder()
                                .url("https://api.mfapi.in/mf/$schemeCode")
                                .header("User-Agent", "Mozilla/5.0")
                                .build()
                        client.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) return@executeWithRetry null
                            moshi
                                .adapter(AmfiResponse::class.java)
                                .fromJson(resp.body.string())
                                ?.data
                                ?.firstOrNull()
                                ?.nav
                                ?.toDoubleOrNull()
                        }
                    }
                }.onFailure {
                    Log.e("FinanceRepository", "fetchAmfiNav failed for $schemeCode", it)
                }.getOrNull()
            }

        override suspend fun fetchPriceOnDate(
            symbol: String,
            dateMs: Long,
        ): Double? =
            withContext(Dispatchers.IO) {
                runCatching {
                    val targetSec = dateMs / 1000
                    val period1 = targetSec - 14L * 86400L
                    val period2 = targetSec + 86400L

                    executeWithRetry {
                        val req =
                            Request
                                .Builder()
                                .url("https://query1.finance.yahoo.com/v8/finance/chart/$symbol?period1=$period1&period2=$period2&interval=1d")
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                .build()

                        client.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) return@executeWithRetry null
                            val histChart = moshi.adapter(YahooHistChart::class.java).fromJson(resp.body.string())
                            val result = histChart?.chart?.result?.firstOrNull() ?: return@executeWithRetry null

                            val timestamps = result.timestamp ?: return@executeWithRetry null
                            val closes =
                                result.indicators
                                    ?.quote
                                    ?.firstOrNull()
                                    ?.close ?: return@executeWithRetry null

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
                    }
                }.onFailure {
                    Log.e("FinanceRepository", "fetchPriceOnDate failed for $symbol", it)
                }.getOrNull()
            }

        override suspend fun fetchNavOnDate(
            schemeCode: String,
            dateMs: Long,
        ): Double? =
            withContext(Dispatchers.IO) {
                runCatching {
                    executeWithRetry {
                        val req =
                            Request
                                .Builder()
                                .url("https://api.mfapi.in/mf/$schemeCode")
                                .header("User-Agent", "Mozilla/5.0")
                                .build()
                        client.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) return@executeWithRetry null
                            val allData = moshi.adapter(AmfiResponse::class.java).fromJson(resp.body.string())?.data ?: return@executeWithRetry null

                            val targetDate = Date(dateMs)
                            for (entry in allData) {
                                val entryDate = runCatching { amfiDateFmt.parse(entry.date) }.getOrNull() ?: continue
                                if (!entryDate.after(targetDate)) {
                                    return@executeWithRetry entry.nav.toDoubleOrNull()
                                }
                            }
                            null
                        }
                    }
                }.onFailure {
                    Log.e("FinanceRepository", "fetchNavOnDate failed for $schemeCode", it)
                }.getOrNull()
            }

        override suspend fun searchStockSymbols(query: String): List<SymbolResult> =
            withContext(Dispatchers.IO) {
                if (query.isBlank()) return@withContext emptyList()
                runCatching {
                    executeWithRetry {
                        val req =
                            Request
                                .Builder()
                                .url("https://query1.finance.yahoo.com/v1/finance/search?q=${query.trim()}&quotesCount=8&newsCount=0&listsCount=0")
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                .build()
                        client.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) return@executeWithRetry emptyList()
                            moshi
                                .adapter(YahooSearchResponse::class.java)
                                .fromJson(resp.body.string())
                                ?.quotes
                                ?.filter {
                                    it.symbol != null && it.quoteType in listOf("EQUITY", "CRYPTOCURRENCY", "ETF", "FUTURE")
                                }?.map { q ->
                                    SymbolResult(
                                        symbol = q.symbol ?: "",
                                        name = q.longname ?: q.shortname ?: q.symbol ?: "",
                                        exchange = q.exchDisp ?: q.exchange ?: "",
                                    )
                                } ?: emptyList()
                        }
                    } ?: emptyList()
                }.getOrElse { emptyList() }
            }

        override suspend fun searchMutualFunds(query: String): List<SymbolResult> =
            withContext(Dispatchers.IO) {
                if (query.isBlank()) return@withContext emptyList()
                runCatching {
                    executeWithRetry {
                        val req =
                            Request
                                .Builder()
                                .url("https://api.mfapi.in/mf/search?q=${query.trim().replace(" ", "+")}")
                                .header("User-Agent", "Mozilla/5.0")
                                .build()
                        client.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) return@executeWithRetry emptyList()
                            val listType =
                                moshi.adapter<List<AmfiSearchItem>>(
                                    com.squareup.moshi.Types
                                        .newParameterizedType(List::class.java, AmfiSearchItem::class.java),
                                )
                            listType
                                .fromJson(resp.body.string())
                                ?.take(8)
                                ?.map { SymbolResult(it.schemeCode.toString(), it.schemeName, "AMFI") }
                                ?: emptyList()
                        }
                    } ?: emptyList()
                }.getOrElse { emptyList() }
            }

        override fun goldSuggestions(): List<SymbolResult> =
            listOf(
                SymbolResult("GC=F", "Gold Futures (COMEX) – USD/oz", "COMEX"),
                SymbolResult("GOLDBEES.NS", "Nippon India Gold BeES ETF", "NSE"),
                SymbolResult("HDFCGOLD.NS", "HDFC Gold Exchange Traded Fund", "NSE"),
            )
    }
