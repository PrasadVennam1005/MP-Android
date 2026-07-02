package prasad.vennam.moneypilot.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.dao.*
import prasad.vennam.moneypilot.data.entity.*
import prasad.vennam.moneypilot.data.repository.DataManagementRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class SyncResult {
    object Success : SyncResult()

    data class NeedAuthorization(
        val intent: Intent,
    ) : SyncResult()

    data class Error(
        val exception: Exception,
    ) : SyncResult()

    object NoBackupFound : SyncResult()
}

class SheetStructureBrokenException(
    message: String,
) : Exception(message)

object GoogleSheetsSyncHelper {
    private val client = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    const val SYNC_WORK_NAME = "GoogleSheetsSyncWork"

    suspend fun performTwoWaySync(
        context: Context,
        email: String,
        repository: DataManagementRepository,
        analyticsHelper: AnalyticsHelper,
        userPreferences: UserPreferences,
        spreadsheetId: String?,
        isRestore: Boolean = false,
        onSpreadsheetIdFound: suspend (String) -> Unit,
    ): SyncResult =
        withContext(Dispatchers.IO) {
            Log.d(
                "GoogleSheetsSyncHelper",
                "performTwoWaySync: Start for email=$email, spreadsheetId=$spreadsheetId, isRestore=$isRestore",
            )
            analyticsHelper.logEvent(AnalyticsConstants.Event.SYNC_STARTED)
            try {
                if (isRestore) {
                    userPreferences.clearDeletedTransactionIds()
                }
                val account = android.accounts.Account(email, "com.google")
                val scopeString = "oauth2:https://www.googleapis.com/auth/spreadsheets https://www.googleapis.com/auth/drive.metadata.readonly"
                val token = GoogleAuthUtil.getToken(context, account, scopeString)
                Log.d(
                    "GoogleSheetsSyncHelper",
                    "performTwoWaySync: Google OAuth token retrieved successfully",
                )

                var currentSpreadsheetId = spreadsheetId

                if (currentSpreadsheetId.isNullOrEmpty()) {
                    Log.d(
                        "GoogleSheetsSyncHelper",
                        "performTwoWaySync: spreadsheetId is null/empty, searching Drive...",
                    )
                    currentSpreadsheetId = findExistingSpreadsheet(token)
                    if (currentSpreadsheetId != null) {
                        Log.d(
                            "GoogleSheetsSyncHelper",
                            "performTwoWaySync: Found existing spreadsheet in Drive: $currentSpreadsheetId",
                        )
                        onSpreadsheetIdFound(currentSpreadsheetId)
                    } else {
                        Log.d(
                            "GoogleSheetsSyncHelper",
                            "performTwoWaySync: No existing spreadsheet found. Creating a new one...",
                        )
                        currentSpreadsheetId = createSpreadsheet(token)
                        Log.d(
                            "GoogleSheetsSyncHelper",
                            "performTwoWaySync: Created new spreadsheet with ID: $currentSpreadsheetId",
                        )
                        onSpreadsheetIdFound(currentSpreadsheetId)
                    }
                } else {
                    Log.d(
                        "GoogleSheetsSyncHelper",
                        "performTwoWaySync: Using current spreadsheet ID: $currentSpreadsheetId",
                    )
                }

                try {
                    Log.d(
                        "GoogleSheetsSyncHelper",
                        "performTwoWaySync: Verifying required sheets exist in $currentSpreadsheetId...",
                    )
                    ensureRequiredSheetsExist(token, currentSpreadsheetId)
                    Log.d(
                        "GoogleSheetsSyncHelper",
                        "performTwoWaySync: Running Two-Way Sync sequence. Downloading cloud data first...",
                    )
                    val valueRanges = downloadSpreadsheetData(token, currentSpreadsheetId)
                    if (valueRanges != null) {
                        Log.d(
                            "GoogleSheetsSyncHelper",
                            "performTwoWaySync: Cloud data downloaded. Merging into local database...",
                        )
                        mergeCloudDataIntoLocal(
                            categoryDao = repository.categoryDao,
                            budgetDao = repository.budgetDao,
                            investmentDao = repository.investmentDao,
                            transactionDao = repository.transactionDao,
                            emergencyFundDao = repository.emergencyFundDao,
                            subscriptionDao = repository.subscriptionDao,
                            savingGoalDao = repository.savingGoalDao,
                            loanDao = repository.loanDao,
                            loanPaymentDao = repository.loanPaymentDao,
                            userPreferences = userPreferences,
                            valueRanges = valueRanges,
                        )
                    } else {
                        Log.w(
                            "GoogleSheetsSyncHelper",
                            "performTwoWaySync: Downloaded valueRanges is null",
                        )
                    }
                    Log.d(
                        "GoogleSheetsSyncHelper",
                        "performTwoWaySync: Uploading local data to sync database on cloud...",
                    )
                    uploadLocalDataToSpreadsheet(
                        token = token,
                        spreadsheetId = currentSpreadsheetId,
                        categoryDao = repository.categoryDao,
                        budgetDao = repository.budgetDao,
                        investmentDao = repository.investmentDao,
                        transactionDao = repository.transactionDao,
                        emergencyFundDao = repository.emergencyFundDao,
                        subscriptionDao = repository.subscriptionDao,
                        savingGoalDao = repository.savingGoalDao,
                        loanDao = repository.loanDao,
                        loanPaymentDao = repository.loanPaymentDao,
                    )
                    Log.d(
                        "GoogleSheetsSyncHelper",
                        "performTwoWaySync: Sync sequence successfully completed",
                    )
                    userPreferences.clearDeletedTransactionIds()
                    analyticsHelper.logEvent(AnalyticsConstants.Event.SYNC_SUCCESS)
                } catch (e: SheetStructureBrokenException) {
                    analyticsHelper.logEvent(AnalyticsConstants.Event.SYNC_FAILURE)
                    Log.w(
                        "GoogleSheetsSyncHelper",
                        "performTwoWaySync: SheetStructureBrokenException caught. Recreating spreadsheet...",
                        e,
                    )
                    val newId = createSpreadsheet(token)
                    onSpreadsheetIdFound(newId)
                    Log.d(
                        "GoogleSheetsSyncHelper",
                        "performTwoWaySync: Recreated spreadsheet with ID: $newId. Uploading local data...",
                    )
                    uploadLocalDataToSpreadsheet(
                        token = token,
                        spreadsheetId = newId,
                        categoryDao = repository.categoryDao,
                        budgetDao = repository.budgetDao,
                        investmentDao = repository.investmentDao,
                        transactionDao = repository.transactionDao,
                        emergencyFundDao = repository.emergencyFundDao,
                        subscriptionDao = repository.subscriptionDao,
                        savingGoalDao = repository.savingGoalDao,
                        loanDao = repository.loanDao,
                        loanPaymentDao = repository.loanPaymentDao,
                    )
                    userPreferences.clearDeletedTransactionIds()
                }

                SyncResult.Success
            } catch (e: UserRecoverableAuthException) {
                Log.w(
                    "GoogleSheetsSyncHelper",
                    "performTwoWaySync: UserRecoverableAuthException caught. Needs user approval.",
                )
                SyncResult.NeedAuthorization(e.intent ?: Intent())
            } catch (e: Exception) {
                Log.e("GoogleSheetsSyncHelper", "Sync Exception in performTwoWaySync", e)
                SyncResult.Error(e)
            }
        }

    private fun findExistingSpreadsheet(token: String): String? {
        val query = "mimeType='application/vnd.google-apps.spreadsheet' and name='MoneyPilot Backup' and trashed=false"
        val request =
            Request
                .Builder()
                .url("https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&spaces=drive")
                .addHeader("Authorization", "Bearer $token")
                .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body.string()
            if (!response.isSuccessful) return null
            if (bodyStr.isEmpty()) return null

            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val driveResponse = moshi.adapter(DriveFilesResponse::class.java).fromJson(bodyStr)
            return driveResponse?.files?.firstOrNull()?.id
        }
    }

    private fun createSpreadsheet(token: String): String {
        val createBody =
            """
            {
              "properties": { "title": "MoneyPilot Backup" },
              "sheets": [
                { "properties": { "title": "Transactions" } },
                { "properties": { "title": "Categories" } },
                { "properties": { "title": "Budgets" } },
                { "properties": { "title": "Investments" } },
                { "properties": { "title": "EmergencyFund" } }
              ]
            }
            """.trimIndent()
        val request =
            Request
                .Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets")
                .post(createBody.toRequestBody(mediaType))
                .addHeader("Authorization", "Bearer $token")
                .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body.string()
            if (!response.isSuccessful) throw Exception("Failed to create spreadsheet")
            val match = "\"spreadsheetId\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(bodyStr)
            return match?.groupValues?.get(1) ?: throw Exception("Could not parse spreadsheetId")
        }
    }

    private fun downloadSpreadsheetData(
        token: String,
        spreadsheetId: String,
    ): List<ValueRange>? {
        Log.d(
            "GoogleSheetsSyncHelper",
            "downloadSpreadsheetData: Downloading values from spreadsheetId=$spreadsheetId",
        )
        val url =
            "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values:batchGet" +
                "?ranges=Transactions!A2:Z10000" +
                "&ranges=Categories!A2:Z10000" +
                "&ranges=Budgets!A2:Z10000" +
                "&ranges=Investments!A2:Z10000" +
                "&ranges=EmergencyFund!A2:Z10000" +
                "&ranges=Subscriptions!A2:Z10000" +
                "&ranges=SavingGoals!A2:Z10000" +
                "&ranges=Loans!A2:Z10000" +
                "&ranges=LoanPayments!A2:Z10000"

        val request =
            Request
                .Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()

        client.newCall(request).execute().use { response ->
            Log.d(
                "GoogleSheetsSyncHelper",
                "downloadSpreadsheetData: Response code=${response.code}, message=${response.message}",
            )
            if (response.code == 400 || response.code == 404) {
                Log.w(
                    "GoogleSheetsSyncHelper",
                    "downloadSpreadsheetData: Format invalid or spreadsheet missing (throwing SheetStructureBrokenException)",
                )
                throw SheetStructureBrokenException("Spreadsheet format invalid")
            }
            if (!response.isSuccessful) {
                Log.e(
                    "GoogleSheetsSyncHelper",
                    "downloadSpreadsheetData: Request failed with code=${response.code}",
                )
                throw Exception("Failed to fetch values")
            }
            val bodyStr = response.body.string()
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val batchResponse = moshi.adapter(BatchGetSpreadsheetResponse::class.java).fromJson(bodyStr)
            val ranges = batchResponse?.valueRanges
            Log.d(
                "GoogleSheetsSyncHelper",
                "downloadSpreadsheetData: Successfully parsed ${ranges?.size ?: 0} ranges from sheet",
            )
            return ranges
        }
    }

    internal suspend fun mergeCloudDataIntoLocal(
        categoryDao: CategoryDao,
        budgetDao: BudgetDao,
        investmentDao: InvestmentDao,
        transactionDao: TransactionDao,
        emergencyFundDao: EmergencyFundDao,
        subscriptionDao: SubscriptionDao,
        savingGoalDao: SavingGoalDao,
        loanDao: LoanDao,
        loanPaymentDao: LoanPaymentDao,
        userPreferences: UserPreferences,
        valueRanges: List<ValueRange>,
    ) {
        val order = listOf(
            "Categories",
            "Loans",
            "Budgets",
            "Investments",
            "EmergencyFund",
            "Subscriptions",
            "SavingGoals",
            "Transactions",
            "LoanPayments"
        )

        val sortedValueRanges = valueRanges.sortedBy { vr ->
            val rangeName = vr.range.orEmpty()
            val idx = order.indexOfFirst { rangeName.contains(it) }
            if (idx != -1) idx else order.size
        }

        for (vr in sortedValueRanges) {
            val range = vr.range ?: continue
            val values = vr.values ?: continue

            if (range.contains("Categories")) {
                Log.d(
                    "GoogleSheetsSyncHelper",
                    "mergeCloudDataIntoLocal: Parsing ${values.size} categories...",
                )
                val localCategories: Map<Long, Category> = categoryDao.getAllCategoriesSync().associateBy { it.id }
                for (row in values) {
                    val idStr = row.getOrNull(0)?.toString().orEmpty()
                    val id = idStr.toDoubleOrNull()?.toLong() ?: 0L
                    val name = row.getOrNull(1)?.toString().orEmpty()
                    val iconName = row.getOrNull(2)?.toString().orEmpty()
                    val color =
                        row
                            .getOrNull(3)
                            ?.toString()
                            ?.toDoubleOrNull()
                            ?.toLong() ?: 0L
                    val isExpense = row.getOrNull(4)?.toString()?.toBoolean() ?: true
                    val lastUpdated =
                        row
                            .getOrNull(5)
                            ?.toString()
                            ?.toDoubleOrNull()
                            ?.toLong() ?: 0L
                    if (name.isNotEmpty()) {
                        val localCat = localCategories[id]
                        if (localCat == null || lastUpdated > localCat.lastUpdated) {
                            categoryDao.insertCategory(
                                Category(
                                    id = id,
                                    name = name,
                                    iconName = iconName,
                                    color = color,
                                    isExpense = isExpense,
                                    lastUpdated = lastUpdated,
                                ),
                            )
                        }
                    }
                }
            } else if (range.contains("Budgets")) {
                Log.d(
                    "GoogleSheetsSyncHelper",
                    "mergeCloudDataIntoLocal: Parsing ${values.size} budgets...",
                )
                val localBudgets: Map<Long, Budget> = budgetDao.getAllBudgetsSync().associateBy { it.id }
                for (row in values) {
                    val idStr = row.getOrNull(0)?.toString().orEmpty()
                    val id = idStr.toDoubleOrNull()?.toLong() ?: 0L
                    val categoryId =
                        row
                            .getOrNull(1)
                            ?.toString()
                            ?.toDoubleOrNull()
                            ?.toLong() ?: continue
                    val amount = row.getOrNull(2)?.toString()?.toDoubleOrNull() ?: 0.0
                    val period = row.getOrNull(3)?.toString().orEmpty()
                    val currencyCode = row.getOrNull(4)?.toString().takeIf { !it.isNullOrBlank() } ?: "INR"
                    val lastUpdated =
                        row
                            .getOrNull(5)
                            ?.toString()
                            ?.toDoubleOrNull()
                            ?.toLong() ?: 0L
                    val localB = localBudgets[id]
                    if (localB == null || lastUpdated > localB.lastUpdated) {
                        budgetDao.insertBudget(
                            Budget(
                                id = id,
                                categoryId = categoryId,
                                amount = (amount * 100).toLong(),
                                period = period,
                                currencyCode = currencyCode,
                                lastUpdated = lastUpdated,
                            ),
                        )
                    }
                }
            } else if (range.contains("Investments")) {
                Log.d(
                    "GoogleSheetsSyncHelper",
                    "mergeCloudDataIntoLocal: Parsing ${values.size} investments...",
                )
                val localInvestments: Map<Long, Investment> = investmentDao.getAllInvestmentsSync().associateBy { it.id }
                for (row in values) {
                    val idStr = row.getOrNull(0)?.toString().orEmpty()
                    val id = idStr.toDoubleOrNull()?.toLong() ?: 0L
                    val name = row.getOrNull(1)?.toString().orEmpty()
                    val type = row.getOrNull(2)?.toString().orEmpty()
                    val investedAmount = row.getOrNull(3)?.toString()?.toDoubleOrNull() ?: 0.0
                    val currentValue = row.getOrNull(4)?.toString()?.toDoubleOrNull() ?: 0.0
                    val currencyCode = row.getOrNull(5)?.toString().takeIf { !it.isNullOrBlank() } ?: "INR"

                    val sheetSymbol = row.getOrNull(6)?.toString().takeIf { !it.isNullOrBlank() }
                    val sheetQuantity = row.getOrNull(7)?.toString()?.toDoubleOrNull()
                    val sheetInterestRate = row.getOrNull(8)?.toString()?.toDoubleOrNull()
                    val sheetStartDate =
                        row
                            .getOrNull(9)
                            ?.toString()
                            ?.toDoubleOrNull()
                            ?.toLong()
                    val lastUpdated =
                        row
                            .getOrNull(10)
                            ?.toString()
                            ?.toDoubleOrNull()
                            ?.toLong() ?: 0L

                    val localInv = localInvestments[id]
                    val symbol = sheetSymbol ?: localInv?.symbol
                    val quantity = sheetQuantity ?: localInv?.quantity
                    val interestRate = sheetInterestRate ?: localInv?.interestRate
                    val startDate = sheetStartDate ?: localInv?.startDate

                    if (name.isNotEmpty()) {
                        if (localInv == null || lastUpdated > localInv.lastUpdated) {
                            investmentDao.insertInvestment(
                                Investment(
                                    id = id,
                                    name = name,
                                    type = type,
                                    investedAmount = (investedAmount * 100).toLong(),
                                    currentValue = (currentValue * 100).toLong(),
                                    currencyCode = currencyCode,
                                    symbol = symbol,
                                    quantity = quantity,
                                    interestRate = interestRate,
                                    startDate = startDate,
                                    lastUpdated = lastUpdated,
                                ),
                            )
                        }
                    }
                }
            } else if (range.contains("Transactions")) {
                Log.d(
                    "GoogleSheetsSyncHelper",
                    "mergeCloudDataIntoLocal: Parsing ${values.size} transactions...",
                )
                val deletedIds = userPreferences.deletedTransactionIds.first()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val localTransactions: Map<Long, Transaction> = transactionDao.getAllTransactionsSync().associateBy { it.id }
                for (row in values) {
                    val idStr = row.getOrNull(0)?.toString().orEmpty()
                    val id = idStr.toDoubleOrNull()?.toLong() ?: 0L
                    
                    if (deletedIds.contains(id.toString())) {
                        Log.d("GoogleSheetsSyncHelper", "mergeCloudDataIntoLocal: Skipping deleted transaction ID: $id")
                        continue
                    }
                    val dateStr = row.getOrNull(1)?.toString().orEmpty()
                    val typeStr = row.getOrNull(2)?.toString().orEmpty()
                    val categoryId =
                        row
                            .getOrNull(3)
                            ?.toString()
                            ?.toDoubleOrNull()
                            ?.toLong()
                    val paymentMode = row.getOrNull(4)?.toString().orEmpty()
                    val amount = row.getOrNull(5)?.toString()?.toDoubleOrNull() ?: 0.0
                    val note = row.getOrNull(6)?.toString().orEmpty()
                    val currencyCode = row.getOrNull(7)?.toString().takeIf { !it.isNullOrBlank() } ?: "INR"

                    val sheetSubCategory = row.getOrNull(8)?.toString().orEmpty()
                    val lastUpdated =
                        row
                            .getOrNull(9)
                            ?.toString()
                            ?.toDoubleOrNull()
                            ?.toLong() ?: 0L
                    val localTrans = localTransactions[id]
                    val subCategory = if (row.size > 8) sheetSubCategory else (localTrans?.subCategory ?: "")

                    val timestamp =
                        try {
                            dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                    val type =
                        try {
                            TransactionType.valueOf(typeStr)
                        } catch (e: Exception) {
                            TransactionType.EXPENSE
                        }

                    if (localTrans == null || lastUpdated > localTrans.lastUpdated) {
                        transactionDao.insertTransaction(
                            Transaction(
                                id = id,
                                amount = (amount * 100).toLong(),
                                timestamp = timestamp,
                                categoryId = categoryId,
                                paymentMode = paymentMode,
                                note = note,
                                type = type,
                                currencyCode = currencyCode,
                                subCategory = subCategory,
                                lastUpdated = lastUpdated,
                            ),
                        )
                    }
                }
            } else if (range.contains("EmergencyFund")) {
                val row = values.getOrNull(0)
                Log.d(
                    "GoogleSheetsSyncHelper",
                    "mergeCloudDataIntoLocal: Parsing Emergency Fund config row: $row",
                )
                if (row != null) {
                    val monthly = row.getOrNull(0)?.toString()?.toDoubleOrNull() ?: 0.0
                    val months =
                        row
                            .getOrNull(1)
                            ?.toString()
                            ?.toDoubleOrNull()
                            ?.toInt() ?: 6
                    val saved = row.getOrNull(2)?.toString()?.toDoubleOrNull() ?: 0.0
                    if (monthly > 0.0) {
                        Log.d(
                            "GoogleSheetsSyncHelper",
                            "mergeCloudDataIntoLocal: Saving Emergency Fund to DB: monthlyExpenses=$monthly, targetMonths=$months, currentSaved=$saved",
                        )
                        emergencyFundDao.insertEmergencyFund(
                            EmergencyFund(
                                id = 1,
                                monthlyExpenses = monthly,
                                targetMonths = months,
                                currentSaved = saved,
                            ),
                        )
                    } else {
                        Log.w(
                            "GoogleSheetsSyncHelper",
                            "mergeCloudDataIntoLocal: monthlyExpenses is 0.0 or invalid: monthly=$monthly",
                        )
                    }
                } else {
                    Log.w(
                        "GoogleSheetsSyncHelper",
                        "mergeCloudDataIntoLocal: EmergencyFund sheet values row is empty/null",
                    )
                }

            } else if (range.contains("Subscriptions")) {
                Log.d("GoogleSheetsSyncHelper", "mergeCloudDataIntoLocal: Parsing ${values.size} subscriptions...")
                val localSubscriptions = subscriptionDao.getAllSubscriptionsSync().associateBy { it.id }
                for (row in values) {
                    try {
                        val id = row.getOrNull(0)?.toString()?.toDoubleOrNull()?.toLong() ?: continue
                        val name = row.getOrNull(1)?.toString() ?: continue
                        val amount = row.getOrNull(2)?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                        val billingCycle = row.getOrNull(3)?.toString() ?: "Monthly"
                        val nextPaymentDate = row.getOrNull(4)?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                        val paymentMode = row.getOrNull(5)?.toString() ?: "UPI"
                        val categoryId = row.getOrNull(6)?.toString()?.toDoubleOrNull()?.toLong()
                        val isNotificationEnabled = row.getOrNull(7)?.toString()?.toBoolean() ?: true
                        val lastUpdated = row.getOrNull(8)?.toString()?.toDoubleOrNull()?.toLong() ?: System.currentTimeMillis()

                        val localSub = localSubscriptions[id]
                        if (localSub == null || lastUpdated > localSub.lastUpdated) {
                            subscriptionDao.insertSubscription(
                                prasad.vennam.moneypilot.data.entity.Subscription(
                                    id = id,
                                    name = name,
                                    amount = amount,
                                    billingCycle = billingCycle,
                                    nextPaymentDate = nextPaymentDate,
                                    paymentMode = paymentMode,
                                    categoryId = categoryId,
                                    isNotificationEnabled = isNotificationEnabled,
                                    lastUpdated = lastUpdated
                                )
                            )
                        }
                    } catch (e: Exception) { Log.w("GoogleSheetsSyncHelper", "Failed to parse Subscription row: $row", e) }
                }
            } else if (range.contains("SavingGoals")) {
                Log.d("GoogleSheetsSyncHelper", "mergeCloudDataIntoLocal: Parsing ${values.size} saving goals...")
                val localGoals = savingGoalDao.getAllSavingGoalsSync().associateBy { it.id }
                for (row in values) {
                    try {
                        val id = row.getOrNull(0)?.toString()?.toDoubleOrNull()?.toLong() ?: continue
                        val name = row.getOrNull(1)?.toString() ?: continue
                        val targetAmount = row.getOrNull(2)?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                        val currentSavedAmount = row.getOrNull(3)?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                        val deadline = row.getOrNull(4)?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                        val colorHex = row.getOrNull(5)?.toString() ?: "#3F51B5"
                        val iconName = row.getOrNull(6)?.toString() ?: "Savings"
                        val isCompleted = row.getOrNull(7)?.toString()?.toBoolean() ?: false
                        val lastUpdated = row.getOrNull(8)?.toString()?.toDoubleOrNull()?.toLong() ?: System.currentTimeMillis()

                        val localGoal = localGoals[id]
                        if (localGoal == null || lastUpdated > localGoal.lastUpdated) {
                            savingGoalDao.insertSavingGoal(
                                prasad.vennam.moneypilot.data.entity.SavingGoal(
                                    id = id,
                                    name = name,
                                    targetAmount = targetAmount,
                                    currentSavedAmount = currentSavedAmount,
                                    deadline = deadline,
                                    colorHex = colorHex,
                                    iconName = iconName,
                                    isCompleted = isCompleted,
                                    lastUpdated = lastUpdated
                                )
                            )
                        }
                    } catch (e: Exception) { Log.w("GoogleSheetsSyncHelper", "Failed to parse SavingGoal row: $row", e) }
                }
            } else if (range.contains("Loans")) {
                Log.d("GoogleSheetsSyncHelper", "mergeCloudDataIntoLocal: Parsing ${values.size} loans...")
                val localLoans = loanDao.getAllLoansSync().associateBy { it.id }
                for (row in values) {
                    try {
                        val id = row.getOrNull(0)?.toString()?.toDoubleOrNull()?.toLong() ?: continue
                        val name = row.getOrNull(1)?.toString() ?: continue
                        val totalAmount = row.getOrNull(2)?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                        val outstandingAmount = row.getOrNull(3)?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                        val emiAmount = row.getOrNull(4)?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                        val nextEmiDate = row.getOrNull(5)?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                        val currencyCode = row.getOrNull(6)?.toString() ?: "INR"
                        val lenderName = row.getOrNull(7)?.toString() ?: ""
                        val interestRate = row.getOrNull(8)?.toString()?.toDoubleOrNull() ?: 0.0
                        val tenureMonths = row.getOrNull(9)?.toString()?.toDoubleOrNull()?.toInt() ?: 12
                        val dueDayOfMonth = row.getOrNull(10)?.toString()?.toDoubleOrNull()?.toInt() ?: 1
                        val isNotificationEnabled = row.getOrNull(11)?.toString()?.toBoolean() ?: true
                        val startDate = row.getOrNull(12)?.toString()?.toDoubleOrNull()?.toLong() ?: System.currentTimeMillis()
                        val lastUpdatedVal = row.getOrNull(13)?.toString()?.toDoubleOrNull()?.toLong() ?: System.currentTimeMillis()

                        val localLoan = localLoans[id]
                        if (localLoan == null || lastUpdatedVal > localLoan.lastUpdated) {
                            loanDao.insertLoan(
                                prasad.vennam.moneypilot.data.entity.Loan(
                                    id = id,
                                    name = name,
                                    totalAmount = totalAmount,
                                    outstandingAmount = outstandingAmount,
                                    emiAmount = emiAmount,
                                    nextEmiDate = nextEmiDate,
                                    currencyCode = currencyCode,
                                    lenderName = lenderName,
                                    interestRate = interestRate,
                                    tenureMonths = tenureMonths,
                                    dueDayOfMonth = dueDayOfMonth,
                                    isNotificationEnabled = isNotificationEnabled,
                                    startDate = startDate,
                                    lastUpdated = lastUpdatedVal
                                )
                            )
                        }
                    } catch (e: Exception) { Log.w("GoogleSheetsSyncHelper", "Failed to parse Loan row: $row", e) }
                }
            } else if (range.contains("LoanPayments")) {
                Log.d("GoogleSheetsSyncHelper", "mergeCloudDataIntoLocal: Parsing ${values.size} loan payments...")
                for (row in values) {
                    try {
                        val id = row.getOrNull(0)?.toString()?.toDoubleOrNull()?.toLong() ?: continue
                        val loanId = row.getOrNull(1)?.toString()?.toDoubleOrNull()?.toLong() ?: continue
                        val amount = row.getOrNull(2)?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                        val date = row.getOrNull(3)?.toString()?.toDoubleOrNull()?.toLong() ?: 0L
                        val isExtraPayment = row.getOrNull(4)?.toString()?.toBoolean() ?: false
                        val note = row.getOrNull(5)?.toString() ?: ""
                        val paymentMode = row.getOrNull(6)?.toString() ?: "Cash"

                        loanPaymentDao.insertPayment(
                            prasad.vennam.moneypilot.data.entity.LoanPayment(
                                id = id,
                                loanId = loanId,
                                amount = amount,
                                date = date,
                                isExtraPayment = isExtraPayment,
                                note = note,
                                paymentMode = paymentMode
                            )
                        )
                    } catch (e: Exception) { Log.w("GoogleSheetsSyncHelper", "Failed to parse LoanPayment row: $row", e) }
                }
            }
        }
    }

    private suspend fun uploadLocalDataToSpreadsheet(
        token: String,
        spreadsheetId: String,
        categoryDao: CategoryDao,
        budgetDao: BudgetDao,
        investmentDao: InvestmentDao,
        transactionDao: TransactionDao,
        emergencyFundDao: EmergencyFundDao,
        subscriptionDao: SubscriptionDao,
        savingGoalDao: SavingGoalDao,
        loanDao: LoanDao,
        loanPaymentDao: LoanPaymentDao,
    ) {
        Log.d(
            "GoogleSheetsSyncHelper",
            "uploadLocalDataToSpreadsheet: Clearing current values on spreadsheetId=$spreadsheetId",
        )
        val clearBody =
            """
            {
              "ranges": [
                "Transactions!A1:Z10000",
                "Categories!A1:Z10000",
                "Budgets!A1:Z10000",
                "Investments!A1:Z10000",
                "EmergencyFund!A1:Z10000",
                "Subscriptions!A1:Z10000",
                "SavingGoals!A1:Z10000",
                "Loans!A1:Z10000",
                "LoanPayments!A1:Z10000"
              ]
            }
            """.trimIndent()
        val clearRequest =
            Request
                .Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values:batchClear")
                .post(clearBody.toRequestBody(mediaType))
                .addHeader("Authorization", "Bearer $token")
                .build()
        client.newCall(clearRequest).execute().use { response ->
            Log.d(
                "GoogleSheetsSyncHelper",
                "uploadLocalDataToSpreadsheet: batchClear response code=${response.code}",
            )
            if (response.code == 400 || response.code == 404) {
                Log.w(
                    "GoogleSheetsSyncHelper",
                    "uploadLocalDataToSpreadsheet: Format invalid or spreadsheet missing (throwing SheetStructureBrokenException)",
                )
                throw SheetStructureBrokenException("Spreadsheet invalid")
            }
            if (!response.isSuccessful) {
                Log.e(
                    "GoogleSheetsSyncHelper",
                    "uploadLocalDataToSpreadsheet: Clear request failed code=${response.code}",
                )
                throw Exception("Failed to clear")
            }
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val transactions: List<Transaction> = transactionDao.getAllTransactionsSync()
        val categories: List<Category> = categoryDao.getAllCategoriesSync()
        val budgets: List<Budget> = budgetDao.getAllBudgetsSync()
        val investments: List<Investment> = investmentDao.getAllInvestmentsSync()

        val tRows =
            mutableListOf<List<Any>>(
                listOf(
                    "ID",
                    "Date",
                    "Type",
                    "Category ID",
                    "Payment Mode",
                    "Amount",
                    "Note",
                    "Currency Code",
                    "Subcategory",
                    "Last Updated",
                ),
            )
        for (t in transactions) {
            val dateStr = dateFormat.format(Date(t.timestamp))
            tRows.add(
                listOf(
                    t.id,
                    dateStr,
                    t.type.name,
                    t.categoryId ?: "",
                    t.paymentMode,
                    t.amount / 100.0,
                    t.note,
                    t.currencyCode,
                    t.subCategory,
                    t.lastUpdated,
                ),
            )
        }

        val cRows = mutableListOf<List<Any>>(listOf("ID", "Name", "Icon Name", "Color", "Is Expense", "Last Updated"))
        for (c in categories) cRows.add(listOf(c.id, c.name, c.iconName, c.color, c.isExpense, c.lastUpdated))

        val bRows = mutableListOf<List<Any>>(listOf("ID", "Category ID", "Amount", "Period", "Currency Code", "Last Updated"))
        for (b in budgets) bRows.add(listOf(b.id, b.categoryId, b.amount / 100.0, b.period, b.currencyCode, b.lastUpdated))

        val iRows =
            mutableListOf<List<Any>>(
                listOf(
                    "ID",
                    "Name",
                    "Type",
                    "Invested Amount",
                    "Current Value",
                    "Currency Code",
                    "Symbol",
                    "Quantity",
                    "Interest Rate",
                    "Start Date",
                    "Last Updated",
                ),
            )
        for (i in investments) {
            iRows.add(
                listOf(
                    i.id,
                    i.name,
                    i.type,
                    i.investedAmount / 100.0,
                    i.currentValue / 100.0,
                    i.currencyCode,
                    i.symbol ?: "",
                    i.quantity ?: "",
                    i.interestRate ?: "",
                    i.startDate ?: "",
                    i.lastUpdated,
                ),
            )
        }

        val ef = emergencyFundDao.getEmergencyFundSync()
        val emergencyMonthlyExpenses = ef?.monthlyExpenses ?: 0.0
        val emergencyTargetMonths = ef?.targetMonths ?: 6
        val emergencyCurrentSaved = ef?.currentSaved ?: 0.0

        val efRows =
            mutableListOf<List<Any>>(
                listOf("Monthly Expenses", "Target Months", "Current Saved"),
                listOf(emergencyMonthlyExpenses, emergencyTargetMonths, emergencyCurrentSaved),
            )


        val subscriptions: List<prasad.vennam.moneypilot.data.entity.Subscription> = subscriptionDao.getAllSubscriptionsSync()
        val savingGoals: List<prasad.vennam.moneypilot.data.entity.SavingGoal> = savingGoalDao.getAllSavingGoalsSync()
        val loans: List<prasad.vennam.moneypilot.data.entity.Loan> = loanDao.getAllLoansSync()
        val loanPayments: List<prasad.vennam.moneypilot.data.entity.LoanPayment> = loanPaymentDao.getAllLoanPaymentsSync()

        val subRows = mutableListOf<List<Any>>(listOf("ID", "Name", "Amount", "Billing Cycle", "Next Payment Date", "Payment Mode", "Category ID", "Is Notification Enabled", "Last Updated"))
        for (s in subscriptions) subRows.add(listOf(s.id, s.name, s.amount, s.billingCycle, s.nextPaymentDate, s.paymentMode, s.categoryId ?: "", s.isNotificationEnabled, s.lastUpdated))

        val sgRows = mutableListOf<List<Any>>(listOf("ID", "Name", "Target Amount", "Current Saved Amount", "Deadline", "Color Hex", "Icon Name", "Is Completed", "Last Updated"))
        for (sg in savingGoals) sgRows.add(listOf(sg.id, sg.name, sg.targetAmount, sg.currentSavedAmount, sg.deadline, sg.colorHex, sg.iconName, sg.isCompleted, sg.lastUpdated))

        val lRows = mutableListOf<List<Any>>(listOf("ID", "Name", "Total Amount", "Outstanding Amount", "EMI Amount", "Next EMI Date", "Currency Code", "Lender Name", "Interest Rate", "Tenure Months", "Due Day of Month", "Is Notification Enabled", "Start Date", "Last Updated"))
        for (l in loans) lRows.add(listOf(l.id, l.name, l.totalAmount, l.outstandingAmount, l.emiAmount, l.nextEmiDate, l.currencyCode, l.lenderName, l.interestRate, l.tenureMonths, l.dueDayOfMonth, l.isNotificationEnabled, l.startDate, l.lastUpdated))

        val lpRows = mutableListOf<List<Any>>(listOf("ID", "Loan ID", "Amount", "Date", "Is Extra Payment", "Note", "Payment Mode"))
        for (lp in loanPayments) lpRows.add(listOf(lp.id, lp.loanId, lp.amount, lp.date, lp.isExtraPayment, lp.note, lp.paymentMode))

        val bodyMap =
            mapOf(
                "valueInputOption" to "USER_ENTERED",
                "data" to
                    listOf(
                        mapOf("range" to "Transactions!A1", "values" to tRows),
                        mapOf("range" to "Categories!A1", "values" to cRows),
                        mapOf("range" to "Budgets!A1", "values" to bRows),
                        mapOf("range" to "Investments!A1", "values" to iRows),
                        mapOf("range" to "EmergencyFund!A1", "values" to efRows),
                        mapOf("range" to "Subscriptions!A1", "values" to subRows),
                        mapOf("range" to "SavingGoals!A1", "values" to sgRows),
                        mapOf("range" to "Loans!A1", "values" to lRows),
                        mapOf("range" to "LoanPayments!A1", "values" to lpRows),
                    ),
            )
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val updateBody = moshi.adapter(Map::class.java).toJson(bodyMap)

        Log.d(
            "GoogleSheetsSyncHelper",
            "uploadLocalDataToSpreadsheet: Uploading local data sizes: " +
                "transactions=${transactions.size}, categories=${categories.size}, budgets=${budgets.size}, " +
                "investments=${investments.size}, emergencyFund(monthly=$emergencyMonthlyExpenses, months=$emergencyTargetMonths, saved=$emergencyCurrentSaved)",
        )

        val updateReq =
            Request
                .Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values:batchUpdate")
                .post(updateBody.toRequestBody(mediaType))
                .addHeader("Authorization", "Bearer $token")
                .build()
        client.newCall(updateReq).execute().use { response ->
            Log.d(
                "GoogleSheetsSyncHelper",
                "uploadLocalDataToSpreadsheet: batchUpdate response code=${response.code}, message=${response.message}",
            )
            if (!response.isSuccessful) {
                Log.e(
                    "GoogleSheetsSyncHelper",
                    "uploadLocalDataToSpreadsheet: batchUpdate request failed code=${response.code}",
                )
                throw Exception("Failed to upload values")
            }
        }
    }

    suspend fun deleteSpreadsheetFile(
        context: Context,
        email: String,
        spreadsheetId: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val account = android.accounts.Account(email, "com.google")
                val scopeString = "oauth2:https://www.googleapis.com/auth/spreadsheets https://www.googleapis.com/auth/drive.metadata.readonly"
                val token = GoogleAuthUtil.getToken(context, account, scopeString)

                // 1. Attempt to delete spreadsheet file using Drive API
                val deleteReq =
                    Request
                        .Builder()
                        .url("https://www.googleapis.com/drive/v3/files/$spreadsheetId")
                        .delete()
                        .addHeader("Authorization", "Bearer $token")
                        .build()

                client.newCall(deleteReq).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d("GoogleSheetsSyncHelper", "Spreadsheet deleted successfully via Drive API")
                        return@withContext true
                    } else {
                        Log.w(
                            "GoogleSheetsSyncHelper",
                            "Drive API delete returned status ${response.code}. Attempting fallback to clear contents.",
                        )
                    }
                }

                // 2. Fallback: Clear all values in the spreadsheet using Sheets API batchClear
                val clearBody =
                    """
                    {
                      "ranges": [
                        "Transactions!A1:Z10000",
                        "Categories!A1:Z10000",
                        "Budgets!A1:Z10000",
                        "Investments!A1:Z10000",
                        "EmergencyFund!A1:Z10000"
                      ]
                    }
                    """.trimIndent()
                val clearRequest =
                    Request
                        .Builder()
                        .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values:batchClear")
                        .post(clearBody.toRequestBody(mediaType))
                        .addHeader("Authorization", "Bearer $token")
                        .build()

                client.newCall(clearRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d("GoogleSheetsSyncHelper", "Spreadsheet data wiped successfully via Sheets API (fallback)")
                        return@withContext true
                    } else {
                        Log.e("GoogleSheetsSyncHelper", "Wiping spreadsheet data also failed: code ${response.code}")
                    }
                }
                false
            } catch (e: Exception) {
                Log.e("GoogleSheetsSyncHelper", "Exception during spreadsheet deletion/clearing", e)
                false
            }
        }

    private fun ensureRequiredSheetsExist(
        token: String,
        spreadsheetId: String,
    ) {
        Log.d(
            "GoogleSheetsSyncHelper",
            "ensureRequiredSheetsExist: Checking sheet structure for spreadsheetId=$spreadsheetId",
        )
        try {
            val request =
                Request
                    .Builder()
                    .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId?fields=sheets.properties.title")
                    .addHeader("Authorization", "Bearer $token")
                    .build()

            client.newCall(request).execute().use { response ->
                Log.d(
                    "GoogleSheetsSyncHelper",
                    "ensureRequiredSheetsExist: Get metadata response code=${response.code}",
                )
                if (!response.isSuccessful) {
                    Log.e(
                        "GoogleSheetsSyncHelper",
                        "ensureRequiredSheetsExist: Failed to retrieve spreadsheet metadata, response code=${response.code}",
                    )
                    return
                }

                val bodyStr = response.body.string()

                // Extract all sheet titles using regex
                val regex = "\"title\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                val existingTitles = regex.findAll(bodyStr).map { it.groupValues[1] }.toSet()
                Log.d(
                    "GoogleSheetsSyncHelper",
                    "ensureRequiredSheetsExist: Existing sheets in spreadsheet: $existingTitles",
                )

                val requiredSheets =
                    listOf("Transactions", "Categories", "Budgets", "Investments", "EmergencyFund", "Subscriptions", "SavingGoals", "Loans", "LoanPayments")
                val missingSheets = requiredSheets.filter { it !in existingTitles }

                if (missingSheets.isNotEmpty()) {
                    Log.d(
                        "GoogleSheetsSyncHelper",
                        "ensureRequiredSheetsExist: Missing sheets detected: $missingSheets. Sending batchUpdate to add them...",
                    )
                    val requestsJson =
                        missingSheets.joinToString(separator = ",") { sheetTitle ->
                            """
                            {
                              "addSheet": {
                                "properties": {
                                  "title": "$sheetTitle"
                                }
                              }
                            }
                            """.trimIndent()
                        }
                    val batchUpdateBody =
                        """
                        {
                          "requests": [
                            $requestsJson
                          ]
                        }
                        """.trimIndent()

                    val updateRequest =
                        Request
                            .Builder()
                            .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId:batchUpdate")
                            .post(batchUpdateBody.toRequestBody(mediaType))
                            .addHeader("Authorization", "Bearer $token")
                            .build()

                    client.newCall(updateRequest).execute().use { updateResponse ->
                        if (!updateResponse.isSuccessful) {
                            Log.e(
                                "GoogleSheetsSyncHelper",
                                "ensureRequiredSheetsExist: Failed to add missing sheets: ${updateResponse.code}",
                            )
                        } else {
                            Log.d(
                                "GoogleSheetsSyncHelper",
                                "ensureRequiredSheetsExist: Successfully added missing sheets: $missingSheets",
                            )
                        }
                    }
                } else {
                    Log.d(
                        "GoogleSheetsSyncHelper",
                        "ensureRequiredSheetsExist: All required sheets already exist in the spreadsheet",
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(
                "GoogleSheetsSyncHelper",
                "ensureRequiredSheetsExist: Error checking/updating sheets structure",
                e,
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class DriveFilesResponse(
    val files: List<DriveFile>?,
)

@JsonClass(generateAdapter = true)
data class DriveFile(
    val id: String?,
    val name: String?,
)

@JsonClass(generateAdapter = true)
data class BatchGetSpreadsheetResponse(
    val valueRanges: List<ValueRange>?,
)

@JsonClass(generateAdapter = true)
data class ValueRange(
    val range: String?,
    val values: List<List<Any>>?,
)
