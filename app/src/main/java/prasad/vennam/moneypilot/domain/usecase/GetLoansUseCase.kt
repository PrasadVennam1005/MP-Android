package prasad.vennam.moneypilot.domain.usecase

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.data.repository.LoanRepository
import javax.inject.Inject

class GetLoansUseCase
    @Inject
    constructor(
        private val repository: LoanRepository,
    ) {
        operator fun invoke(): Flow<List<Loan>> = repository.allLoans
    }
