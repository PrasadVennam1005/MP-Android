package prasad.vennam.moneypilot.feature.ai.service

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

class LlmService(private val context: Context) {
    private var llmInference: LlmInference? = null
    
    private val _partialResponses = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val partialResponses: SharedFlow<String> = _partialResponses.asSharedFlow()

    fun initialize(modelPath: String) {
        if (llmInference != null) return
        
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(512)
            .setTopK(40)
            .setTemperature(0.7f)
            .setRandomSeed(42)
            .build()
            
        llmInference = LlmInference.createFromOptions(context, options)
    }

    suspend fun generateResponse(prompt: String): String {
        val inference = llmInference ?: throw IllegalStateException("LLM not initialized")
        return inference.generateResponse(prompt)
    }

    fun generateResponseStreaming(prompt: String) {
        val inference = llmInference ?: throw IllegalStateException("LLM not initialized")
        inference.generateResponseAsync(prompt) { partialResult, done ->
            _partialResponses.tryEmit(partialResult)
        }
    }
    
    fun close() {
        llmInference?.close()
        llmInference = null
    }
}
