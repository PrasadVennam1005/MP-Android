package prasad.vennam.moneypilot.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRate(
    @PrimaryKey val currencyCode: String,
    val rateAgainstUSD: Double,
    val lastUpdated: Long
)
