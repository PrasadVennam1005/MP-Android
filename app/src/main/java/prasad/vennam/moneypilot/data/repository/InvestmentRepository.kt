package prasad.vennam.moneypilot.data.repository

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.dao.InvestmentDao
import prasad.vennam.moneypilot.data.entity.Investment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvestmentRepository
    @Inject
    constructor(
        private val investmentDao: InvestmentDao,
    ) {
        val allInvestments: Flow<List<Investment>> = investmentDao.getAllInvestments()

        suspend fun insertInvestment(investment: Investment) = investmentDao.insertInvestment(investment)

        suspend fun updateInvestment(investment: Investment) = investmentDao.updateInvestment(investment)

        suspend fun deleteInvestment(investment: Investment) = investmentDao.deleteInvestment(investment)

        suspend fun getInvestmentById(id: Long): Investment? = investmentDao.getInvestmentById(id)
    }
