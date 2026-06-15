package prasad.vennam.moneypilot.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.PendingTransaction

@Dao
interface PendingTransactionDao {
    @Query("SELECT * FROM pending_transactions ORDER BY timestamp DESC")
    fun getAllPendingTransactions(): Flow<List<PendingTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingTransaction(pending: PendingTransaction): Long

    @Delete
    suspend fun deletePendingTransaction(pending: PendingTransaction)

    @Query("DELETE FROM pending_transactions")
    suspend fun clearAllPendingTransactions()
}
