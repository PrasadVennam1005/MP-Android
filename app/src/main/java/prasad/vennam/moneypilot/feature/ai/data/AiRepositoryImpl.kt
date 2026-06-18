package prasad.vennam.moneypilot.feature.ai.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import prasad.vennam.moneypilot.feature.ai.domain.AiActionParser
import prasad.vennam.moneypilot.feature.ai.domain.AiRepository
import prasad.vennam.moneypilot.feature.ai.model.AiAction
import prasad.vennam.moneypilot.feature.ai.model.LlmState
import prasad.vennam.moneypilot.feature.ai.service.LlmService
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class AiRepositoryImpl
    @Inject
    constructor(
        private val context: Context,
        private val llmService: LlmService,
        private val moneyPilotRepository: MoneyPilotRepository,
    ) : AiRepository {
        private val _state = MutableStateFlow<LlmState>(LlmState.Idle)
        override val state: StateFlow<LlmState> = _state.asStateFlow()

        private val _downloadProgress = MutableStateFlow(0f)
        override val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

        // Emulator detection: use a small, CPU-compatible model
        private val isEmulator: Boolean by lazy {
            android.os.Build.FINGERPRINT
                .contains("generic") ||
                android.os.Build.FINGERPRINT
                    .startsWith("unknown") ||
                android.os.Build.MODEL
                    .contains("google_sdk") ||
                android.os.Build.MODEL
                    .contains("sdk_gphone64_arm64") ||
                android.os.Build.MODEL
                    .contains("sdk_gphone64_arm64") ||
                android.os.Build.MANUFACTURER
                    .contains("google") ||
                (
                    android.os.Build.BRAND
                        .startsWith("google") &&
                        android.os.Build.DEVICE
                            .startsWith("emu64a")
                ) ||
                android.os.Build.PRODUCT == "sdk_gphone64_arm64"
        }

        /**
         * Model configuration:
         * - Emulator / CPU-only: Qwen2.5-1.5B-Instruct q8 (~1.5GB).
         *   Apache 2.0 license — NO HuggingFace login required. Runs CPU inference on emulator.
         * - Physical device (GPU): Gemma 4 E2B IT (~2.58GB). Best quality, requires GPU/NPU.
         */
        private val modelFileName: String
            get() = if (isEmulator) EMULATOR_MODEL_FILE else DEVICE_MODEL_FILE

        private val modelUrl: String
            get() = if (isEmulator) EMULATOR_MODEL_URL else DEVICE_MODEL_URL

        // Required free disk space per model
        private val requiredSpaceBytes: Long
            get() = if (isEmulator) 2_000_000_000L else 3_000_000_000L // 2GB or 3GB

        private val downloadClient =
            OkHttpClient
                .Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()

        init {
            Log.d(TAG, "AiRepositoryImpl created. isEmulator=$isEmulator, modelFile=$modelFileName")

            // Listen to local model generation responses
            llmService.partialResponses
                .onEach { response ->
                    if (!response.isDone) {
                        // Stream partial tokens — strip any partial action tag from displayed text
                        val displayText = response.text.substringBefore("[ACTION:").trimEnd()
                        _state.value = LlmState.Generating(displayText.ifEmpty { response.text })
                    } else {
                        // Generation complete — parse for action tags
                        val (action, cleanedText) = AiActionParser.parse(response.text)
                        if (action != null) {
                            Log.d(TAG, "Action detected: $action")
                            _state.value = LlmState.ActionConfirm(action, cleanedText)
                        } else {
                            _state.value = LlmState.Ready
                        }
                    }
                }.launchIn(kotlinx.coroutines.GlobalScope)
        }

        private fun getModelFile(): File {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            return File(dir, modelFileName)
        }

        override suspend fun initialize() {
            if (_state.value is LlmState.Ready) return

            val modelFile = getModelFile()
            Log.d(TAG, "Initializing with model: ${modelFile.absolutePath} (exists=${modelFile.exists()}, size=${modelFile.length()})")

            if (!modelFile.exists() || modelFile.length() == 0L) {
                Log.d(TAG, "Model file not found — staying Idle so UI can prompt download.")
                _state.value = LlmState.Idle
                return
            }

            _state.value = LlmState.Initializing
            try {
                Log.d(TAG, "Calling LlmService.initialize...")
                llmService.initialize(modelFile.absolutePath)
                _state.value = LlmState.Ready
                Log.d(TAG, "Model initialized successfully. State → Ready")
            } catch (e: Exception) {
                val friendlyMsg =
                    when {
                        e.message?.contains("OOM", ignoreCase = true) == true ||
                            e.message?.contains("out of memory", ignoreCase = true) == true ->
                            "Not enough RAM to load this model. Try restarting the app or device."
                        e.message?.contains("No such file", ignoreCase = true) == true ->
                            "Model file not found. Please re-download the model."
                        e.message?.contains("Incompatible", ignoreCase = true) == true ->
                            "Model format incompatible with this device. Please contact support."
                        else -> "AI engine failed to start. ${e.message ?: "Unknown error"}"
                    }
                Log.e(TAG, "Model initialization failed: ${e.message}", e)
                _state.value = LlmState.Error(friendlyMsg)
            }
        }

        override suspend fun downloadModel() =
            withContext(Dispatchers.IO) {
                // Check if already downloading OR if model is already ready/initializing
                if (_state.value is LlmState.Downloading) {
                    Log.d(TAG, "Download ignored — Model download already in progress.")
                    return@withContext
                }
                if (_state.value is LlmState.Initializing || _state.value is LlmState.Ready) {
                    Log.d(TAG, "Download ignored — Model already initializing or ready.")
                    return@withContext
                }
                _state.value = LlmState.Downloading
                _downloadProgress.value = 0f

                val destinationDir = context.getExternalFilesDir(null) ?: context.filesDir
                val modelFile = File(destinationDir, modelFileName)
                val tempFile = File(destinationDir, "$modelFileName.tmp")

                Log.d(TAG, "Download requested. Model: $modelFileName, URL: $modelUrl, isEmulator: $isEmulator")

                // Storage space check
                val usableSpace = destinationDir.usableSpace
                if (usableSpace < requiredSpaceBytes) {
                    val requiredMB = requiredSpaceBytes / (1024 * 1024)
                    val availableMB = usableSpace / (1024 * 1024)
                    val msg = "Insufficient disk space: need ${requiredMB}MB, only ${availableMB}MB free."
                    Log.e(TAG, msg)
                    _state.value = LlmState.Error(msg)
                    return@withContext
                }

                try {
                    Log.d(TAG, "Starting model download from: $modelUrl")
                    val request =
                        Request
                            .Builder()
                            .url(modelUrl)
                            .header("User-Agent", "Mozilla/5.0 MoneyPilot/1.0")
                            .build()

                    downloadClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            val errorMsg = "Server error ${response.code}: ${response.message}"
                            Log.e(TAG, errorMsg)
                            _state.value = LlmState.Error(errorMsg)
                            return@withContext
                        }

                        val body = response.body
                        val totalBytes = body.contentLength()
                        Log.d(TAG, "Content length: $totalBytes bytes")

                        tempFile.parentFile?.mkdirs()
                        if (tempFile.exists()) tempFile.delete()

                        var bytesCopied = 0L
                        val buffer = ByteArray(64 * 1024) // 64 KB
                        var lastProgressUpdate = System.currentTimeMillis()

                        body.byteStream().use { input ->
                            FileOutputStream(tempFile).use { output ->
                                var bytesRead = input.read(buffer)
                                while (bytesRead >= 0) {
                                    output.write(buffer, 0, bytesRead)
                                    bytesCopied += bytesRead

                                    if (totalBytes > 0) {
                                        val progress = bytesCopied.toFloat() / totalBytes.toFloat()
                                        val now = System.currentTimeMillis()
                                        if (now - lastProgressUpdate > 100 || progress >= 1f) {
                                            _downloadProgress.value = progress
                                            lastProgressUpdate = now
                                        }
                                    }
                                    bytesRead = input.read(buffer)
                                }
                                output.flush()
                            }
                        }

                        Log.d(TAG, "Download complete ($bytesCopied bytes). Renaming temp file...")
                        if (modelFile.exists()) modelFile.delete()

                        if (tempFile.renameTo(modelFile)) {
                            Log.d(TAG, "Rename successful. Initializing model.")
                            _downloadProgress.value = 1f
                            initialize()
                        } else {
                            Log.e(TAG, "Rename failed. Attempting manual copy...")
                            try {
                                tempFile.copyTo(modelFile, overwrite = true)
                                tempFile.delete()
                                Log.d(TAG, "Manual copy successful. Initializing model.")
                                _downloadProgress.value = 1f
                                initialize()
                            } catch (copyEx: Exception) {
                                Log.e(TAG, "Manual copy failed", copyEx)
                                _state.value = LlmState.Error("Failed to save downloaded model file.")
                            }
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Network/IO error during download", e)
                    _state.value = LlmState.Error("Network error: ${e.localizedMessage ?: "Connection reset or timeout"}")
                    if (tempFile.exists()) tempFile.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error during download", e)
                    _state.value = LlmState.Error("Error: ${e.message}")
                    if (tempFile.exists()) tempFile.delete()
                }
            }

        private suspend fun buildSystemMessage(userPrompt: String): String {
            val promptLower = userPrompt.lowercase()

            // If the prompt is simple logs, skip loading full DB contexts to save tokens
            val isLoggingOnly =
                (
                    promptLower.contains("add") ||
                        promptLower.contains("log") ||
                        promptLower.contains("spent") ||
                        promptLower.contains("earned")
                ) &&
                    !(
                        promptLower.contains("how") ||
                            promptLower.contains("show") ||
                            promptLower.contains("list") ||
                            promptLower.contains("summary") ||
                            promptLower.contains("status")
                    )

            val transactions = if (isLoggingOnly) emptyList() else moneyPilotRepository.allTransactions.first().take(3)
            val budgets = if (isLoggingOnly) emptyList() else moneyPilotRepository.allBudgets.first().take(3)
            val investments = if (isLoggingOnly) emptyList() else moneyPilotRepository.allInvestments.first().take(3)
            val loans = if (isLoggingOnly) emptyList() else moneyPilotRepository.allLoans.first().take(3)
            val categories = moneyPilotRepository.allCategories.first().associateBy { it.id }

            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())

            val expenseCategories =
                moneyPilotRepository.allCategories
                    .first()
                    .filter { it.isExpense }
                    .map { it.name }
                    .take(8)
                    .joinToString(", ")
            val incomeCategories =
                moneyPilotRepository.allCategories
                    .first()
                    .filter { !it.isExpense }
                    .map { it.name }
                    .take(4)
                    .joinToString(", ")

            return buildString {
                append("You are MoneyPilot AI. Help user track finances. Be extremely concise (1-2 sentences).\n\n")

                append("[ADDING DATA]\n")
                append("To log data, output a review request and exactly ONE action tag (no placeholders, use whole units):\n")
                append("- [ACTION:ADD_EXPENSE|amount=X|category=Y|note=Z|date=today]\n")
                append("- [ACTION:ADD_INCOME|amount=X|category=Y|note=Z|date=today]\n")
                append("- [ACTION:ADD_INVESTMENT|name=X|type=Y|amount=Z|current_value=W]\n")
                append("- [ACTION:ADD_LOAN|name=X|amount=Y|emi=Z]\n")
                append("Expense categories: $expenseCategories\n")
                append("Income categories: $incomeCategories\n")
                append("Investment types: Stock, Mutual Fund, Crypto, FD, Gold, SIP\n")
                append(
                    "Example: 'add 500 food Swiggy' -> 'Please confirm to log \u20b9500 food expense from Swiggy: [ACTION:ADD_EXPENSE|amount=500|category=Food|note=Swiggy|date=today]'\n\n",
                )

                if (transactions.isNotEmpty() || budgets.isNotEmpty() || investments.isNotEmpty() || loans.isNotEmpty()) {
                    append("[FINANCIAL DATA]\n")
                    if (transactions.isNotEmpty()) {
                        append("Recent Transactions:\n")
                        transactions.forEach { t ->
                            val catName = categories[t.categoryId]?.name ?: "Other"
                            append("- ${t.type}: \u20b9${t.amount} ($catName, ${t.note}) on ${sdf.format(java.util.Date(t.timestamp))}\n")
                        }
                    }
                    if (budgets.isNotEmpty()) {
                        append("Budgets:\n")
                        budgets.forEach { b ->
                            val catName = categories[b.categoryId]?.name ?: "Other"
                            append("- $catName: Limit \u20b9${b.amount} (${b.period})\n")
                        }
                    }
                    if (investments.isNotEmpty()) {
                        append("Investments:\n")
                        investments.forEach { i ->
                            append("- ${i.name}: Value \u20b9${i.currentValue}, Invested \u20b9${i.investedAmount}\n")
                        }
                    }
                    if (loans.isNotEmpty()) {
                        append("Loans:\n")
                        loans.forEach { l ->
                            append("- ${l.name}: Principal \u20b9${l.totalAmount}, EMI \u20b9${l.emiAmount}\n")
                        }
                    }
                }
            }
        }

        private suspend fun buildFinancialContext(userPrompt: String): String {
            val systemMessage = buildSystemMessage(userPrompt)
            return "<start_of_turn>user\n$systemMessage\n\n"
        }

        override suspend fun sendMessage(prompt: String) {
            if (_state.value !is LlmState.Ready && _state.value !is LlmState.Generating) {
                _state.value = LlmState.Error("AI not ready")
                return
            }

            try {
                // Complete the Gemma chat template:
                // <start_of_turn>user\n{system_message}\n\n{question}<end_of_turn>\n<start_of_turn>model\n
                val contextPrompt = buildFinancialContext(prompt) + prompt + "<end_of_turn>\n<start_of_turn>model\n"
                llmService.generateResponseStreaming(contextPrompt)
            } catch (e: Exception) {
                _state.value = LlmState.Error("Generation failed: ${e.message}")
            }
        }

        /**
         * Executes a confirmed AI action by writing to the Room database.
         * @return Result with a human-readable success/failure message.
         */
        override suspend fun executeAction(action: AiAction): Result<String> =
            withContext(Dispatchers.IO) {
                return@withContext try {
                    when (action) {
                        is AiAction.AddTransaction -> {
                            val categories = moneyPilotRepository.allCategories.first()
                            val categoryId =
                                fuzzyMatchCategory(
                                    name = action.categoryName,
                                    categories = categories,
                                    isExpense = action.type == TransactionType.EXPENSE,
                                )
                            val timestamp =
                                System.currentTimeMillis() +
                                    (action.dateOffset * 24 * 60 * 60 * 1000L)
                            moneyPilotRepository.insertTransaction(
                                Transaction(
                                    amount = action.amount * 100,
                                    timestamp = timestamp,
                                    categoryId = categoryId,
                                    note = action.note,
                                    type = action.type,
                                    paymentMode = "Cash",
                                    currencyCode = "INR",
                                ),
                            )
                            val typeLabel = if (action.type == TransactionType.EXPENSE) "Expense" else "Income"
                            _state.value = LlmState.Ready
                            Result.success("\u20b9${action.amount} $typeLabel added successfully!")
                        }

                        is AiAction.AddInvestment -> {
                            moneyPilotRepository.insertInvestment(
                                Investment(
                                    name = action.name,
                                    type = action.type,
                                    investedAmount = action.investedAmount * 100,
                                    currentValue = action.currentValue * 100,
                                    startDate = System.currentTimeMillis(),
                                    currencyCode = "INR",
                                ),
                            )
                            _state.value = LlmState.Ready
                            Result.success("Investment \"${action.name}\" added successfully!")
                        }

                        is AiAction.AddLoan -> {
                            val nextEmiTimestamp =
                                System.currentTimeMillis() +
                                    (action.nextEmiDays * 24 * 60 * 60 * 1000L)
                            moneyPilotRepository.insertLoan(
                                Loan(
                                    name = action.name,
                                    totalAmount = action.totalAmount * 100,
                                    outstandingAmount = action.totalAmount * 100, // starts as full amount
                                    emiAmount = action.emiAmount * 100,
                                    nextEmiDate = nextEmiTimestamp,
                                    currencyCode = "INR",
                                ),
                            )
                            _state.value = LlmState.Ready
                            Result.success("Loan \"${action.name}\" added successfully!")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "executeAction failed", e)
                    _state.value = LlmState.Error("Failed to save: ${e.message}")
                    Result.failure(e)
                }
            }

        /**
         * Fuzzy-matches a category name from the model to the closest DB category.
         * Priority: exact match (case-insensitive) > contains match > default null.
         */
        private fun fuzzyMatchCategory(
            name: String,
            categories: List<prasad.vennam.moneypilot.data.entity.Category>,
            isExpense: Boolean,
        ): Long? {
            val filtered = categories.filter { it.isExpense == isExpense }
            val nameLower = name.lowercase().trim()
            // 1. Exact match
            filtered.firstOrNull { it.name.lowercase() == nameLower }?.let { return it.id }
            // 2. Contains match (e.g. "grocery" matches "Food")
            filtered
                .firstOrNull { it.name.lowercase().contains(nameLower) || nameLower.contains(it.name.lowercase()) }
                ?.let { return it.id }
            // 3. Keyword synonyms
            val synonymMap =
                mapOf(
                    "grocery" to "Food",
                    "groceries" to "Food",
                    "restaurant" to "Food",
                    "cafe" to "Food",
                    "coffee" to "Food",
                    "swiggy" to "Food",
                    "zomato" to "Food",
                    "uber" to "Transport",
                    "ola" to "Transport",
                    "petrol" to "Transport",
                    "fuel" to "Transport",
                    "amazon" to "Shopping",
                    "flipkart" to "Shopping",
                    "clothes" to "Shopping",
                    "netflix" to "Entertainment",
                    "movie" to "Entertainment",
                    "game" to "Entertainment",
                    "hospital" to "Health",
                    "medicine" to "Health",
                    "doctor" to "Health",
                    "pharmacy" to "Health",
                    "electricity" to "Utilities",
                    "water" to "Utilities",
                    "gas" to "Utilities",
                    "internet" to "Utilities",
                    "rent" to "Housing",
                    "maintenance" to "Housing",
                    "school" to "Education",
                    "college" to "Education",
                    "course" to "Education",
                    "flight" to "Travel",
                    "hotel" to "Travel",
                    "trip" to "Travel",
                    "mutual fund" to "Investments",
                    "sip" to "Investments",
                    "stock" to "Investments",
                    "gift" to "Gifts",
                )
            val mappedName = synonymMap[nameLower]
            if (mappedName != null) {
                filtered.firstOrNull { it.name.equals(mappedName, ignoreCase = true) }?.let { return it.id }
            }
            // 4. No match — return null (uncategorized)
            Log.w(TAG, "No category match for '$name', leaving uncategorized")
            return null
        }

        override suspend fun generateShortAdvice(summary: String): String =
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                if (_state.value !is LlmState.Ready) {
                    return@withContext ""
                }
                try {
                    // Keep the prompt extremely simple and compact to prevent exceeding the context window
                    val contextPrompt =
                        "<start_of_turn>user\n" +
                            "You are MoneyPilot AI, a financial advisor. Write exactly one short, encouraging advice sentence (max 15 words) based on the user's financial stats:\n" +
                            "$summary\n" +
                            "Keep it direct and action-oriented. Do not include tags or markup.<end_of_turn>\n" +
                            "<start_of_turn>model\n"

                    val response = llmService.generateResponse(contextPrompt).trim()
                    Log.d(TAG, "AI Advice generated: $response")
                    response
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to generate short advice: ${e.message}", e)
                    ""
                }
            }

        override fun cleanup() {
            llmService.close()
            _state.value = LlmState.Idle
        }

        companion object {
            private const val TAG = "AiRepository"

            // ── Emulator / CPU-fallback model ──────────────────────────────────────────
            // Gemma 3 1B IT int4 — Gemma License.
            // Native .litertlm format, CPU+GPU compatible. ~600MB.
            // Gated-free public mirror: adiagarwal/nanochat-models
            const val EMULATOR_MODEL_FILE = "gemma3-1b-it-int4.litertlm"
            const val EMULATOR_MODEL_URL =
                "https://huggingface.co/adiagarwal/nanochat-models/resolve/main/Gemma3-1B-IT/gemma3-1b-it-int4.litertlm"

            // ── Tiny fallback (ultra-low memory, e.g. 4GB RAM emulator) ───────────────
            // Gemma 3 1B IT int4 — Gemma License. ~600MB.
            const val TINY_MODEL_FILE = "gemma3-1b-it-int4.litertlm"
            const val TINY_MODEL_URL =
                "https://huggingface.co/adiagarwal/nanochat-models/resolve/main/Gemma3-1B-IT/gemma3-1b-it-int4.litertlm"

            // ── Production / Physical device model ────────────────────────────────────
            // Gemma 3n E4B IT — 2.58GB, GPU/NPU compiled, best quality on real devices
            const val DEVICE_MODEL_FILE = "gemma-3n-E4B-it-int4.litertlm"
            const val DEVICE_MODEL_URL =
                "https://huggingface.co/adiagarwal/nanochat-models/resolve/main/Gemma-3n-E4B-it/gemma-3n-E4B-it-int4.litertlm"
        }
    }
