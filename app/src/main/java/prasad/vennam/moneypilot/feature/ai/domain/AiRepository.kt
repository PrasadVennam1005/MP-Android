package prasad.vennam.moneypilot.feature.ai.domain

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.feature.ai.model.AiAction
import prasad.vennam.moneypilot.feature.ai.model.LlmState

interface AiRepository {
    val state: Flow<LlmState>
    val downloadProgress: Flow<Float>

    suspend fun initialize()

    suspend fun downloadModel()

    suspend fun sendMessage(prompt: String)

    suspend fun executeAction(action: AiAction): Result<String>

    suspend fun generateShortAdvice(summary: String): String

    fun cleanup()
}
