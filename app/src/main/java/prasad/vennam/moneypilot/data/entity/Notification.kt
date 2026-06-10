package prasad.vennam.moneypilot.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val category: String, // Alerts, Sync, Budget, System
    val timestamp: Long,
    val isRead: Boolean = false,
    val url: String? = null,
)
