package prasad.vennam.moneypilot.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
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
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val timestamp: Long,
    val categoryId: Long?,
    val subCategory: String = "",
    val paymentMode: String = "Cash",
    val note: String,
    val type: TransactionType,
    val currencyCode: String = "INR",
)

enum class TransactionType {
    INCOME,
    EXPENSE,
}
