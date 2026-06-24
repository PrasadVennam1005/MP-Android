package prasad.vennam.moneypilot.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.SavingGoal

@Dao
interface SavingGoalDao {
    @Query("SELECT * FROM saving_goals")
    fun getAllSavingGoals(): Flow<List<SavingGoal>>

    @Query("SELECT * FROM saving_goals")
    suspend fun getAllSavingGoalsSync(): List<SavingGoal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingGoal(savingGoal: SavingGoal)

    @Update
    suspend fun updateSavingGoal(savingGoal: SavingGoal)

    @Delete
    suspend fun deleteSavingGoal(savingGoal: SavingGoal)

    @Query("SELECT * FROM saving_goals WHERE id = :id")
    suspend fun getSavingGoalById(id: Long): SavingGoal?
}
