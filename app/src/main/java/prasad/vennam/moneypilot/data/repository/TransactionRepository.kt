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

        suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)

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
                            loanDao.updateLoan(l.copy(outstandingAmount = newOutstanding))
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
