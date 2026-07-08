package prasad.vennam.moneypilot.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "autopay_alerts")
data class AutopayAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val amount: Double,
    val scheduledDate: Long,
    val upiMandateId: String?,
    val paymentApp: String?,
    val status: String = "PENDING", // PENDING, CANCELLED, EXECUTED
    val rawMessage: String,
    val timestamp: Long,
)
