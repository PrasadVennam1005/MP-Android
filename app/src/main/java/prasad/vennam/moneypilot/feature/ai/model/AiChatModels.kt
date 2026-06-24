package prasad.vennam.moneypilot.feature.ai.model

data class ChatMessage(
    val id: String =
        java.util.UUID
            .randomUUID()
            .toString(),
    val content: String,
    val author: Author,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class Author {
    USER,
    AI,
}

sealed interface LlmState {
    object Idle : LlmState

    object Initializing : LlmState

    object Downloading : LlmState

    data class Ready(
        val finalResponse: String? = null,
    ) : LlmState

    data class Error(
        val message: String,
    ) : LlmState

    data class Generating(
        val partialResponse: String,
    ) : LlmState

    /** Shown after generation completes and an action tag is detected — awaiting user confirm/dismiss */
    data class ActionConfirm(
        val action: prasad.vennam.moneypilot.feature.ai.model.AiAction,
        val displayText: String,
    ) : LlmState
}

data class LlmResponse(
    val text: String,
    val isDone: Boolean,
)
