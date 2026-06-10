package prasad.vennam.moneypilot.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val totalAmount: Long, // Stored in minor units (e.g., Paise)
    val outstandingAmount: Long,
    val emiAmount: Long,
    val nextEmiDate: Long,
    val currencyCode: String = "INR"
)
