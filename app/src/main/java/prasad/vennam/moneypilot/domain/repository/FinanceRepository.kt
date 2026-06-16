package prasad.vennam.moneypilot.domain.repository

import prasad.vennam.moneypilot.domain.model.SymbolResult
import prasad.vennam.moneypilot.domain.model.YahooPriceInfo

interface FinanceRepository {
    suspend fun fetchYahooPrice(symbol: String): YahooPriceInfo?

    suspend fun fetchAmfiNav(schemeCode: String): Double?

    suspend fun fetchPriceOnDate(
        symbol: String,
        dateMs: Long,
    ): Double?

    suspend fun fetchNavOnDate(
        schemeCode: String,
        dateMs: Long,
    ): Double?

    suspend fun searchStockSymbols(query: String): List<SymbolResult>

    suspend fun searchMutualFunds(query: String): List<SymbolResult>

    fun goldSuggestions(): List<SymbolResult>
}
