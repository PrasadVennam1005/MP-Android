package prasad.vennam.moneypilot.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.Investment

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investments")
    fun getAllInvestments(): Flow<List<Investment>>

    @Query("SELECT * FROM investments")
    suspend fun getAllInvestmentsSync(): List<Investment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: Investment)

    @Update
    suspend fun updateInvestment(investment: Investment)

    @Delete
    suspend fun deleteInvestment(investment: Investment)

    @Query("SELECT * FROM investments WHERE id = :id")
    suspend fun getInvestmentById(id: Long): Investment?
}
