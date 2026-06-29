package prasad.vennam.moneypilot.feature.ai.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.entity.Loan
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.data.repository.*
import prasad.vennam.moneypilot.feature.ai.domain.AiActionParser
import prasad.vennam.moneypilot.feature.ai.domain.AiRepository
import prasad.vennam.moneypilot.feature.ai.model.AiAction
import prasad.vennam.moneypilot.feature.ai.model.LlmState
import prasad.vennam.moneypilot.feature.ai.service.LlmService
import prasad.vennam.moneypilot.util.ParsedReceipt
import java.io.File
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
        private val transactionRepository: TransactionRepository,
        private val budgetRepository: BudgetRepository,
        private val investmentRepository: InvestmentRepository,
        private val loanRepository: LoanRepository,
        private val remoteConfigHelper: prasad.vennam.moneypilot.util.RemoteConfigHelper,
    ) : AiRepository {
        private val _state = MutableStateFlow<LlmState>(LlmState.Idle)
        override val state: StateFlow<LlmState> = _state.asStateFlow()

        private val _downloadProgress = MutableStateFlow(0f)
        override val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

        private val downloadMutex = Mutex()

        internal var geminiApiKeyProvider: () -> String = { prasad.vennam.moneypilot.BuildConfig.GEMINI_API_KEY }

        private val isCloudEnabled: Boolean
            get() {
                val apiKey = geminiApiKeyProvider().trim().removeSurrounding("\"")
                return apiKey.isNotBlank()
            }

        // Emulator detection: use a small, CPU-compatible model
        private val isEmulator: Boolean by lazy {
            val fingerprint = android.os.Build.FINGERPRINT ?: ""
            val model = android.os.Build.MODEL ?: ""
            val manufacturer = android.os.Build.MANUFACTURER ?: ""
            val brand = android.os.Build.BRAND ?: ""
            val device = android.os.Build.DEVICE ?: ""
            val product = android.os.Build.PRODUCT ?: ""

            fingerprint.contains("generic") ||
                fingerprint.startsWith("unknown") ||
                model.contains("google_sdk") ||
                model.contains("sdk_gphone64_arm64") ||
                manufacturer.contains("google") ||
                (brand.startsWith("google") && device.startsWith("emu64a")) ||
                product == "sdk_gphone64_arm64"
        }

        private val modelFileName: String
            get() =
                if (isEmulator) {
                    remoteConfigHelper.getEmulatorModelFile().ifEmpty { EMULATOR_MODEL_FILE }
                } else {
                    remoteConfigHelper.getDeviceModelFile().ifEmpty { DEVICE_MODEL_FILE }
                }

        private val modelUrl: String
            get() =
                if (isEmulator) {
                    remoteConfigHelper.getEmulatorModelUrl().ifEmpty { EMULATOR_MODEL_URL }
                } else {
                    remoteConfigHelper.getDeviceModelUrl().ifEmpty { DEVICE_MODEL_URL }
                }

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

        private val workManager by lazy { androidx.work.WorkManager.getInstance(context) }

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
                            _state.value = LlmState.Ready(cleanedText)
                        }
                    }
                }.launchIn(kotlinx.coroutines.GlobalScope)

            // Listen to background model download updates via WorkManager
            workManager
                .getWorkInfosForUniqueWorkFlow("llm_model_download_work")
                .onEach { workInfos ->
                    val workInfo = workInfos.firstOrNull() ?: return@onEach
                    when (workInfo.state) {
                        androidx.work.WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress.getFloat("progress", 0f)
                            _state.value = LlmState.Downloading
                            _downloadProgress.value = progress
                        }
                        androidx.work.WorkInfo.State.SUCCEEDED -> {
                            if (_state.value is LlmState.Downloading || _state.value is LlmState.Idle || _state.value is LlmState.Error) {
                                Log.d(TAG, "Background download succeeded. Initializing model...")
                                _downloadProgress.value = 1f
                                _state.value = LlmState.Idle // Reset downloading state to allow initialization
                                initialize()
                            }
                        }
                        androidx.work.WorkInfo.State.FAILED -> {
                            if (_state.value is LlmState.Downloading) {
                                Log.e(TAG, "Background download failed.")
                                if (isCloudEnabled) {
                                    _state.value = LlmState.Ready()
                                } else {
                                    _state.value = LlmState.Error("Background model download failed. Please try again.")
                                }
                            }
                        }
                        androidx.work.WorkInfo.State.CANCELLED -> {
                            if (_state.value is LlmState.Downloading) {
                                Log.w(TAG, "Background download cancelled.")
                                if (isCloudEnabled) {
                                    _state.value = LlmState.Ready()
                                } else {
                                    _state.value = LlmState.Idle
                                }
                            }
                        }
                        else -> {
                            // ENQUEUED, BLOCKED, etc.
                            if (_state.value is LlmState.Idle) {
                                _state.value = LlmState.Downloading
                            }
                        }
                    }
                }.launchIn(kotlinx.coroutines.GlobalScope)
        }

        private fun getModelFile(): File {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            return File(dir, modelFileName)
        }

        override suspend fun initialize() {
            if (_state.value is LlmState.Ready || _state.value is LlmState.Initializing || _state.value is LlmState.Downloading) return

            val modelFile = getModelFile()
            Log.d(TAG, "Initializing with model: ${modelFile.absolutePath} (exists=${modelFile.exists()}, size=${modelFile.length()})")

            if (!modelFile.exists() || modelFile.length() == 0L) {
                if (isCloudEnabled) {
                    Log.d(TAG, "Model file not found, but cloud is enabled. Transitioning to Ready.")
                    _state.value = LlmState.Ready()
                } else {
                    Log.d(TAG, "Model file not found — staying Idle so UI can prompt download.")
                    _state.value = LlmState.Idle
                }
                return
            }

            _state.value = LlmState.Initializing
            try {
                Log.d(TAG, "Calling LlmService.initialize...")
                llmService.initialize(modelFile.absolutePath)
                _state.value = LlmState.Ready()
                Log.d(TAG, "Model initialized successfully. State → Ready")
            } catch (e: Exception) {
                Log.e(TAG, "Model initialization failed: ${e.message}", e)
                if (isCloudEnabled) {
                    Log.w(TAG, "Local model initialization failed, but cloud is enabled. Transitioning to Ready.")
                    _state.value = LlmState.Ready()
                } else {
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
                    _state.value = LlmState.Error(friendlyMsg)
                }
            }
        }

        override suspend fun downloadModel() =
            withContext(Dispatchers.IO) {
                // Check if already downloading, ready, or if model already exists on disk
                val shouldInitialize =
                    downloadMutex.withLock {
                        if (_state.value is LlmState.Downloading) {
                            Log.d(TAG, "Download ignored — Model download already in progress.")
                            return@withContext
                        }
                        if (_state.value is LlmState.Initializing || (_state.value is LlmState.Ready && getModelFile().exists() && getModelFile().length() > 0L)) {
                            Log.d(TAG, "Download ignored — Model already initializing or ready locally.")
                            return@withContext
                        }

                        val destinationDir = context.getExternalFilesDir(null) ?: context.filesDir
                        val modelFile = File(destinationDir, modelFileName)
                        if (modelFile.exists() && modelFile.length() > 0L) {
                            Log.d(TAG, "Download ignored — Model file already exists on disk. Initializing...")
                            true
                        } else {
                            _state.value = LlmState.Downloading
                            _downloadProgress.value = 0f
                            false
                        }
                    }

                if (shouldInitialize) {
                    initialize()
                    return@withContext
                }

                val destinationDir = context.getExternalFilesDir(null) ?: context.filesDir

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

                // Enqueue background download using WorkManager
                try {
                    val workRequest =
                        androidx.work
                            .OneTimeWorkRequestBuilder<prasad.vennam.moneypilot.worker.ModelDownloadWorker>()
                            .setInputData(
                                androidx.work.workDataOf(
                                    "model_url" to modelUrl,
                                    "model_file_name" to modelFileName,
                                ),
                            ).setConstraints(
                                androidx.work.Constraints
                                    .Builder()
                                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                    .build(),
                            ).build()

                    workManager.enqueueUniqueWork(
                        "llm_model_download_work",
                        androidx.work.ExistingWorkPolicy.KEEP,
                        workRequest,
                    )
                    Log.d(TAG, "Model download enqueued via WorkManager successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to enqueue download via WorkManager: ${e.message}", e)
                    _state.value = LlmState.Error("Failed to start background download.")
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

            val transactions = if (isLoggingOnly) emptyList() else transactionRepository.allTransactions.first().take(3)
            val budgets = if (isLoggingOnly) emptyList() else budgetRepository.allBudgets.first().take(3)
            val investments = if (isLoggingOnly) emptyList() else investmentRepository.allInvestments.first().take(3)
            val loans = if (isLoggingOnly) emptyList() else loanRepository.allLoans.first().take(3)
            val categories = transactionRepository.allCategories.first().associateBy { it.id }

            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())

            val expenseCategories =
                transactionRepository.allCategories
                    .first()
                    .filter { it.isExpense }
                    .map { it.name }
                    .take(8)
                    .joinToString(", ")
            val incomeCategories =
                transactionRepository.allCategories
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
            val isReady = _state.value is LlmState.Ready || _state.value is LlmState.Generating

            if (!isReady && !isCloudEnabled) {
                _state.value = LlmState.Error("AI not ready and no API Key configured.")
                return
            }

            try {
                val contextPrompt = buildFinancialContext(prompt) + prompt + "<end_of_turn>\n<start_of_turn>model\n"
                if (isReady && getModelFile().exists()) {
                    llmService.generateResponseStreaming(contextPrompt)
                } else {
                    llmService.generateCloudResponseStreaming(contextPrompt)
                }
            } catch (e: Exception) {
                _state.value = LlmState.Error("Generation failed: ${e.message}")
            }
        }

        override suspend fun executeAction(action: AiAction): Result<String> =
            withContext(Dispatchers.IO) {
                return@withContext try {
                    when (action) {
                        is AiAction.AddTransaction -> {
                            val categories = transactionRepository.allCategories.first()
                            val categoryId =
                                fuzzyMatchCategory(
                                    name = action.categoryName,
                                    categories = categories,
                                    isExpense = action.type == TransactionType.EXPENSE,
                                )
                            val timestamp =
                                System.currentTimeMillis() +
                                    (action.dateOffset * 24 * 60 * 60 * 1000L)
                            transactionRepository.insertTransaction(
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
                            _state.value = LlmState.Ready()
                            Result.success("\u20b9${action.amount} $typeLabel added successfully!")
                        }

                        is AiAction.AddInvestment -> {
                            investmentRepository.insertInvestment(
                                Investment(
                                    name = action.name,
                                    type = action.type,
                                    investedAmount = action.investedAmount * 100,
                                    currentValue = action.currentValue * 100,
                                    startDate = System.currentTimeMillis(),
                                    currencyCode = "INR",
                                ),
                            )
                            _state.value = LlmState.Ready()
                            Result.success("Investment \"${action.name}\" added successfully!")
                        }

                        is AiAction.AddLoan -> {
                            val nextEmiTimestamp =
                                System.currentTimeMillis() +
                                    (action.nextEmiDays * 24 * 60 * 60 * 1000L)
                            loanRepository.insertLoan(
                                Loan(
                                    name = action.name,
                                    totalAmount = action.totalAmount * 100,
                                    outstandingAmount = action.totalAmount * 100,
                                    emiAmount = action.emiAmount * 100,
                                    nextEmiDate = nextEmiTimestamp,
                                    currencyCode = "INR",
                                ),
                            )
                            _state.value = LlmState.Ready()
                            Result.success("Loan \"${action.name}\" added successfully!")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "executeAction failed", e)
                    _state.value = LlmState.Error("Failed to save: ${e.message}")
                    Result.failure(e)
                }
            }

        private fun fuzzyMatchCategory(
            name: String,
            categories: List<prasad.vennam.moneypilot.data.entity.Category>,
            isExpense: Boolean,
        ): Long? {
            val filtered = categories.filter { it.isExpense == isExpense }
            val nameLower = name.lowercase().trim()
            filtered.firstOrNull { it.name.lowercase() == nameLower }?.let { return it.id }
            filtered
                .firstOrNull { it.name.lowercase().contains(nameLower) || nameLower.contains(it.name.lowercase()) }
                ?.let { return it.id }
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
            Log.w(TAG, "No category match for '$name', leaving uncategorized")
            return null
        }

        override suspend fun generateShortAdvice(summary: String): String =
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                val isReady = _state.value is LlmState.Ready

                if (!isReady && !isCloudEnabled) {
                    return@withContext ""
                }
                try {
                    val contextPrompt =
                        "<start_of_turn>user\n" +
                            "You are MoneyPilot AI, a financial advisor. Write exactly one short, encouraging advice sentence (max 15 words) based on the user's financial stats:\n" +
                            "$summary\n" +
                            "Keep it direct and action-oriented. Do not include tags or markup.<end_of_turn>\n" +
                            "<start_of_turn>model\n"

                    val response =
                        if (isReady) {
                            llmService.generateResponse(contextPrompt).trim()
                        } else {
                            llmService.generateCloudResponse(contextPrompt)?.trim() ?: ""
                        }
                    Log.d(TAG, "AI Advice generated: $response")
                    response
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to generate short advice: ${e.message}", e)
                    ""
                }
            }

        override suspend fun parseReceiptText(ocrText: String): ParsedReceipt? =
            withContext(Dispatchers.Default) {
                val isReady = _state.value is LlmState.Ready

                if (!isReady && !isCloudEnabled) {
                    Log.d(TAG, "parseReceiptText ignored: LLM is not ready and no Cloud Fallback key available.")
                    return@withContext null
                }
                try {
                    val prompt =
                        buildString {
                            append("<start_of_turn>user\n")
                            append("Analyze the following OCR text from a transaction receipt and extract:\n")
                            append("1. The merchant name (e.g. Starbucks, Walmart, Swiggy).\n")
                            append("2. The total transaction amount paid as a numeric value.\n")
                            append("Format your response as an action tag with NO other text or explanation:\n")
                            append("[ACTION:ADD_EXPENSE|amount=VALUE|category=Other|note=MERCHANT_NAME|date=today]\n\n")
                            append("OCR Text:\n")
                            append(ocrText)
                            append("<end_of_turn>\n<start_of_turn>model\n")
                        }
                    val response =
                        if (isReady) {
                            Log.d(TAG, "Running parseReceiptText locally via LiteRT")
                            llmService.generateResponse(prompt).trim()
                        } else {
                            Log.d(TAG, "Running parseReceiptText via Cloud Fallback")
                            llmService.generateCloudResponse(prompt)?.trim()
                        }

                    if (response.isNullOrEmpty()) {
                        Log.w(TAG, "Received empty response from LLM")
                        return@withContext null
                    }

                    Log.d(TAG, "OCR LLM Response: $response")
                    val (action, _) = AiActionParser.parse(response)
                    if (action is AiAction.AddTransaction) {
                        val finalDate = System.currentTimeMillis() + (action.dateOffset * 24 * 60 * 60 * 1000L)
                        return@withContext ParsedReceipt(
                            merchant = action.note.trim().ifBlank { null },
                            amount = action.amount.toDouble(),
                            date = finalDate,
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse receipt text using LLM: ${e.message}", e)
                }
                return@withContext null
            }

        override fun cleanup() {
            llmService.close()
            _state.value = LlmState.Idle
        }

        companion object {
            private const val TAG = "AiRepository"
            const val EMULATOR_MODEL_FILE = "gemma3-1b-it-int4.litertlm"
            const val EMULATOR_MODEL_URL =
                "https://huggingface.co/adiagarwal/nanochat-models/resolve/main/Gemma3-1B-IT/gemma3-1b-it-int4.litertlm"
            const val TINY_MODEL_FILE = "gemma3-1b-it-int4.litertlm"
            const val TINY_MODEL_URL =
                "https://huggingface.co/adiagarwal/nanochat-models/resolve/main/Gemma3-1B-IT/gemma3-1b-it-int4.litertlm"
            const val DEVICE_MODEL_FILE = "gemma-3n-E4B-it-int4.litertlm"
            const val DEVICE_MODEL_URL =
                "https://huggingface.co/adiagarwal/nanochat-models/resolve/main/Gemma-3n-E4B-it/gemma-3n-E4B-it-int4.litertlm"
        }
    }
