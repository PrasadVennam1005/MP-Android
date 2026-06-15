package prasad.vennam.moneypilot.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_fund")
data class EmergencyFund(
    @PrimaryKey val id: Long = 1,
    val monthlyExpenses: Double,
    val targetMonths: Int,
    val currentSaved: Double,
)
