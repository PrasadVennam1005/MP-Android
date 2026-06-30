package prasad.vennam.moneypilot.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.MoneyPilotDatabase
import prasad.vennam.moneypilot.data.dao.CategoryDao
import prasad.vennam.moneypilot.data.dao.LoanDao
import prasad.vennam.moneypilot.data.dao.LoanPaymentDao
import prasad.vennam.moneypilot.data.dao.TransactionDao
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.data.entity.LoanPayment
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepository
    @Inject
    constructor(
        private val loanDao: LoanDao,
        private val loanPaymentDao: LoanPaymentDao,
        private val transactionDao: TransactionDao,
        private val categoryDao: CategoryDao,
        private val database: MoneyPilotDatabase,
    ) {
        val allLoans: Flow<List<Loan>> = loanDao.getAllLoans()

        suspend fun insertLoan(loan: Loan) = loanDao.insertLoan(loan)

        suspend fun updateLoan(loan: Loan) = loanDao.updateLoan(loan)

        suspend fun deleteLoan(loan: Loan) = loanDao.deleteLoan(loan)

        suspend fun getLoanById(id: Long) = loanDao.getLoanById(id)

        fun getPaymentsForLoan(loanId: Long) = loanPaymentDao.getPaymentsForLoan(loanId)

        suspend fun insertLoanPayment(payment: LoanPayment) {
            database.withTransaction {
                val paymentId = loanPaymentDao.insertPayment(payment)
                // Update outstanding amount
                val loan = loanDao.getLoanById(payment.loanId)
                loan?.let {
                    val newOutstanding = (it.outstandingAmount - payment.amount).coerceAtLeast(0)

                    // Increment next EMI date by 1 month
                    val cal =
                        java.util.Calendar.getInstance().apply {
                            timeInMillis = it.nextEmiDate
                        }
                    cal.add(java.util.Calendar.MONTH, 1)
                    val newNextEmiDate = cal.timeInMillis

                    // Record matching expense transaction
                    val categories = categoryDao.getAllCategoriesSync()
                    var categoryId =
                        categories
                            .find { cat ->
                                cat.name.equals("Bills", ignoreCase = true) ||
                                    cat.name.equals("Bills/EMI", ignoreCase = true) ||
                                    cat.name.equals("Loan", ignoreCase = true)
                            }?.id
                    if (categoryId == null && categories.isNotEmpty()) {
                        categoryId = categories.firstOrNull { cat -> cat.isExpense }?.id
                    }

                    val transaction =
                        Transaction(
                            amount = payment.amount,
                            timestamp = payment.date,
                            categoryId = categoryId,
                            note = if (payment.note.isNotBlank()) payment.note else "EMI Payment: ${it.name}",
                            type = TransactionType.EXPENSE,
                            paymentMode = payment.paymentMode,
                            currencyCode = it.currencyCode,
                            loanPaymentId = paymentId,
                        )
                    transactionDao.insertTransaction(transaction)

                    loanDao.updateLoan(
                        it.copy(
                            outstandingAmount = newOutstanding,
                            nextEmiDate = newNextEmiDate,
                        ),
                    )
                }
            }
        }

        suspend fun deleteLoanPayment(payment: LoanPayment) {
            database.withTransaction {
                loanPaymentDao.deletePayment(payment)
                // Delete associated transaction if it exists
                val transaction = transactionDao.getTransactionByLoanPaymentId(payment.id)
                transaction?.let {
                    transactionDao.deleteTransaction(it)
                }
                // Update outstanding amount
                val loan = loanDao.getLoanById(payment.loanId)
                loan?.let {
                    val newOutstanding = it.outstandingAmount + payment.amount
                    loanDao.updateLoan(it.copy(outstandingAmount = newOutstanding))
                }
            }
        }
    }
