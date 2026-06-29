package prasad.vennam.moneypilot.data.repository

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.data.dao.EmergencyFundDao
import prasad.vennam.moneypilot.data.dao.SavingGoalDao
import prasad.vennam.moneypilot.data.entity.EmergencyFund
import prasad.vennam.moneypilot.data.entity.SavingGoal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository
    @Inject
    constructor(
        private val emergencyFundDao: EmergencyFundDao,
        private val savingGoalDao: SavingGoalDao,
    ) {
        // Emergency Fund
        val emergencyFund: Flow<EmergencyFund?> = emergencyFundDao.getEmergencyFund()

        suspend fun getEmergencyFundSync(): EmergencyFund? = emergencyFundDao.getEmergencyFundSync()

        suspend fun insertEmergencyFund(emergencyFund: EmergencyFund) = emergencyFundDao.insertEmergencyFund(emergencyFund)

        suspend fun deleteEmergencyFund() = emergencyFundDao.deleteEmergencyFund()

        // Saving Goals
        val allSavingGoals: Flow<List<SavingGoal>> = savingGoalDao.getAllSavingGoals()

        suspend fun insertSavingGoal(savingGoal: SavingGoal) = savingGoalDao.insertSavingGoal(savingGoal)

        suspend fun updateSavingGoal(savingGoal: SavingGoal) = savingGoalDao.updateSavingGoal(savingGoal)

        suspend fun deleteSavingGoal(savingGoal: SavingGoal) = savingGoalDao.deleteSavingGoal(savingGoal)

        suspend fun getSavingGoalById(id: Long): SavingGoal? = savingGoalDao.getSavingGoalById(id)
    }
