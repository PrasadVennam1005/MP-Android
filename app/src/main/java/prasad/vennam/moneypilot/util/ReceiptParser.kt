package prasad.vennam.moneypilot.util

import com.google.mlkit.vision.text.Text
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

data class ParsedReceipt(
    val merchant: String? = null,
    val amount: Double? = null,
    val date: Long? = null,
)

object ReceiptParser {
    private val amountRegex =
        Pattern.compile(
            "(?i)(?:\\p{Sc}|rs\\.?|inr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        )

    private val merchantIgnoreWords =
        setOf(
            "invoice",
            "tax invoice",
            "gst invoice",
            "gstin",
            "phone",
            "mobile",
            "tel",
            "date",
            "bill no",
            "receipt no",
            "customer copy",
            "cashier",
            "qty",
            "quantity",
            "rate",
            "amount",
            "subtotal",
            "total",
            "cgst",
            "sgst",
            "igst",
            "hsn",
            "thank you",
            "thanks",
            "visit again",
            "www.",
            ".com",
            ".in",
            ".org",
            "email",
        )

    private val amountKeywords =
        mapOf(
            "grand total" to 100,
            "total amount" to 95,
            "amount paid" to 90,
            "net payable" to 85,
            "net amount" to 80,
            "payable" to 75,
            "total" to 70,
            "paid" to 65,
            "payment" to 60,
            "debited" to 60,
            "sent" to 55,
            "received" to 55,
            "fare" to 50,
            "bill amount" to 50,
            "subtotal" to 20,
            "cgst" to 5,
            "sgst" to 5,
            "igst" to 5,
        )

    fun parse(visionText: Text): ParsedReceipt {
        val lines =
            visionText.textBlocks
                .flatMap { block -> block.lines }
                .sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
                .map { it.text.trim() }
                .filter { it.isNotBlank() }

        val merchant = extractMerchant(lines)
        val amount = extractAmount(lines)
        val date = extractDate(lines)

        return ParsedReceipt(
            merchant = merchant,
            amount = amount,
            date = date,
        )
    }

    private fun cleanOcrLine(line: String): String {
        val numberCandidateRegex = Regex("(?i)^(?:\\p{Sc}|rs\\.?|inr)?[0-9oOlI|.,]+$")
        return line.split("\\s+".toRegex()).joinToString(" ") { word ->
            if (numberCandidateRegex.matches(word)) {
                word
                    .replace(Regex("[oO]"), "0")
                    .replace(Regex("[lI|]"), "1")
            } else {
                word
            }
        }
    }

    private fun extractMerchant(lines: List<String>): String? {
        val rawMerchant =
            lines.firstOrNull { line ->
                val lower = line.lowercase(Locale.getDefault())

                merchantIgnoreWords.none {
                    lower.contains(it)
                } &&
                    !line.any { char -> char.isDigit() } &&
                    line.length > 2 &&
                    line.length < 60
            } ?: return null

        val lower = rawMerchant.lowercase(Locale.getDefault())
        return when {
            lower.startsWith("welcome to ") -> rawMerchant.substring(11).trim()
            lower.startsWith("welcome ") -> rawMerchant.substring(8).trim()
            else -> rawMerchant.trim()
        }
    }

    private fun extractAmount(lines: List<String>): Double? {
        val cleanedLines = lines.map { cleanOcrLine(it) }
        var bestAmount: Double? = null
        var bestScore = -1

        cleanedLines.forEachIndexed { index, line ->
            val lower = line.lowercase(Locale.getDefault())
            val prevLine = if (index > 0) cleanedLines[index - 1].lowercase(Locale.getDefault()) else ""
            val nextLine = if (index < cleanedLines.size - 1) cleanedLines[index + 1].lowercase(Locale.getDefault()) else ""

            val matcher = amountRegex.matcher(line)

            while (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "") ?: continue
                if (amountStr.substringBefore('.').length > 7) continue

                val amount = amountStr.toDoubleOrNull() ?: continue
                if (amount <= 0 || amount > 100000.0) continue

                var score = 0

                amountKeywords.forEach { (keyword, keywordScore) ->
                    val regex = Regex("\\b${Regex.escape(keyword)}\\b")
                    if (regex.containsMatchIn(lower)) {
                        score += keywordScore
                    } else if (regex.containsMatchIn(prevLine)) {
                        score += (keywordScore * 0.8).toInt()
                    } else if (regex.containsMatchIn(nextLine)) {
                        score += (keywordScore * 0.5).toInt()
                    }
                }

                if (line.contains(Regex("\\p{Sc}"))) score += 20
                if (line.contains("Rs", true)) score += 20
                if (line.contains("INR", true)) score += 20

                if (amount > 100) score += 10
                if (amount > 500) score += 10

                if (score > bestScore) {
                    bestScore = score
                    bestAmount = amount
                }
            }
        }

        if (bestAmount != null) {
            return bestAmount
        }

        val filteredLines =
            cleanedLines.filter { line ->
                val lower = line.lowercase(Locale.getDefault())
                val containsIgnoreMetadata =
                    listOf(
                        "phone",
                        "mobile",
                        "tel",
                        "invoice",
                        "date",
                        "bill no",
                        "receipt no",
                        "gstin",
                        "zip",
                        "pin",
                        "card",
                        "tax",
                        "number",
                        "ac no",
                        "a/c",
                        "fax",
                        "email",
                    ).any { lower.contains(it) }
                !containsIgnoreMetadata
            }

        return filteredLines
            .flatMap { line ->
                amountRegex.matcher(line).run {
                    generateSequence {
                        if (find()) group(1) else null
                    }.toList()
                }
            }.map { it.replace(",", "") }
            .filter { it.substringBefore('.').length <= 7 }
            .mapNotNull { it.toDoubleOrNull() }
            .filter { it > 0 && it <= 100000.0 }
            .maxOrNull()
    }

    private fun extractDate(lines: List<String>): Long? {
        val datePatterns =
            listOf(
                "\\d{1,2}/\\d{1,2}/\\d{2,4}",
                "\\d{1,2}-\\d{1,2}-\\d{2,4}",
                "\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}",
                "\\d{4}-\\d{1,2}-\\d{1,2}",
                "\\d{4}/\\d{1,2}/\\d{1,2}",
                "\\d{1,2}\\s+[A-Za-z]{3,9}\\s+\\d{2,4}",
                "[A-Za-z]{3,9}\\s+\\d{1,2},?\\s+\\d{2,4}",
            )

        val formats =
            listOf(
                "dd/MM/yyyy",
                "dd/MM/yy",
                "dd-MM-yyyy",
                "dd-MM-yy",
                "dd.MM.yyyy",
                "dd.MM.yy",
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "dd MMM yyyy",
                "dd MMMM yyyy",
                "MMM dd yyyy",
                "MMMM dd yyyy",
            )

        for (line in lines) {
            for (pattern in datePatterns) {
                val regex = Regex(pattern)
                val match = regex.find(line)

                if (match != null) {
                    val dateStr = match.value.replace(",", "").trim()

                    for (format in formats) {
                        try {
                            val sdf = SimpleDateFormat(format, Locale.ENGLISH).apply { isLenient = false }
                            val parsed = sdf.parse(dateStr)
                            if (parsed != null) {
                                return parsed.time
                            }
                        } catch (_: Exception) {
                            try {
                                val sdf = SimpleDateFormat(format, Locale.getDefault()).apply { isLenient = false }
                                val parsed = sdf.parse(dateStr)
                                if (parsed != null) {
                                    return parsed.time
                                }
                            } catch (__: Exception) {
                            }
                        }
                    }
                }
            }
        }

        return null
    }
}
