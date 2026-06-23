package prasad.vennam.moneypilot.feature.ai.service

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.*
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import prasad.vennam.moneypilot.feature.ai.model.LlmResponse

/**
 * LlmService - manages the LiteRT-LM engine lifecycle.
 *
 * Strategy:
 *  1. Try GPU backend first (for real physical devices with GPU support)
 *  2. On failure, fall back to CPU backend (works on emulators and devices without GPU drivers)
 *
 * Known issues on emulators:
 *  - GPU backend always fails (emulators have no real GPU compute support for LiteRT)
 *  - CPU backend works but is slow; reduce maxNumTokens to stay within emulator RAM limits
 */
class LlmService(
    private val context: Context,
) {
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private val client = OkHttpClient()

    private val _partialResponses =
        MutableSharedFlow<LlmResponse>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val partialResponses: SharedFlow<LlmResponse> = _partialResponses.asSharedFlow()

    private val accumulated = StringBuilder()

    // Detect if running on an emulator; lower token limits to avoid OOM
    private val isEmulator: Boolean by lazy {
        android.os.Build.FINGERPRINT
            .contains("generic") ||
            android.os.Build.FINGERPRINT
                .startsWith("unknown") ||
            android.os.Build.MODEL
                .contains("google_sdk") ||
            android.os.Build.MODEL
                .contains("Emulator") ||
            android.os.Build.MODEL
                .contains("Android SDK built for") ||
            android.os.Build.MANUFACTURER
                .contains("Genymotion") ||
            (
                android.os.Build.BRAND
                    .startsWith("generic") &&
                    android.os.Build.DEVICE
                        .startsWith("generic")
            ) ||
            android.os.Build.PRODUCT == "google_sdk"
    }

    /**
     * Initializes the LiteRT-LM engine.
     * Tries GPU first, then falls back to CPU.
     */
    fun initialize(modelPath: String) {
        if (engine != null) {
            Log.d(TAG, "Engine already initialized, skipping.")
            return
        }

        val maxTokens =
            if (isEmulator) {
                Log.d(TAG, "Emulator detected — using maxNumTokens (1024)")
                1024
            } else {
                Log.d(TAG, "Physical device detected — using maxNumTokens (2048)")
                2048
            }

        Log.d(TAG, "Initializing LiteRT-LM. Model: $modelPath, maxTokens: $maxTokens, isEmulator: $isEmulator")

        // 1. Attempt GPU backend (only relevant on physical devices)
        if (!isEmulator) {
            try {
                Log.d(TAG, "Attempting GPU backend...")
                val gpuConfig =
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.GPU(),
                        maxNumTokens = maxTokens,
                        cacheDir = context.cacheDir.absolutePath,
                    )
                engine = Engine(gpuConfig).apply { initialize() }
                conversation = engine?.createConversation()
                Log.d(TAG, "GPU backend initialized successfully.")
                return
            } catch (gpuEx: Exception) {
                Log.w(TAG, "GPU backend failed: ${gpuEx.message}. Falling back to CPU...")
                safeCloseEngine()
            }
        } else {
            Log.d(TAG, "Skipping GPU backend on emulator (not supported).")
        }

        // 2. CPU backend fallback
        try {
            Log.d(TAG, "Attempting CPU backend...")
            val cpuConfig =
                EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU(),
                    maxNumTokens = maxTokens,
                    cacheDir = context.cacheDir.absolutePath,
                )
            engine = Engine(cpuConfig).apply { initialize() }
            conversation = engine?.createConversation()
            Log.d(TAG, "CPU backend initialized successfully.")
        } catch (cpuEx: Exception) {
            Log.e(TAG, "CPU backend also failed: ${cpuEx.message}", cpuEx)
            safeCloseEngine()
            throw RuntimeException(
                "Failed to initialize LiteRT-LM on both GPU and CPU.\n" +
                    "Model path: $modelPath\n" +
                    "Error: ${cpuEx.message}\n" +
                    "This may be caused by:\n" +
                    "  - Insufficient device RAM (model requires ~3GB)\n" +
                    "  - Corrupted or incomplete model file\n" +
                    "  - Incompatible model format for this device",
                cpuEx,
            )
        }
    }

    fun generateResponse(prompt: String): String {
        val eng = engine ?: throw IllegalStateException("LLM not initialized")
        try {
            conversation?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing previous conversation: ${e.message}")
        }
        val convo = eng.createConversation()
        conversation = convo
        var response = ""
        runBlocking {
            try {
                convo.sendMessageAsync(prompt).collect { token ->
                    response += token
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in generateResponse", e)
            }
        }
        return response
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    fun generateResponseStreaming(prompt: String) {
        Log.d(TAG, "generateResponseStreaming called. Prompt length: ${prompt.length}")
        val eng =
            engine ?: run {
                Log.e(TAG, "Cannot generate response: LLM is not initialized (engine is null)")
                throw IllegalStateException("LLM not initialized")
            }
        try {
            conversation?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing previous conversation: ${e.message}")
        }
        val convo = eng.createConversation()
        conversation = convo
        accumulated.clear()

        GlobalScope.launch(Dispatchers.Default) {
            try {
                Log.d(TAG, "Starting token collection. Prompt preview: ${prompt.take(150)}...")
                var tokenCount = 0
                convo.sendMessageAsync(prompt).collect { token ->
                    tokenCount++
                    Log.v(TAG, "Token #$tokenCount: '$token'")
                    accumulated.append(token)
                    _partialResponses.tryEmit(LlmResponse(accumulated.toString(), false))
                }
                Log.d(TAG, "Token collection complete. Total: $tokenCount tokens, response length: ${accumulated.length}")
                _partialResponses.tryEmit(LlmResponse(accumulated.toString(), true))
            } catch (e: Exception) {
                Log.e(TAG, "Error during streaming token collection", e)
                _partialResponses.tryEmit(LlmResponse(accumulated.toString(), true))
            }
        }
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    fun generateCloudResponseStreaming(prompt: String) {
        Log.d(TAG, "generateCloudResponseStreaming called. Prompt length: ${prompt.length}")
        accumulated.clear()
        _partialResponses.tryEmit(LlmResponse("Thinking...", false))

        GlobalScope.launch(Dispatchers.Default) {
            try {
                val response = generateCloudResponse(prompt)
                if (response != null) {
                    accumulated.clear()
                    accumulated.append(response)
                    _partialResponses.tryEmit(LlmResponse(accumulated.toString(), true))
                } else {
                    _partialResponses.tryEmit(LlmResponse("Cloud AI is currently unavailable. Please check your internet connection or try again later.", true))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during cloud streaming generation", e)
                _partialResponses.tryEmit(LlmResponse("Error during cloud generation: ${e.message}", true))
            }
        }
    }

    suspend fun generateCloudResponse(prompt: String): String? =
        withContext(Dispatchers.IO) {
            val apiKey = prasad.vennam.moneypilot.BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "\"\"") {
                Log.d(TAG, "generateCloudResponse: GEMINI_API_KEY is empty/placeholder, skipping cloud fallback")
                return@withContext null
            }

            Log.d(TAG, "generateCloudResponse: Sending query to Gemini API Cloud")
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

            val escapedPrompt = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")

            val requestBodyJson = """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "$escapedPrompt"
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toRequestBody(mediaType))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    Log.d(TAG, "generateCloudResponse: HTTP code = ${response.code}")
                    if (!response.isSuccessful || body.isNullOrEmpty()) {
                        Log.e(TAG, "generateCloudResponse failed: code=${response.code}, body=$body")
                        return@withContext null
                    }
                    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                    val geminiResponse = moshi.adapter(GeminiResponse::class.java).fromJson(body)
                    val responseText = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    Log.d(TAG, "generateCloudResponse: Successfully parsed response of length: ${responseText?.length ?: 0}")
                    responseText
                }
            } catch (e: Exception) {
                Log.e(TAG, "generateCloudResponse Exception: ${e.message}", e)
                null
            }
        }

    fun close() {
        safeCloseEngine()
    }

    private fun safeCloseEngine() {
        try {
            conversation?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing conversation (ignored): ${e.message}")
        } finally {
            conversation = null
        }
        try {
            engine?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing engine (ignored): ${e.message}")
        } finally {
            engine = null
        }
    }

    companion object {
        private const val TAG = "LlmService"
    }
}

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>?
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String?
)
