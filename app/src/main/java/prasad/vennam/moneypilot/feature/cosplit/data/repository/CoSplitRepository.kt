package prasad.vennam.moneypilot.feature.cosplit.data.repository

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitGroup
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitExpense

interface CoSplitRepository {
    fun getGroups(userEmail: String): Flow<List<CoSplitGroup>>
    
    fun getGroup(groupId: String): Flow<CoSplitGroup?>
    
    suspend fun createGroup(
        name: String,
        members: List<String>,
        memberNames: Map<String, String>,
        creatorId: String,
        creatorEmail: String
    ): Result<String>
    
    fun getExpenses(groupId: String): Flow<List<CoSplitExpense>>
    
    suspend fun addExpense(
        groupId: String,
        description: String,
        amount: Double,
        paidBy: String,
        splitType: String,
        splitDetails: Map<String, Double>,
        creatorId: String
    ): Result<Unit>
    
    suspend fun deleteExpense(
        groupId: String,
        expenseId: String
    ): Result<Unit>

    suspend fun settleUp(
        groupId: String,
        payerEmail: String,
        receiverEmail: String,
        amount: Double,
        creatorId: String
    ): Result<Unit>

    suspend fun joinGroup(
        groupId: String,
        userEmail: String
    ): Result<Unit>

    suspend fun deleteGroup(groupId: String): Result<Unit>
    
    suspend fun updateGroupMembers(groupId: String, members: List<String>, memberNames: Map<String, String>): Result<Unit>

    suspend fun getUserProfile(email: String): String?

    suspend fun getUserUpiId(email: String): String?

    suspend fun saveUserUpiId(email: String, upiId: String): Result<Unit>
}
