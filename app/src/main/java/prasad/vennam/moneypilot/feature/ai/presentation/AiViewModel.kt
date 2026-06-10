package prasad.vennam.moneypilot.feature.ai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.feature.ai.domain.AiRepository
import prasad.vennam.moneypilot.feature.ai.model.Author
import prasad.vennam.moneypilot.feature.ai.model.ChatMessage
import prasad.vennam.moneypilot.feature.ai.model.LlmState
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val aiState = aiRepository.state

    init {
        viewModelScope.launch {
            aiRepository.initialize()
        }
        
        // Listen to AI responses and update the last AI message
        aiRepository.state
            .filterIsInstance<LlmState.Generating>()
            .onEach { state ->
                updateLastAiMessage(state.partialResponse)
            }
            .launchIn(viewModelScope)
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        val userMessage = ChatMessage(content = text, author = Author.USER)
        _messages.value = _messages.value + userMessage
        
        // Prepare a placeholder for AI response
        val aiPlaceholder = ChatMessage(content = "...", author = Author.AI)
        _messages.value = _messages.value + aiPlaceholder
        
        viewModelScope.launch {
            aiRepository.sendMessage(text)
        }
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
