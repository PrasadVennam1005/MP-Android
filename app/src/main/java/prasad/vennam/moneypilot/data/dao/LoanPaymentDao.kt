package prasad.vennam.moneypilot.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.LoanPayment

@Dao
interface LoanPaymentDao {
    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY date DESC")
    fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPayment>>

    @Query("SELECT * FROM loan_payments")
    suspend fun getAllLoanPaymentsSync(): List<LoanPayment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: LoanPayment)

    @Delete
    suspend fun deletePayment(payment: LoanPayment)

    @Query("SELECT SUM(amount) FROM loan_payments WHERE loanId = :loanId")
    suspend fun getTotalPaidForLoan(loanId: Long): Long?
}
