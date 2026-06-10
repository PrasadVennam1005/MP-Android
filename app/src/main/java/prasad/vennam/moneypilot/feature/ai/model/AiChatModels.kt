package prasad.vennam.moneypilot.feature.ai.model

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val author: Author,
    val timestamp: Long = System.currentTimeMillis()
)

enum class Author {
    USER, AI
}

sealed interface LlmState {
    object Idle : LlmState
    object Initializing : LlmState
    object Ready : LlmState
    data class Error(val message: String) : LlmState
    data class Generating(val partialResponse: String) : LlmState
}
