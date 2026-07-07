package prasad.vennam.moneypilot.feature.cosplit.util

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonClass
import prasad.vennam.moneypilot.feature.ai.model.CloudResult
import prasad.vennam.moneypilot.feature.ai.service.LlmService
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class AiParsedSplit(
    val description: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "",
    val splitType: String = "EQUAL",
    val splitDetails: Map<String, Double> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class AiReceiptLineItem(
    val name: String = "",
    val price: Double = 0.0
)

@Singleton
class CoSplitAiHelper @Inject constructor(
    private val llmService: LlmService,
    private val moshi: Moshi
) {
    private val TAG = "CoSplitAiHelper"

    suspend fun parseSplitCommand(
        commandText: String,
        groupMembers: List<String>,
        currentUserEmail: String
    ): AiParsedSplit? {
        val prompt = """
            You are CoSplit AI. Analyze this command message: "$commandText".
            Convert it into a structured JSON expense.
            The available group members list: $groupMembers.
            The current user's email: $currentUserEmail.
            
            Rules:
            1. Identify the description of the expense.
            2. Extract the total numeric amount.
            3. Identify who paid. Use the email address from the group members list. If it says "me", "I", or "my", use the current user's email ($currentUserEmail).
            4. Identify the splitType (EQUAL, EXACT, PERCENT). If not specified, default to EQUAL.
            5. Map splitDetails containing email address as keys and split values (amounts/percentages/shares) as values. Ensure every email key belongs to the group members list.
            
            Output ONLY valid JSON matching this schema, with no other text, explanation or markdown code block wrapper:
            {
              "description": "string",
              "amount": double,
              "paidBy": "string",
              "splitType": "string",
              "splitDetails": {
                "email1": double,
                "email2": double
              }
            }
        """.trimIndent()

        return try {
            when (val result = llmService.generateCloudResponse(prompt)) {
                is CloudResult.Success -> {
                    val rawJson = cleanJsonResponse(result.text)
                    Log.d(TAG, "parseSplitCommand: parsed json=$rawJson")
                    moshi.adapter(AiParsedSplit::class.java).fromJson(rawJson)
                }
                else -> {
                    Log.e(TAG, "parseSplitCommand: Failed to generate cloud response")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseSplitCommand Exception: ${e.message}", e)
            null
        }
    }

    suspend fun parseReceiptToLineItems(ocrText: String): List<AiReceiptLineItem>? {
        val prompt = """
            You are a receipt line-item extractor. Analyze the OCR text:
            $ocrText
            
            Extract all individual purchase items with their name and price.
            Ignore totals, taxes, discounts, or meta details.
            
            Output ONLY valid JSON matching this schema (a JSON array of objects), with no explanation or markdown code block wrapper:
            [
              {
                "name": "item description",
                "price": double
              }
            ]
        """.trimIndent()

        return try {
            when (val result = llmService.generateCloudResponse(prompt)) {
                is CloudResult.Success -> {
                    val rawJson = cleanJsonResponse(result.text)
                    Log.d(TAG, "parseReceiptToLineItems: parsed json=$rawJson")
                    val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, AiReceiptLineItem::class.java)
                    moshi.adapter<List<AiReceiptLineItem>>(type).fromJson(rawJson)
                }
                else -> {
                    Log.e(TAG, "parseReceiptToLineItems: Failed to generate cloud response")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseReceiptToLineItems Exception: ${e.message}", e)
            null
        }
    }

    suspend fun explainSettlements(settlements: List<String>): String {
        if (settlements.isEmpty()) return "You're all square! No settlements needed."
        val prompt = """
            Explain these debt settlement steps in a very friendly, encouraging, and conversational tone for a group of friends:
            $settlements
            
            Tell them exactly who needs to pay whom and how much, so they can get square. Keep it brief.
        """.trimIndent()

        return when (val result = llmService.generateCloudResponse(prompt)) {
            is CloudResult.Success -> result.text.trim()
            else -> "Here are the settlements to do:\n" + settlements.joinToString("\n")
        }
    }

    private fun cleanJsonResponse(text: String): String {
        // Strip markdown code block wrappers like ```json or ``` if present
        return text
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
