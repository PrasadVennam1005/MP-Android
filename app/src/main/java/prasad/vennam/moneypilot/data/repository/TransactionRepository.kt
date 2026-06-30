package prasad.vennam.moneypilot.data.repository

import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction
import prasad.vennam.moneypilot.data.MoneyPilotDatabase
import prasad.vennam.moneypilot.data.dao.CategoryDao
import prasad.vennam.moneypilot.data.dao.LoanDao
import prasad.vennam.moneypilot.data.dao.LoanPaymentDao
import prasad.vennam.moneypilot.data.dao.PendingTransactionDao
import prasad.vennam.moneypilot.data.dao.TransactionDao
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.PendingTransaction
import prasad.vennam.moneypilot.data.entity.Transaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
        private val pendingTransactionDao: PendingTransactionDao,
        private val loanPaymentDao: LoanPaymentDao,
        private val loanDao: LoanDao,
        private val database: MoneyPilotDatabase,
    ) {
        // Transactions
        val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

        suspend fun insertTransaction(transaction: Transaction) = transactionDao.insertTransaction(transaction)

        suspend fun updateTransaction(transaction: Transaction) {
            database.withTransaction {
                val oldTransaction = transactionDao.getTransactionById(transaction.id)
                transactionDao.updateTransaction(transaction)

                if (oldTransaction != null && transaction.loanPaymentId != null) {
                    val payment = loanPaymentDao.getPaymentById(transaction.loanPaymentId)
                    payment?.let { pay ->
                        val delta = transaction.amount - oldTransaction.amount
                        
                        // Update the Loan Payment amount and date to match the transaction
                        loanPaymentDao.insertPayment(
                            pay.copy(
                                amount = transaction.amount,
                                date = transaction.timestamp
                            )
                        )

                        if (delta != 0L) {
                            // Update the Loan outstanding amount
                            val loan = loanDao.getLoanById(pay.loanId)
                            loan?.let { l ->
                                val newOutstanding = (l.outstandingAmount - delta).coerceAtLeast(0)
                                loanDao.updateLoan(l.copy(outstandingAmount = newOutstanding))
                            }
                        }
                    }
                }
            }
        }

        suspend fun deleteTransaction(transaction: Transaction) {
            database.withTransaction {
                transactionDao.deleteTransaction(transaction)
                transaction.loanPaymentId?.let { paymentId ->
                    val payment = loanPaymentDao.getPaymentById(paymentId)
                    payment?.let { pay ->
                        loanPaymentDao.deletePayment(pay)
                        val loan = loanDao.getLoanById(pay.loanId)
                        loan?.let { l ->
                            val newOutstanding = l.outstandingAmount + pay.amount
                            
                            // Revert next EMI date by 1 month ONLY if it was NOT an extra payment
                            val revertedNextEmiDate = if (!pay.isExtraPayment) {
                                val cal = java.util.Calendar.getInstance().apply {
                                    timeInMillis = l.nextEmiDate
                                }
                                cal.add(java.util.Calendar.MONTH, -1)
                                cal.timeInMillis
                            } else {
                                l.nextEmiDate
                            }
                            
                            loanDao.updateLoan(
                                l.copy(
                                    outstandingAmount = newOutstanding,
                                    nextEmiDate = revertedNextEmiDate
                                )
                            )
                        }
                    }
                }
            }
        }

        suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getTransactionById(id)

        fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> = transactionDao.getTransactionsByCategory(categoryId)

        // Pending Transactions
        val allPendingTransactions: Flow<List<PendingTransaction>> = pendingTransactionDao.getAllPendingTransactions()

        suspend fun insertPendingTransaction(pending: PendingTransaction) = pendingTransactionDao.insertPendingTransaction(pending)

        suspend fun deletePendingTransaction(pending: PendingTransaction) = pendingTransactionDao.deletePendingTransaction(pending)

        suspend fun clearAllPendingTransactions() = pendingTransactionDao.clearAllPendingTransactions()
    }
