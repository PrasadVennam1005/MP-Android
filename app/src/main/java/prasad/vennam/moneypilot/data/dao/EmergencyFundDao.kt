package prasad.vennam.moneypilot.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.EmergencyFund

@Dao
interface EmergencyFundDao {
    @Query("SELECT * FROM emergency_fund WHERE id = 1 LIMIT 1")
    fun getEmergencyFund(): Flow<EmergencyFund?>

    @Query("SELECT * FROM emergency_fund WHERE id = 1 LIMIT 1")
    suspend fun getEmergencyFundSync(): EmergencyFund?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyFund(emergencyFund: EmergencyFund)

    @Query("DELETE FROM emergency_fund")
    suspend fun deleteEmergencyFund()
}
