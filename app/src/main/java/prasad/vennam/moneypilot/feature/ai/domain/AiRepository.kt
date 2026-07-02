package prasad.vennam.moneypilot.feature.ai.domain

import kotlinx.coroutines.flow.StateFlow
import prasad.vennam.moneypilot.feature.ai.model.AiAction
import prasad.vennam.moneypilot.feature.ai.model.LlmState
import prasad.vennam.moneypilot.util.ParsedReceipt

interface AiRepository {
    val state: StateFlow<LlmState>
    val downloadProgress: StateFlow<Float>

    /**
     * Whether the user has opted in to sending financial context to the cloud Gemini API.
     * If false, cloud requests must not be made.
     */
    val isUserConsentGranted: StateFlow<Boolean>
    val isLocalModelAvailable: StateFlow<Boolean>

    /** Persist the user's cloud AI consent choice. */
    suspend fun setUserConsent(granted: Boolean)

    suspend fun initialize()

    suspend fun downloadModel()

    suspend fun sendMessage(prompt: String)

    suspend fun executeAction(action: AiAction): Result<String>

    suspend fun generateShortAdvice(summary: String): String

    suspend fun parseReceiptText(ocrText: String): ParsedReceipt?

    fun cleanup()
}
