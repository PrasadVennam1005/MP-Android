package prasad.vennam.moneypilot.feature.ai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.feature.ai.domain.AiRepository
import prasad.vennam.moneypilot.feature.ai.model.AiAction
import prasad.vennam.moneypilot.feature.ai.model.Author
import prasad.vennam.moneypilot.feature.ai.model.ChatMessage
import prasad.vennam.moneypilot.feature.ai.model.LlmState
import javax.inject.Inject

@HiltViewModel
class AiViewModel
    @Inject
    constructor(
        private val aiRepository: AiRepository,
    ) : ViewModel() {
        private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
        val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

        val aiState = aiRepository.state
        val downloadProgress = aiRepository.downloadProgress

        private val _pendingAction = MutableStateFlow<AiAction?>(null)
        val pendingAction: StateFlow<AiAction?> = _pendingAction.asStateFlow()

        private val _actionFeedback = MutableSharedFlow<String>()
        val actionFeedback: SharedFlow<String> = _actionFeedback.asSharedFlow()

        init {
            viewModelScope.launch {
                aiRepository.initialize()
            }

            // Listen to AI responses and update the last AI message
            aiRepository.state
                .onEach { state ->
                    when (state) {
                        is LlmState.Generating -> updateLastAiMessage(state.partialResponse)
                        is LlmState.ActionConfirm -> {
                            updateLastAiMessage(state.displayText)
                            _pendingAction.value = state.action
                        }
                        is LlmState.Ready -> {
                            state.finalResponse?.let { updateLastAiMessage(it) }
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
            _messages.value += userMessage

            // Prepare a placeholder for AI response
            val aiPlaceholder = ChatMessage(content = "...", author = Author.AI)
            _messages.value += aiPlaceholder

            viewModelScope.launch {
                aiRepository.sendMessage(text)
            }
        }

        fun downloadModel() {
            // Check state to prevent duplicate triggers
            if (aiRepository.state.value is LlmState.Downloading) return

            viewModelScope.launch {
                aiRepository.downloadModel()
            }
        }

        fun confirmAction(action: AiAction) {
            viewModelScope.launch {
                val result = aiRepository.executeAction(action)
                _pendingAction.value = null
                result
                    .onSuccess { msg ->
                        _actionFeedback.emit(msg)
                        _messages.value += ChatMessage(content = "✅ $msg", author = Author.AI)
                    }.onFailure { err ->
                        _actionFeedback.emit("Error: ${err.message}")
                        _messages.value += ChatMessage(content = "❌ Failed: ${err.message}", author = Author.AI)
                    }
            }
        }

        fun dismissAction() {
            _pendingAction.value = null
            _messages.value += ChatMessage(content = "Action cancelled.", author = Author.AI)
        }

        private fun updateLastAiMessage(content: String) {
            val currentList = _messages.value.toMutableList()
            if (currentList.isNotEmpty() && currentList.last().author == Author.AI) {
                currentList[currentList.lastIndex] = currentList.last().copy(content = content)
                _messages.value = currentList
            }
        }

        override fun onCleared() {
            super.onCleared()
            aiRepository.cleanup()
        }
    }
