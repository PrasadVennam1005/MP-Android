package prasad.vennam.moneypilot.feature.ai.model

/**
 * Typed result from a Gemini cloud API call.
 *
 *  - [Success]     → response text is available
 *  - [RateLimited] → HTTP 429: free-tier quota exceeded; tell UI to prompt local download
 *  - [Unavailable] → any other failure (network, 5xx, empty body, …)
 */
sealed interface CloudResult {
    data class Success(val text: String) : CloudResult
    object RateLimited : CloudResult
    object Unavailable : CloudResult
}
