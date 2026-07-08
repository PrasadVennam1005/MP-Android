package prasad.vennam.moneypilot.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.AutopayAlert

@Dao
interface AutopayAlertDao {
    @Query("SELECT * FROM autopay_alerts ORDER BY scheduledDate ASC")
    fun getAllAutopayAlerts(): Flow<List<AutopayAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutopayAlert(alert: AutopayAlert): Long

    @Update
    suspend fun updateAutopayAlert(alert: AutopayAlert)

    @Query("DELETE FROM autopay_alerts WHERE id = :id")
    suspend fun deleteAutopayAlert(id: Long)
}
