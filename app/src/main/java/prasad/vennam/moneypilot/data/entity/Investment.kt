package prasad.vennam.moneypilot.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investments")
data class Investment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // e.g., Stock, Mutual Fund, Crypto, Real Estate
    val investedAmount: Double,
    val currentValue: Double,
    val symbol: String? = null,
    val quantity: Double? = null,
    val interestRate: Double? = null,
    val startDate: Long? = null
)
