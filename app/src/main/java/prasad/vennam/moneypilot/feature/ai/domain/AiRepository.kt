package prasad.vennam.moneypilot.feature.ai.domain

import kotlinx.coroutines.flow.Flow
import prasad.vennam.moneypilot.feature.ai.model.LlmState

interface AiRepository {
    val state: Flow<LlmState>
    suspend fun initialize()
    suspend fun sendMessage(prompt: String)
    fun cleanup()
}
