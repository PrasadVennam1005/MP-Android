package prasad.vennam.moneypilot

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitGroup
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitExpense
import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepository
import java.util.UUID

class FakeCoSplitRepository : CoSplitRepository {
    private val groupsFlow = MutableStateFlow<List<CoSplitGroup>>(emptyList())
    private val expensesMap = mutableMapOf<String, MutableStateFlow<List<CoSplitExpense>>>()

    override fun getGroups(userEmail: String): Flow<List<CoSplitGroup>> {
        val cleanEmail = userEmail.trim().lowercase()
        return groupsFlow.map { list ->
            list.filter { cleanEmail in it.members }
        }
    }

    override fun getGroup(groupId: String): Flow<CoSplitGroup?> {
        return groupsFlow.map { list ->
            list.find { it.id == groupId }
        }
    }

    override suspend fun createGroup(
        name: String,
        members: List<String>,
        memberNames: Map<String, String>,
        creatorId: String,
        creatorEmail: String
    ): Result<String> {
        val cleanCreatorEmail = creatorEmail.trim().lowercase()
        val cleanMembers = (members + cleanCreatorEmail)
            .map { it.trim().lowercase() }
            .distinct()

        val finalMemberNames = memberNames.toMutableMap()
        if (!finalMemberNames.containsKey(cleanCreatorEmail)) {
            finalMemberNames[cleanCreatorEmail] = "Test Pilot"
        }

        val id = UUID.randomUUID().toString()
        val newGroup = CoSplitGroup(
            id = id,
            name = name.trim(),
            createdById = creatorId,
            createdAt = System.currentTimeMillis(),
            members = cleanMembers,
            memberNames = finalMemberNames,
            balances = cleanMembers.associateWith { 0.0 }
        )

        groupsFlow.value = groupsFlow.value + newGroup
        expensesMap[id] = MutableStateFlow(emptyList())
        return Result.success(id)
    }

    override fun getExpenses(groupId: String): Flow<List<CoSplitExpense>> {
        return expensesMap.getOrPut(groupId) { MutableStateFlow(emptyList()) }
    }

    override suspend fun addExpense(
        groupId: String,
        description: String,
        amount: Double,
        paidBy: String,
        splitType: String,
        splitDetails: Map<String, Double>,
        creatorId: String
    ): Result<Unit> {
        val cleanPayer = paidBy.trim().lowercase()
        val cleanSplitDetails = splitDetails.mapKeys { it.key.trim().lowercase() }
        
        // Calculate split shares
        val splitAmounts = calculateSplitAmounts(amount, splitType, cleanSplitDetails)

        val currentGroups = groupsFlow.value.toMutableList()
        val index = currentGroups.indexOfFirst { it.id == groupId }
        if (index == -1) return Result.failure(Exception("Group not found"))

        val group = currentGroups[index]
        val newBalances = group.balances.toMutableMap()
        
        // Credit the payer
        newBalances[cleanPayer] = (newBalances[cleanPayer] ?: 0.0) + amount

        // Debit members in the split
        splitAmounts.forEach { (email, share) ->
            newBalances[email] = (newBalances[email] ?: 0.0) - share
        }

        currentGroups[index] = group.copy(balances = newBalances)
        groupsFlow.value = currentGroups

        val expensesFlow = expensesMap.getOrPut(groupId) { MutableStateFlow(emptyList()) }
        val id = UUID.randomUUID().toString()
        val newExpense = CoSplitExpense(
            id = id,
            groupId = groupId,
            description = description.trim(),
            amount = amount,
            paidBy = cleanPayer,
            splitType = splitType,
            splitDetails = cleanSplitDetails,
            createdById = creatorId,
            createdAt = System.currentTimeMillis(),
            date = System.currentTimeMillis()
        )
        expensesFlow.value = expensesFlow.value + newExpense

        return Result.success(Unit)
    }

    override suspend fun deleteExpense(groupId: String, expenseId: String): Result<Unit> {
        val expensesFlow = expensesMap[groupId] ?: return Result.failure(Exception("Expenses not found"))
        val currentExpenses = expensesFlow.value.toMutableList()
        val expenseIndex = currentExpenses.indexOfFirst { it.id == expenseId }
        if (expenseIndex == -1) return Result.failure(Exception("Expense not found"))

        val expense = currentExpenses[expenseIndex]
        val splitAmounts = calculateSplitAmounts(expense.amount, expense.splitType, expense.splitDetails)
        val cleanPayer = expense.paidBy.trim().lowercase()

        val currentGroups = groupsFlow.value.toMutableList()
        val groupIndex = currentGroups.indexOfFirst { it.id == groupId }
        if (groupIndex == -1) return Result.failure(Exception("Group not found"))

        val group = currentGroups[groupIndex]
        val newBalances = group.balances.toMutableMap()

        // Debit the payer (revert credit)
        newBalances[cleanPayer] = (newBalances[cleanPayer] ?: 0.0) - expense.amount

        // Credit members in the split (revert debit)
        splitAmounts.forEach { (email, share) ->
            newBalances[email] = (newBalances[email] ?: 0.0) + share
        }

        currentGroups[groupIndex] = group.copy(balances = newBalances)
        groupsFlow.value = currentGroups

        currentExpenses.removeAt(expenseIndex)
        expensesFlow.value = currentExpenses

        return Result.success(Unit)
    }

    override suspend fun settleUp(
        groupId: String,
        payerEmail: String,
        receiverEmail: String,
        amount: Double,
        creatorId: String
    ): Result<Unit> {
        val cleanPayer = payerEmail.trim().lowercase()
        val cleanReceiver = receiverEmail.trim().lowercase()

        val currentGroups = groupsFlow.value.toMutableList()
        val index = currentGroups.indexOfFirst { it.id == groupId }
        if (index == -1) return Result.failure(Exception("Group not found"))

        val group = currentGroups[index]
        val newBalances = group.balances.toMutableMap()

        // Credit the payer (reduces debt or increases credit)
        newBalances[cleanPayer] = (newBalances[cleanPayer] ?: 0.0) + amount
        // Debit the receiver (reduces their credit or increases debt)
        newBalances[cleanReceiver] = (newBalances[cleanReceiver] ?: 0.0) - amount

        currentGroups[index] = group.copy(balances = newBalances)
        groupsFlow.value = currentGroups

        return Result.success(Unit)
    }

    override suspend fun joinGroup(groupId: String, userEmail: String): Result<Unit> {
        val currentGroups = groupsFlow.value.toMutableList()
        val index = currentGroups.indexOfFirst { it.id == groupId }
        if (index == -1) return Result.failure(Exception("Group not found"))

        val group = currentGroups[index]
        val cleanEmail = userEmail.trim().lowercase()
        if (cleanEmail !in group.members) {
            val newMembers = group.members + cleanEmail
            val newBalances = group.balances.toMutableMap()
            newBalances[cleanEmail] = 0.0
            currentGroups[index] = group.copy(members = newMembers, balances = newBalances)
            groupsFlow.value = currentGroups
        }
        return Result.success(Unit)
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        groupsFlow.value = groupsFlow.value.filterNot { it.id == groupId }
        expensesMap.remove(groupId)
        return Result.success(Unit)
    }

    override suspend fun updateGroupMembers(
        groupId: String,
        members: List<String>,
        memberNames: Map<String, String>
    ): Result<Unit> {
        val currentGroups = groupsFlow.value.toMutableList()
        val index = currentGroups.indexOfFirst { it.id == groupId }
        if (index == -1) return Result.failure(Exception("Group not found"))

        val group = currentGroups[index]
        val cleanMembers = members.map { it.trim().lowercase() }.distinct()
        val newBalances = group.balances.filterKeys { it in cleanMembers }.toMutableMap()
        cleanMembers.forEach { email ->
            if (!newBalances.containsKey(email)) {
                newBalances[email] = 0.0
            }
        }
        currentGroups[index] = group.copy(
            members = cleanMembers,
            memberNames = memberNames,
            balances = newBalances
        )
        groupsFlow.value = currentGroups
        return Result.success(Unit)
    }

    override suspend fun getUserProfile(email: String): String? {
        return null
    }

    override suspend fun getUserUpiId(email: String): String? {
        return "testpilot@upi"
    }

    override suspend fun saveUserUpiId(email: String, upiId: String): Result<Unit> {
        return Result.success(Unit)
    }

    private fun calculateSplitAmounts(amount: Double, splitType: String, splitDetails: Map<String, Double>): Map<String, Double> {
        if (splitDetails.isEmpty()) return emptyMap()
        return when (splitType) {
            "EQUAL" -> {
                val share = amount / splitDetails.size
                splitDetails.keys.associateWith { share }
            }
            "EXACT" -> splitDetails
            "PERCENT" -> {
                splitDetails.mapValues { (_, percent) -> (percent / 100.0) * amount }
            }
            else -> emptyMap()
        }
    }
}
