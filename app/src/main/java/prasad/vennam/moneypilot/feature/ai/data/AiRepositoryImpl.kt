package prasad.vennam.moneypilot.feature.ai.data

import android.content.Context
import kotlinx.coroutines.flow.*
import prasad.vennam.moneypilot.feature.ai.domain.AiRepository
import prasad.vennam.moneypilot.feature.ai.model.LlmState
import prasad.vennam.moneypilot.feature.ai.service.LlmService
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val context: Context,
    private val llmService: LlmService
) : AiRepository {

    private val _state = MutableStateFlow<LlmState>(LlmState.Idle)
    override val state: Flow<LlmState> = _state.asStateFlow()

    init {
        llmService.partialResponses
            .onEach { partial ->
                _state.value = LlmState.Generating(partial)
            }
            .launchIn(kotlinx.coroutines.GlobalScope) // Simplification for now
    }

    override suspend fun initialize() {
        if (_state.value is LlmState.Ready) return
        
        _state.value = LlmState.Initializing
        try {
            // In a real app, you would download the model or verify it exists
            // For now, we look for 'gemma-2b-it-cpu-int4.bin' in files directory
            val modelFile = File(context.filesDir, "gemma-2b-it-cpu-int4.bin")
            if (!modelFile.exists()) {
                _state.value = LlmState.Error("Model file not found. Please download Gemma model to ${modelFile.absolutePath}")
                return
            }
            
            llmService.initialize(modelFile.absolutePath)
            _state.value = LlmState.Ready
        } catch (e: Exception) {
            _state.value = LlmState.Error("Failed to initialize: ${e.message}")
        }
    }

    override suspend fun sendMessage(prompt: String) {
        if (_state.value !is LlmState.Ready && _state.value !is LlmState.Generating) {
            _state.value = LlmState.Error("AI not ready")
            return
        }
        
        try {
            llmService.generateResponseStreaming(prompt)
        } catch (e: Exception) {
            _state.value = LlmState.Error("Generation failed: ${e.message}")
        }
    }

    override fun cleanup() {
        llmService.close()
        _state.value = LlmState.Idle
    }
}
