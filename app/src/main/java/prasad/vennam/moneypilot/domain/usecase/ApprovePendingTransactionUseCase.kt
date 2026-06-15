package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.flow.first
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.PendingTransaction
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import prasad.vennam.moneypilot.util.inPaisa
import javax.inject.Inject

class ApprovePendingTransactionUseCase
    @Inject
    constructor(
        private val repository: MoneyPilotRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(
            pending: PendingTransaction,
            categoryId: Long?,
            note: String = "",
        ) {
            val currentCurrency = userPreferences.currency.first()
            val transaction =
                Transaction(
                    amount = pending.amount.inPaisa,
                    timestamp = pending.timestamp,
                    categoryId = categoryId,
                    note = note.ifEmpty { pending.merchant },
                    type = TransactionType.valueOf(pending.type),
                    paymentMode = pending.bankAccount.ifEmpty { "Bank Alert" },
                    currencyCode = currentCurrency,
                )
            repository.insertTransaction(transaction)
            repository.deletePendingTransaction(pending)
        }
    }
