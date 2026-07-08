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

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE timestamp BETWEEN :startTime AND :endTime AND ABS(amount - :amountMajor) < 0.01 AND (merchant LIKE '%' || :merchant || '%' OR rawMessage LIKE '%' || :merchant || '%')")
    suspend fun countDuplicatePendingTransactions(startTime: Long, endTime: Long, amountMajor: Double, merchant: String): Int
}
