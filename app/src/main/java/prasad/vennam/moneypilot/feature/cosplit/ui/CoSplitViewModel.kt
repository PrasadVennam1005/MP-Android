package prasad.vennam.moneypilot.feature.cosplit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitGroup
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitExpense
import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepository
import prasad.vennam.moneypilot.feature.cosplit.util.AiParsedSplit
import prasad.vennam.moneypilot.feature.cosplit.util.AiReceiptLineItem
import prasad.vennam.moneypilot.feature.cosplit.util.CoSplitAiHelper
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class CoSplitViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val repository: CoSplitRepository,
    private val aiHelper: CoSplitAiHelper
) : ViewModel() {

    val userData = userPreferences.userData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val userEmail = userData.map { it?.email ?: "" }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val userId = userData.map { it?.name ?: "" }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    private val _selectedGroup = MutableStateFlow<CoSplitGroup?>(null)
    val selectedGroup: StateFlow<CoSplitGroup?> = _selectedGroup.asStateFlow()

    val groups: StateFlow<List<CoSplitGroup>> = userEmail
        .flatMapLatest { email ->
            if (email.isBlank() || email == "guest@moneypilot.app") {
                flowOf(emptyList())
            } else {
                repository.getGroups(email)
                    .catch { e ->
                        _error.emit("Database error: ${e.message}")
                        emit(emptyList())
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val expenses: StateFlow<List<CoSplitExpense>> = _selectedGroup
        .flatMapLatest { group ->
            if (group == null) {
                flowOf(emptyList())
            } else {
                repository.getExpenses(group.id)
                    .catch { e ->
                        _error.emit("Database error: ${e.message}")
                        emit(emptyList())
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val simplifiedSettlements: StateFlow<List<String>> = _selectedGroup
        .map { group ->
            if (group == null) emptyList() else generateSimplifiedSettlements(group.balances, group.memberNames)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _aiSettlementExplanation = MutableStateFlow<String>("")
    val aiSettlementExplanation: StateFlow<String> = _aiSettlementExplanation.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    fun selectGroup(group: CoSplitGroup?) {
        _selectedGroup.value = group
        _aiSettlementExplanation.value = ""
    }

    fun selectGroupById(groupId: String) {
        val group = groups.value.find { it.id == groupId }
        if (group != null) {
            _selectedGroup.value = group
        } else {
            viewModelScope.launch {
                groups.firstOrNull { list ->
                    val found = list.find { it.id == groupId }
                    if (found != null) {
                        _selectedGroup.value = found
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    fun createGroup(name: String, members: List<String>, memberNames: Map<String, String>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val email = userEmail.value
            val id = userId.value
            if (email.isBlank() || email == "guest@moneypilot.app") {
                _error.emit("Guests cannot manage groups")
                onResult(false)
                return@launch
            }
            val result = repository.createGroup(name, members, memberNames, id, email)
            if (result.isFailure) {
                _error.emit(result.exceptionOrNull()?.message ?: "Failed to create group")
            }
            onResult(result.isSuccess)
        }
    }

    fun addExpense(
        description: String,
        amount: Double,
        paidBy: String,
        splitType: String,
        splitDetails: Map<String, Double>,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val group = _selectedGroup.value
            val id = userId.value
            if (group == null) {
                _error.emit("No group selected")
                onResult(false)
                return@launch
            }
            val result = repository.addExpense(
                groupId = group.id,
                description = description,
                amount = amount,
                paidBy = paidBy,
                splitType = splitType,
                splitDetails = splitDetails,
                creatorId = id
            )
            if (result.isFailure) {
                _error.emit(result.exceptionOrNull()?.message ?: "Failed to add expense")
            }
            onResult(result.isSuccess)
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            val group = _selectedGroup.value ?: return@launch
            val result = repository.deleteExpense(group.id, expenseId)
            if (result.isFailure) {
                _error.emit(result.exceptionOrNull()?.message ?: "Failed to delete expense")
            }
        }
    }

    fun settleUp(payerEmail: String, receiverEmail: String, amount: Double, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val group = _selectedGroup.value ?: return@launch
            val id = userId.value
            val result = repository.settleUp(group.id, payerEmail, receiverEmail, amount, id)
            if (result.isFailure) {
                _error.emit(result.exceptionOrNull()?.message ?: "Failed to settle up")
            }
            onResult(result.isSuccess)
        }
    }

    fun parseSplitCommand(commandText: String, onParsed: (AiParsedSplit?) -> Unit) {
        val group = _selectedGroup.value ?: return
        val currentEmail = userEmail.value
        viewModelScope.launch {
            _isAiLoading.value = true
            val parsed = aiHelper.parseSplitCommand(commandText, group.members, currentEmail)
            _isAiLoading.value = false
            onParsed(parsed)
        }
    }

    fun parseReceiptToLineItems(ocrText: String, onParsed: (List<AiReceiptLineItem>?) -> Unit) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val parsed = aiHelper.parseReceiptToLineItems(ocrText)
            _isAiLoading.value = false
            onParsed(parsed)
        }
    }

    fun explainSimplifiedSettlements() {
        viewModelScope.launch {
            _isAiLoading.value = true
            val explanation = aiHelper.explainSettlements(simplifiedSettlements.value)
            _aiSettlementExplanation.value = explanation
            _isAiLoading.value = false
        }
    }

    fun joinGroup(groupId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val email = userEmail.value
            if (email.isBlank() || email == "guest@moneypilot.app") {
                _error.emit("Log in to join groups")
                onResult(false)
                return@launch
            }
            val result = repository.joinGroup(groupId, email)
            if (result.isFailure) {
                _error.emit(result.exceptionOrNull()?.message ?: "Failed to join group")
            }
            onResult(result.isSuccess)
        }
    }

    fun deleteGroup(groupId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteGroup(groupId)
            if (result.isFailure) {
                _error.emit(result.exceptionOrNull()?.message ?: "Failed to delete group")
            } else {
                if (_selectedGroup.value?.id == groupId) {
                    _selectedGroup.value = null
                }
            }
            onResult(result.isSuccess)
        }
    }

    fun updateGroupMembers(groupId: String, members: List<String>, memberNames: Map<String, String>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.updateGroupMembers(groupId, members, memberNames)
            if (result.isFailure) {
                _error.emit(result.exceptionOrNull()?.message ?: "Failed to update members")
            }
            onResult(result.isSuccess)
        }
    }

    suspend fun getUserProfile(email: String): String? {
        return repository.getUserProfile(email)
    }

    fun generateSimplifiedSettlements(balances: Map<String, Double>, memberNames: Map<String, String> = emptyMap()): List<String> {
        val list = mutableListOf<String>()
        // Filter out members with essentially zero balance
        val members = balances.filterValues { kotlin.math.abs(it) > 0.05 }.toMutableMap()

        while (members.isNotEmpty()) {
            val debtor = members.minByOrNull { it.value } ?: break // largest negative owes most
            val creditor = members.maxByOrNull { it.value } ?: break // largest positive owed most

            if (kotlin.math.abs(debtor.value) < 0.05 || kotlin.math.abs(creditor.value) < 0.05) break

            val amountToSettle = kotlin.math.min(kotlin.math.abs(debtor.value), creditor.value)

            val debtorName = memberNames[debtor.key] ?: debtor.key
            val creditorName = memberNames[creditor.key] ?: creditor.key

            list.add("$debtorName pays $creditorName ₹${String.format("%.2f", amountToSettle)}")

            val nextDebtorVal = debtor.value + amountToSettle
            val nextCreditorVal = creditor.value - amountToSettle

            if (kotlin.math.abs(nextDebtorVal) < 0.05) {
                members.remove(debtor.key)
            } else {
                members[debtor.key] = nextDebtorVal
            }

            if (kotlin.math.abs(nextCreditorVal) < 0.05) {
                members.remove(creditor.key)
            } else {
                members[creditor.key] = nextCreditorVal
            }
        }
        return list
    }
}
