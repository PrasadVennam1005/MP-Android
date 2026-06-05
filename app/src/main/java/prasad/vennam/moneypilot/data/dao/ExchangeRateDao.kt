package prasad.vennam.moneypilot.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.ExchangeRate

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates")
    fun getAllRates(): Flow<List<ExchangeRate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRate>)
    
    @Query("SELECT rateAgainstUSD FROM exchange_rates WHERE currencyCode = :code")
    suspend fun getRate(code: String): Double?
}
