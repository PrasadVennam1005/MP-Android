package prasad.vennam.moneypilot.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saving_goals")
data class SavingGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Long, // In paise
    val currentSavedAmount: Long, // In paise
    val deadline: Long, // Timestamp
    val colorHex: String = "#3F51B5",
    val iconName: String = "Savings",
    val isCompleted: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
)
