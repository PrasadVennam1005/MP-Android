package prasad.vennam.moneypilot.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_transactions")
data class PendingTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val merchant: String,
    val bankAccount: String,
    val rawMessage: String,
    val timestamp: Long,
)
