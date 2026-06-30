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

        suspend fun deleteLoan(
            loan: Loan,
            deleteTransactions: Boolean = true,
        ) {
            database.withTransaction {
                if (deleteTransactions) {
                    transactionDao.deleteTransactionsByLoanId(loan.id)
                }
                loanDao.deleteLoan(loan)
            }
        }

        suspend fun getLoanById(id: Long) = loanDao.getLoanById(id)

        fun getPaymentsForLoan(loanId: Long) = loanPaymentDao.getPaymentsForLoan(loanId)

        suspend fun insertLoanPayment(payment: LoanPayment) {
            database.withTransaction {
                // Fetch the loan first to check outstanding amount
                val loan = loanDao.getLoanById(payment.loanId)
                loan?.let { currentLoan ->
                    // Guard: Cap payment amount at current outstanding balance
                    val cappedAmount = if (payment.amount > currentLoan.outstandingAmount) {
                        currentLoan.outstandingAmount
                    } else {
                        payment.amount
                    }

                    if (cappedAmount <= 0) return@withTransaction

                    val finalPayment = payment.copy(amount = cappedAmount)
                    val paymentId = loanPaymentDao.insertPayment(finalPayment)

                    // Update outstanding amount
                    val newOutstanding = (currentLoan.outstandingAmount - cappedAmount).coerceAtLeast(0)

                    // Increment next EMI date by 1 month ONLY if it's not an extra payment
                    val newNextEmiDate = if (!finalPayment.isExtraPayment) {
                        val cal = java.util.Calendar.getInstance().apply {
                            timeInMillis = currentLoan.nextEmiDate
                        }
                        cal.add(java.util.Calendar.MONTH, 1)
                        cal.timeInMillis
                    } else {
                        currentLoan.nextEmiDate
                    }

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

                    // Check for currency mismatch and potentially log a warning or handle conversion
                    // For now, we use the payment's raw amount but ensure it uses the Loan's currency code
                    // to keep the principal consistent.

                    val transaction =
                        Transaction(
                            amount = cappedAmount,
                            timestamp = finalPayment.date,
                            categoryId = categoryId,
                            note = if (finalPayment.note.isNotBlank()) finalPayment.note else "EMI Payment: ${currentLoan.name}",
                            type = TransactionType.EXPENSE,
                            paymentMode = finalPayment.paymentMode,
                            currencyCode = currentLoan.currencyCode,
                            loanPaymentId = paymentId,
                        )
                    transactionDao.insertTransaction(transaction)

                    loanDao.updateLoan(
                        currentLoan.copy(
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
