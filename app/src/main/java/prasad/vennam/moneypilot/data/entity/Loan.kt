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
    val currencyCode: String = "INR",
    val lenderName: String = "",
    val interestRate: Double = 0.0,
    val tenureMonths: Int = 12,
    val dueDayOfMonth: Int = 1,
    val isNotificationEnabled: Boolean = true,
    val startDate: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
)
