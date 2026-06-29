package prasad.vennam.moneypilot.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId")],
)
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long, // In paise
    val billingCycle: String, // "Weekly", "Monthly", "Yearly"
    val nextPaymentDate: Long, // Timestamp
    val paymentMode: String = "UPI",
    val categoryId: Long?,
    val isNotificationEnabled: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis(),
)
