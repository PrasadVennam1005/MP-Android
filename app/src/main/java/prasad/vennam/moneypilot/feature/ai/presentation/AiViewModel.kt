package prasad.vennam.moneypilot.feature.ai.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.feature.ai.domain.AiRepository
import prasad.vennam.moneypilot.feature.ai.model.*
import javax.inject.Inject

sealed interface DownloadWarning {
    data class InsufficientStorage(val requiredMB: Long, val availableMB: Long) : DownloadWarning
    data class MobileData(val requiredMB: Long) : DownloadWarning
}

/**
 * UI State for the AI Chat screen.
 */
data class AiUiState(
    val messages: List<ChatMessage> = emptyList(),
    val llmState: LlmState = LlmState.Idle,
    val downloadProgress: Float = 0f,
    val pendingAction: AiAction? = null,
    val isLocalModelAvailable: Boolean = false,
    val aiMode: Int = prasad.vennam.moneypilot.data.UserPreferences.AiMode.UNDECIDED,
    val downloadWarning: DownloadWarning? = null,
)

@HiltViewModel
class AiViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val aiRepository: AiRepository,
    ) : ViewModel() {
        private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
        private val _pendingAction = MutableStateFlow<AiAction?>(null)
        private val _actionFeedback = MutableSharedFlow<String>()

        val actionFeedback: SharedFlow<String> = _actionFeedback.asSharedFlow()

        /** Delegates consent state from the repository for observation in the UI. */
        val isUserConsentGranted = aiRepository.isUserConsentGranted

        /** Persists the user's opt-in decision for cloud AI. */
        fun grantConsent() {
            viewModelScope.launch { aiRepository.setUserConsent(true) }
        }

        private val _downloadWarning = MutableStateFlow<DownloadWarning?>(null)
        val downloadWarning: StateFlow<DownloadWarning?> = _downloadWarning.asStateFlow()

        // Consolidating multiple flows into a single UI State for better predictability
        val uiState: StateFlow<AiUiState> =
            combine(
                _messages,
                aiRepository.state,
                aiRepository.downloadProgress,
                _pendingAction,
                aiRepository.isLocalModelAvailable,
                aiRepository.aiMode,
                _downloadWarning
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                AiUiState(
                    messages = args[0] as List<ChatMessage>,
                    llmState = args[1] as LlmState,
                    downloadProgress = args[2] as Float,
                    pendingAction = args[3] as AiAction?,
                    isLocalModelAvailable = args[4] as Boolean,
                    aiMode = args[5] as Int,
                    downloadWarning = args[6] as DownloadWarning?
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AiUiState(),
            )

        init {
            viewModelScope.launch {
                aiRepository.initialize()
            }

            // Reactively update chat messages based on AI repository state changes
            aiRepository.state
                .onEach { state ->
                    when (state) {
                        is LlmState.Generating -> updateLastAiMessage(state.partialResponse)
                        is LlmState.ActionConfirm -> {
                            updateLastAiMessage(state.displayText, isTyping = false)
                            _pendingAction.value = state.action
                        }
                        is LlmState.Ready -> {
                            state.finalResponse?.let { updateLastAiMessage(it, isTyping = false) }
                        }
                        is LlmState.Error -> {
                            updateLastAiMessage(context.getString(R.string.ai_error_prefix, state.message), isTyping = false)
                        }
                        LlmState.RateLimited -> {
                            // Remove the typing placeholder/Thinking indicator so the UI shows the
                            // rate-limit card cleanly.
                            _messages.update { list ->
                                if (list.isNotEmpty() && list.last().author == Author.AI) {
                                    list.dropLast(1)
                                } else list
                            }
                        }
                        else -> {}
                    }
                }.launchIn(viewModelScope)
        }

        fun sendMessage(text: String) {
            if (text.isBlank()) return

            // Clear any pending action when user sends a new message
            _pendingAction.value = null

            val userMessage = ChatMessage(content = text, author = Author.USER)
            // Placeholder shows the typing indicator (isTyping=true) instead of a sentinel string
            val aiPlaceholder = ChatMessage(content = "", isTyping = true, author = Author.AI)

            _messages.update { it + userMessage + aiPlaceholder }

            viewModelScope.launch {
                try {
                    aiRepository.sendMessage(text)
                } catch (e: Exception) {
                    updateLastAiMessage(context.getString(R.string.ai_failed_to_send, e.localizedMessage), isTyping = false)
                }
            }
        }

        fun setAiMode(mode: Int) {
            viewModelScope.launch {
                aiRepository.setAiMode(mode)
                if (mode == prasad.vennam.moneypilot.data.UserPreferences.AiMode.LOCAL) {
                    checkAndDownloadModel()
                }
            }
        }

        fun clearDownloadWarning() {
            _downloadWarning.value = null
        }

        fun checkAndDownloadModel() {
            val destinationDir = context.getExternalFilesDir(null) ?: context.filesDir
            val usableSpace = destinationDir.usableSpace
            val isEmu = isEmulator()
            val requiredSpaceBytes = if (isEmu) 2_000_000_000L else 3_000_000_000L

            if (usableSpace < requiredSpaceBytes) {
                val requiredMB = requiredSpaceBytes / (1024 * 1024)
                val availableMB = usableSpace / (1024 * 1024)
                _downloadWarning.value = DownloadWarning.InsufficientStorage(requiredMB, availableMB)
                return
            }

            // Check if connected via mobile data
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val isWifi = capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ?: false
            val isCellular = capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ?: false

            if (isCellular && !isWifi) {
                val requiredMB = requiredSpaceBytes / (1024 * 1024)
                _downloadWarning.value = DownloadWarning.MobileData(requiredMB)
                return
            }

            downloadModel()
        }

        fun downloadModel(force: Boolean = false) {
            if (force) {
                clearDownloadWarning()
            }
            // Prevent duplicate triggers
            if (aiRepository.state.value is LlmState.Downloading) return

            viewModelScope.launch {
                aiRepository.downloadModel()
            }
        }

        private fun isEmulator(): Boolean {
            val fingerprint = android.os.Build.FINGERPRINT ?: ""
            val model = android.os.Build.MODEL ?: ""
            val manufacturer = android.os.Build.MANUFACTURER ?: ""
            val brand = android.os.Build.BRAND ?: ""
            val device = android.os.Build.DEVICE ?: ""
            val product = android.os.Build.PRODUCT ?: ""

            return fingerprint.contains("generic") ||
                fingerprint.startsWith("unknown") ||
                model.contains("google_sdk") ||
                model.contains("sdk_gphone64_arm64") ||
                manufacturer.contains("google") ||
                (brand.startsWith("google") && device.startsWith("emu64a")) ||
                product == "sdk_gphone64_arm64"
        }

        fun confirmAction(action: AiAction) {
            viewModelScope.launch {
                val result = aiRepository.executeAction(action)
                _pendingAction.value = null
                result
                    .onSuccess { msg ->
                        _actionFeedback.emit(msg)
                        _messages.update { it + ChatMessage(content = "\u2705 $msg", author = Author.AI) }
                    }.onFailure { err ->
                        val errorMsg = err.message ?: context.getString(R.string.unknown_error)
                        _actionFeedback.emit(context.getString(R.string.ai_error_prefix, errorMsg))
                        _messages.update { it + ChatMessage(content = "\u274C ${context.getString(R.string.ai_error_prefix, errorMsg)}", author = Author.AI) }
                    }
            }
        }

        fun dismissAction() {
            _pendingAction.value = null
            _messages.update { it + ChatMessage(content = context.getString(R.string.ai_action_cancelled), author = Author.AI) }
        }

        private fun updateLastAiMessage(content: String, isTyping: Boolean = content.isEmpty()) {
            _messages.update { currentList ->
                if (currentList.isNotEmpty() && currentList.last().author == Author.AI) {
                    val newList = currentList.toMutableList()
                    newList[newList.lastIndex] = newList.last().copy(
                        content = content,
                        isTyping = isTyping
                    )
                    newList
                } else {
                    currentList
                }
            }
        }

        override fun onCleared() {
            super.onCleared()
            aiRepository.cleanup()
        }
    }
