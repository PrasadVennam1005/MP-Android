package prasad.vennam.moneypilot.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import prasad.vennam.moneypilot.data.dao.ExchangeRateDao
import prasad.vennam.moneypilot.data.entity.ExchangeRate
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRateRepository
    @Inject
    constructor(
        private val exchangeRateDao: ExchangeRateDao,
    ) {
        val allRates: Flow<Map<String, Double>> =
            exchangeRateDao.getAllRates().map { list ->
                list.associate { it.currencyCode to it.rateAgainstUSD }
            }

        suspend fun syncRates() {
            withContext(Dispatchers.IO) {
                try {
                    val url = "https://open.er-api.com/v6/latest/USD"
                    val response = URL(url).readText()
                    val json = JSONObject(response)

                    if (json.getString("result") == "success") {
                        val ratesObj = json.getJSONObject("rates")
                        val timeLastUpdateUnix = json.getLong("time_last_update_unix")

                        val ratesList = mutableListOf<ExchangeRate>()
                        ratesObj.keys().forEach { code ->
                            val rate = ratesObj.getDouble(code)
                            ratesList.add(ExchangeRate(code, rate, timeLastUpdateUnix))
                        }

                        exchangeRateDao.insertRates(ratesList)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        suspend fun getRateForCurrency(code: String): Double = exchangeRateDao.getRate(code) ?: 1.0
    }
