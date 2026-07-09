package prasad.vennam.moneypilot.feature.cosplit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitGroup
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitExpense
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitSettlementRoute
import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepository
import prasad.vennam.moneypilot.feature.cosplit.util.AiParsedSplit
import prasad.vennam.moneypilot.feature.cosplit.util.AiReceiptLineItem
import prasad.vennam.moneypilot.feature.cosplit.util.CoSplitAiHelper
import prasad.vennam.moneypilot.domain.usecase.*
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class CoSplitViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val getGroupsUseCase: GetCoSplitGroupsUseCase,
    private val getGroupUseCase: GetCoSplitGroupUseCase,
    private val createCoSplitGroupUseCase: CreateCoSplitGroupUseCase,
    private val joinCoSplitGroupUseCase: JoinCoSplitGroupUseCase,
    private val deleteCoSplitGroupUseCase: DeleteCoSplitGroupUseCase,
    private val updateCoSplitGroupMembersUseCase: UpdateCoSplitGroupMembersUseCase,
    private val getExpensesUseCase: GetCoSplitExpensesUseCase,
    private val addExpenseUseCase: AddCoSplitExpenseUseCase,
    private val deleteExpenseUseCase: DeleteCoSplitExpenseUseCase,
    private val settleUpUseCase: SettleCoSplitUpUseCase,
    private val getUserUpiIdUseCase: GetCoSplitUserUpiIdUseCase,
    private val saveUserUpiIdUseCase: SaveCoSplitUserUpiIdUseCase,
    private val getUserProfileUseCase: GetCoSplitUserProfileUseCase,
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

    val isPremium: StateFlow<Boolean> = userPreferences.isPremium.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    val groups: StateFlow<List<CoSplitGroup>> = userEmail
        .flatMapLatest { email ->
            if (email.isBlank() || email == "guest@moneypilot.app") {
                flowOf(emptyList())
            } else {
                getGroupsUseCase(email)
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

    val selectedGroup: StateFlow<CoSplitGroup?> = _selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) {
                flowOf(null)
            } else {
                getGroupUseCase(groupId)
                    .catch { e ->
                        _error.emit("Database error: ${e.message}")
                        emit(null)
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val expenses: StateFlow<List<CoSplitExpense>> = selectedGroup
        .flatMapLatest { group ->
            if (group == null) {
                flowOf(emptyList())
            } else {
                getExpensesUseCase(group.id)
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

    val simplifiedSettlements: StateFlow<List<CoSplitSettlementRoute>> = selectedGroup
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
        _selectedGroupId.value = group?.id
        _aiSettlementExplanation.value = ""
    }

    fun selectGroupById(groupId: String) {
        _selectedGroupId.value = groupId
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
            val result = createCoSplitGroupUseCase(name, members, memberNames, id, email)
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
            val group = selectedGroup.value
            val id = userId.value
            if (group == null) {
                _error.emit("No group selected")
                onResult(false)
                return@launch
            }
            val result = addExpenseUseCase(
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
            val group = selectedGroup.value ?: return@launch
            val result = deleteExpenseUseCase(group.id, expenseId)
            if (result.isFailure) {
                _error.emit(result.exceptionOrNull()?.message ?: "Failed to delete expense")
            }
        }
    }

    fun settleUp(payerEmail: String, receiverEmail: String, amount: Double, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val group = selectedGroup.value ?: return@launch
            val id = userId.value
            val result = settleUpUseCase(group.id, payerEmail, receiverEmail, amount, id)
            if (result.isFailure) {
                _error.emit(result.exceptionOrNull()?.message ?: "Failed to settle up")
            }
            onResult(result.isSuccess)
        }
    }

    fun parseSplitCommand(commandText: String, onParsed: (AiParsedSplit?) -> Unit) {
        val group = selectedGroup.value ?: return
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
            val explanation = aiHelper.explainSettlements(simplifiedSettlements.value.map { it.displayText })
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
            val result = joinCoSplitGroupUseCase(groupId, email)
            if (result.isFailure) {
                _error.emit(result.exceptionOrNull()?.message ?: "Failed to join group")
            }
            onResult(result.isSuccess)
        }
    }

    fun deleteGroup(groupId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = deleteCoSplitGroupUseCase(groupId)
            if (result.isFailure) {
                _error.emit(result.exceptionOrNull()?.message ?: "Failed to delete group")
            } else {
                if (_selectedGroupId.value == groupId) {
                    _selectedGroupId.value = null
                }
            }
            onResult(result.isSuccess)
        }
    }

    fun updateGroupMembers(groupId: String, members: List<String>, memberNames: Map<String, String>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = updateCoSplitGroupMembersUseCase(groupId, members, memberNames)
            if (result.isFailure) {
                _error.emit(result.exceptionOrNull()?.message ?: "Failed to update members")
            }
            onResult(result.isSuccess)
        }
    }

    suspend fun getUserProfile(email: String): String? {
        return getUserProfileUseCase(email)
    }

    suspend fun getUserUpiId(email: String): String? {
        return getUserUpiIdUseCase(email)
    }

    fun saveUserUpiId(email: String, upiId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = saveUserUpiIdUseCase(email, upiId)
            onResult(result.isSuccess)
        }
    }

    fun generateSimplifiedSettlements(balances: Map<String, Double>, memberNames: Map<String, String> = emptyMap()): List<CoSplitSettlementRoute> {
        val list = mutableListOf<CoSplitSettlementRoute>()
        // Filter out members with essentially zero balance
        val members = balances.filterValues { kotlin.math.abs(it) > 0.05 }.toMutableMap()

        while (members.isNotEmpty()) {
            val debtor = members.minByOrNull { it.value } ?: break // largest negative owes most
            val creditor = members.maxByOrNull { it.value } ?: break // largest positive owed most

            if (kotlin.math.abs(debtor.value) < 0.05 || kotlin.math.abs(creditor.value) < 0.05) break

            val amountToSettle = kotlin.math.min(kotlin.math.abs(debtor.value), creditor.value)

            val debtorName = memberNames[debtor.key] ?: debtor.key
            val creditorName = memberNames[creditor.key] ?: creditor.key

            list.add(
                CoSplitSettlementRoute(
                    debtorEmail = debtor.key,
                    debtorName = debtorName,
                    creditorEmail = creditor.key,
                    creditorName = creditorName,
                    amount = amountToSettle
                )
            )

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
