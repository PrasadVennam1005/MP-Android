package prasad.vennam.moneypilot.domain.usecase

import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.repository.InvestmentRepository
import javax.inject.Inject

class SaveInvestmentUseCase
    @Inject
    constructor(
        private val repository: InvestmentRepository,
        private val userPreferences: UserPreferences,
    ) {
        suspend operator fun invoke(investment: Investment) {
            userPreferences.setSynced(false)
            val investmentToSave = investment.copy(lastUpdated = System.currentTimeMillis())
            if (investment.id == 0L || repository.getInvestmentById(investment.id) == null) {
                repository.insertInvestment(investmentToSave)
            } else {
                repository.updateInvestment(investmentToSave)
            }
        }
    }
