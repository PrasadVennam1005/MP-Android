package prasad.vennam.moneypilot.feature.cosplit.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitGroup
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitExpense
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoSplitRepositoryImpl @Inject constructor() : CoSplitRepository {
    companion object {
        var delegate: CoSplitRepository? = null
    }

    private val firestore: FirebaseFirestore
        get() {
            ensureFirebaseAuthenticated()
            return FirebaseFirestore.getInstance()
        }

    private fun ensureFirebaseAuthenticated() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.d("CoSplitRepository", "No user authenticated in Firebase Auth. Initiating anonymous sign in.")
            auth.signInAnonymously()
                .addOnSuccessListener {
                    Log.d("CoSplitRepository", "Anonymous sign in successful. UID: ${it.user?.uid}")
                }
                .addOnFailureListener { e ->
                    Log.e("CoSplitRepository", "Anonymous sign in failed", e)
                }
        }
    }

    override fun getGroups(userEmail: String): Flow<List<CoSplitGroup>> =
        delegate?.getGroups(userEmail) ?: callbackFlow {
        val email = userEmail.trim().lowercase()
        Log.d("CoSplitRepository", "Listening to groups for email: $email")
        val query = firestore.collection("cosplit_groups")
            .whereArrayContains("members", email)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("CoSplitRepository", "Error in getGroups snapshot listener", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val group = doc.toObject(CoSplitGroup::class.java)
                    group?.copy(id = doc.id)
                }.sortedByDescending { it.createdAt }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    override fun getGroup(groupId: String): Flow<CoSplitGroup?> =
        delegate?.getGroup(groupId) ?: callbackFlow {
        Log.d("CoSplitRepository", "Listening to group: $groupId")
        val docRef = firestore.collection("cosplit_groups").document(groupId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("CoSplitRepository", "Error in getGroup snapshot listener", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val group = snapshot.toObject(CoSplitGroup::class.java)
                trySend(group?.copy(id = snapshot.id))
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    override suspend fun createGroup(
        name: String,
        members: List<String>,
        memberNames: Map<String, String>,
        creatorId: String,
        creatorEmail: String
    ): Result<String> = delegate?.createGroup(name, members, memberNames, creatorId, creatorEmail) ?: withContext(Dispatchers.IO) {
        runCatching {
            Log.d("CoSplitRepository", "createGroup: name='$name', members=$members, creatorId='$creatorId', creatorEmail='$creatorEmail'")
            val cleanMembers = (members + creatorEmail)
                .map { it.trim().lowercase() }
                .distinct()
                
            val finalMemberNames = memberNames.toMutableMap()
            val cleanCreatorEmail = creatorEmail.trim().lowercase()
            if (!finalMemberNames.containsKey(cleanCreatorEmail)) {
                val creatorProfile = getUserProfile(cleanCreatorEmail)
                finalMemberNames[cleanCreatorEmail] = creatorProfile ?: "Me"
            }

            val groupRef = firestore.collection("cosplit_groups").document()
            val group = CoSplitGroup(
                id = groupRef.id,
                name = name.trim(),
                createdById = creatorId,
                createdAt = System.currentTimeMillis(),
                members = cleanMembers,
                memberNames = finalMemberNames,
                balances = cleanMembers.associateWith { 0.0 }
            )
            groupRef.set(group).await()
            groupRef.id
        }.onFailure { e ->
            Log.e("CoSplitRepository", "createGroup failed: ${e.message}", e)
        }
    }

    override fun getExpenses(groupId: String): Flow<List<CoSplitExpense>> =
        delegate?.getExpenses(groupId) ?: callbackFlow {
        val query = firestore.collection("cosplit_groups")
            .document(groupId)
            .collection("expenses")

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val expense = doc.toObject(CoSplitExpense::class.java)
                    expense?.copy(id = doc.id)
                }.sortedByDescending { it.date }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    override suspend fun addExpense(
        groupId: String,
        description: String,
        amount: Double,
        paidBy: String,
        splitType: String,
        splitDetails: Map<String, Double>,
        creatorId: String
    ): Result<Unit> = delegate?.addExpense(groupId, description, amount, paidBy, splitType, splitDetails, creatorId) ?: withContext(Dispatchers.IO) {
        runCatching {
            val groupRef = firestore.collection("cosplit_groups").document(groupId)
            val expenseRef = groupRef.collection("expenses").document()

            val cleanPayer = paidBy.trim().lowercase()
            val cleanSplitDetails = splitDetails.mapKeys { it.key.trim().lowercase() }
            val splitAmounts = calculateSplitAmounts(amount, splitType, cleanSplitDetails)

            firestore.runTransaction { transaction ->
                val groupDoc = transaction.get(groupRef)
                val group = groupDoc.toObject(CoSplitGroup::class.java) ?: throw Exception("Group not found")

                // Copy and update balances
                val newBalances = group.balances.toMutableMap()
                
                // Credit the payer
                newBalances[cleanPayer] = (newBalances[cleanPayer] ?: 0.0) + amount

                // Debit members in the split
                splitAmounts.forEach { (email, share) ->
                    newBalances[email] = (newBalances[email] ?: 0.0) - share
                }

                // Write expense
                val expense = CoSplitExpense(
                    id = expenseRef.id,
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

                transaction.set(groupRef, group.copy(balances = newBalances))
                transaction.set(expenseRef, expense)
            }.await()
            Unit
        }
    }

    override suspend fun deleteExpense(groupId: String, expenseId: String): Result<Unit> =
        delegate?.deleteExpense(groupId, expenseId) ?: withContext(Dispatchers.IO) {
        runCatching {
            val groupRef = firestore.collection("cosplit_groups").document(groupId)
            val expenseRef = groupRef.collection("expenses").document(expenseId)

            firestore.runTransaction { transaction ->
                val groupDoc = transaction.get(groupRef)
                val group = groupDoc.toObject(CoSplitGroup::class.java) ?: throw Exception("Group not found")

                val expenseDoc = transaction.get(expenseRef)
                val expense = expenseDoc.toObject(CoSplitExpense::class.java) ?: throw Exception("Expense not found")

                val splitAmounts = calculateSplitAmounts(expense.amount, expense.splitType, expense.splitDetails)
                val cleanPayer = expense.paidBy.trim().lowercase()

                val newBalances = group.balances.toMutableMap()

                // Debit the payer (revert credit)
                newBalances[cleanPayer] = (newBalances[cleanPayer] ?: 0.0) - expense.amount

                // Credit members in the split (revert debit)
                splitAmounts.forEach { (email, share) ->
                    newBalances[email] = (newBalances[email] ?: 0.0) + share
                }

                transaction.set(groupRef, group.copy(balances = newBalances))
                transaction.delete(expenseRef)
            }.await()
            Unit
        }
    }

    override suspend fun settleUp(
        groupId: String,
        payerEmail: String,
        receiverEmail: String,
        amount: Double,
        creatorId: String
    ): Result<Unit> = delegate?.settleUp(groupId, payerEmail, receiverEmail, amount, creatorId) ?: withContext(Dispatchers.IO) {
        runCatching {
            val groupRef = firestore.collection("cosplit_groups").document(groupId)
            val expenseRef = groupRef.collection("expenses").document()

            val cleanPayer = payerEmail.trim().lowercase()
            val cleanReceiver = receiverEmail.trim().lowercase()

            firestore.runTransaction { transaction ->
                val groupDoc = transaction.get(groupRef)
                val group = groupDoc.toObject(CoSplitGroup::class.java) ?: throw Exception("Group not found")

                val newBalances = group.balances.toMutableMap()

                // Payer balances changes: since they paid off debt, their balance goes up by amount
                newBalances[cleanPayer] = (newBalances[cleanPayer] ?: 0.0) + amount
                // Receiver balances changes: since they received payment, their balance goes down by amount
                newBalances[cleanReceiver] = (newBalances[cleanReceiver] ?: 0.0) - amount

                val settlementExpense = CoSplitExpense(
                    id = expenseRef.id,
                    groupId = groupId,
                    description = "Settlement: $payerEmail to $receiverEmail",
                    amount = amount,
                    paidBy = cleanPayer,
                    splitType = "EXACT",
                    splitDetails = mapOf(cleanReceiver to amount),
                    createdById = creatorId,
                    createdAt = System.currentTimeMillis(),
                    date = System.currentTimeMillis()
                )

                transaction.set(groupRef, group.copy(balances = newBalances))
                transaction.set(expenseRef, settlementExpense)
            }.await()
            Unit
        }
    }

    override suspend fun joinGroup(groupId: String, userEmail: String): Result<Unit> =
        delegate?.joinGroup(groupId, userEmail) ?: withContext(Dispatchers.IO) {
        runCatching {
            val groupRef = firestore.collection("cosplit_groups").document(groupId)
            val cleanEmail = userEmail.trim().lowercase()
            firestore.runTransaction { transaction ->
                val groupDoc = transaction.get(groupRef)
                val group = groupDoc.toObject(CoSplitGroup::class.java) ?: throw Exception("Group not found")

                if (cleanEmail in group.members) {
                    return@runTransaction
                }

                val newMembers = group.members + cleanEmail
                val newBalances = group.balances.toMutableMap()
                newBalances[cleanEmail] = 0.0

                transaction.update(groupRef, mapOf(
                    "members" to newMembers,
                    "balances" to newBalances
                ))
            }.await()
            Unit
        }
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> =
        delegate?.deleteGroup(groupId) ?: withContext(Dispatchers.IO) {
        runCatching {
            val groupRef = firestore.collection("cosplit_groups").document(groupId)
            val expenses = groupRef.collection("expenses").get().await()
            firestore.runBatch { batch ->
                for (expense in expenses.documents) {
                    batch.delete(expense.reference)
                }
                batch.delete(groupRef)
            }.await()
            Unit
        }
    }

    override suspend fun updateGroupMembers(groupId: String, members: List<String>, memberNames: Map<String, String>): Result<Unit> =
        delegate?.updateGroupMembers(groupId, members, memberNames) ?: withContext(Dispatchers.IO) {
        runCatching {
            val groupRef = firestore.collection("cosplit_groups").document(groupId)
            firestore.runTransaction { transaction ->
                val groupDoc = transaction.get(groupRef)
                val group = groupDoc.toObject(CoSplitGroup::class.java) ?: throw Exception("Group not found")
                
                val cleanMembers = members.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
                val newBalances = group.balances.toMutableMap()
                
                cleanMembers.forEach { member ->
                    if (!newBalances.containsKey(member)) {
                        newBalances[member] = 0.0
                    }
                }
                
                val newMemberNames = group.memberNames.toMutableMap()
                newMemberNames.putAll(memberNames)
                
                transaction.update(groupRef, mapOf(
                    "members" to cleanMembers,
                    "memberNames" to newMemberNames,
                    "balances" to newBalances
                ))
            }.await()
            Unit
        }
    }

    override suspend fun getUserProfile(email: String): String? = withContext(Dispatchers.IO) {
        val d = delegate
        if (d != null) return@withContext d.getUserProfile(email)
        val cleanEmail = email.trim().lowercase()
        try {
            val doc = firestore.collection("users").document(cleanEmail).get().await()
            if (doc.exists()) {
                doc.getString("name")
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getUserUpiId(email: String): String? = withContext(Dispatchers.IO) {
        val d = delegate
        if (d != null) return@withContext d.getUserUpiId(email)
        val cleanEmail = email.trim().lowercase()
        try {
            val doc = firestore.collection("users").document(cleanEmail).get().await()
            if (doc.exists()) {
                doc.getString("upiId")
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveUserUpiId(email: String, upiId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val d = delegate
        if (d != null) return@withContext d.saveUserUpiId(email, upiId)
        runCatching {
            val cleanEmail = email.trim().lowercase()
            val docRef = firestore.collection("users").document(cleanEmail)
            val doc = docRef.get().await()
            if (doc.exists()) {
                docRef.update("upiId", upiId.trim()).await()
            } else {
                val profileMap = hashMapOf(
                    "email" to cleanEmail,
                    "upiId" to upiId.trim()
                )
                docRef.set(profileMap).await()
            }
            Unit
        }
    }

    private fun calculateSplitAmounts(
        amount: Double,
        splitType: String,
        splitDetails: Map<String, Double>
    ): Map<String, Double> {
        return when (splitType) {
            "EQUAL" -> {
                val count = splitDetails.size.coerceAtLeast(1)
                val share = amount / count
                splitDetails.keys.associateWith { share }
            }
            "EXACT" -> {
                splitDetails
            }
            "PERCENT" -> {
                splitDetails.mapValues { (_, pct) -> (pct / 100.0) * amount }
            }
            else -> {
                val count = splitDetails.size.coerceAtLeast(1)
                val share = amount / count
                splitDetails.keys.associateWith { share }
            }
        }
    }
}
