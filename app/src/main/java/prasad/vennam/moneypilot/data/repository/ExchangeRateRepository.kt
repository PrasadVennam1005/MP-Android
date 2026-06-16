package prasad.vennam.moneypilot.data.repository

import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import prasad.vennam.moneypilot.data.dao.ExchangeRateDao
import prasad.vennam.moneypilot.data.entity.ExchangeRate
import prasad.vennam.moneypilot.data.model.ExchangeRateResponse
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class ExchangeRateRepository
    @Inject
    constructor(
        private val exchangeRateDao: ExchangeRateDao,
        private val client: OkHttpClient,
        private val moshi: Moshi,
    ) {
        val allRates: Flow<Map<String, Double>> =
            exchangeRateDao.getAllRates().map { list ->
                list.associate { it.currencyCode to it.rateAgainstUSD }
            }

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

        suspend fun syncRates() {
            withContext(Dispatchers.IO) {
                runCatching {
                    executeWithRetry {
                        val req =
                            Request
                                .Builder()
                                .url("https://open.er-api.com/v6/latest/USD")
                                .build()
                        client.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) return@executeWithRetry null
                            val parsed =
                                moshi
                                    .adapter(ExchangeRateResponse::class.java)
                                    .fromJson(resp.body.string())
                            if (parsed?.result == "success") {
                                parsed
                            } else {
                                null
                            }
                        }
                    }?.let { parsed ->
                        val ratesList =
                            parsed.rates.map { (code, rate) ->
                                ExchangeRate(code, rate, parsed.timeLastUpdateUnix)
                            }
                        exchangeRateDao.insertRates(ratesList)
                    }
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }

        suspend fun getRateForCurrency(code: String): Double = exchangeRateDao.getRate(code) ?: 1.0
    }
