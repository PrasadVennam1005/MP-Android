package prasad.vennam.moneypilot.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "loan_payments",
    foreignKeys = [
        ForeignKey(
            entity = Loan::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("loanId")],
)
data class LoanPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val amount: Long, // Stored in minor units
    val date: Long,
    val isExtraPayment: Boolean = false,
    val note: String = "",
    val paymentMode: String = "Cash",
)
