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
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.EmergencyFund
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
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
        repository: MoneyPilotRepository,
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
            try {
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
                    if (isRestore) {
                        Log.d(
                            "GoogleSheetsSyncHelper",
                            "performTwoWaySync: Running Restore sequence. Downloading cloud data first...",
                        )
                        val valueRanges = downloadSpreadsheetData(token, currentSpreadsheetId)
                        if (valueRanges != null) {
                            Log.d(
                                "GoogleSheetsSyncHelper",
                                "performTwoWaySync: Cloud data downloaded. Merging into local database...",
                            )
                            mergeCloudDataIntoLocal(repository, userPreferences, valueRanges)
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
                        uploadLocalDataToSpreadsheet(token, currentSpreadsheetId, repository)
                    } else {
                        Log.d(
                            "GoogleSheetsSyncHelper",
                            "performTwoWaySync: Running normal Sync sequence. Uploading local changes first...",
                        )
                        uploadLocalDataToSpreadsheet(token, currentSpreadsheetId, repository)
                        Log.d(
                            "GoogleSheetsSyncHelper",
                            "performTwoWaySync: Downloading latest cloud updates...",
                        )
                        val valueRanges = downloadSpreadsheetData(token, currentSpreadsheetId)
                        if (valueRanges != null) {
                            Log.d(
                                "GoogleSheetsSyncHelper",
                                "performTwoWaySync: Merging cloud updates into local database...",
                            )
                            mergeCloudDataIntoLocal(repository, userPreferences, valueRanges)
                        } else {
                            Log.w(
                                "GoogleSheetsSyncHelper",
                                "performTwoWaySync: Downloaded valueRanges is null",
                            )
                        }
                    }
                    Log.d(
                        "GoogleSheetsSyncHelper",
                        "performTwoWaySync: Sync sequence successfully completed",
                    )
                } catch (e: SheetStructureBrokenException) {
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
                    uploadLocalDataToSpreadsheet(token, newId, repository)
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
            val match = "\"spreadsheetId\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(bodyStr ?: "")
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
                "&ranges=EmergencyFund!A2:Z10000"

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

    private suspend fun mergeCloudDataIntoLocal(
        repository: MoneyPilotRepository,
        userPreferences: UserPreferences,
        valueRanges: List<ValueRange>,
    ) {
        for (vr in valueRanges) {
            val range = vr.range ?: continue
            val values = vr.values ?: continue

            if (range.contains("Categories")) {
                Log.d(
                    "GoogleSheetsSyncHelper",
                    "mergeCloudDataIntoLocal: Parsing ${values.size} categories...",
                )
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
                    if (name.isNotEmpty()) {
                        repository.insertCategory(Category(id = id, name = name, iconName = iconName, color = color, isExpense = isExpense))
                    }
                }
            } else if (range.contains("Budgets")) {
                Log.d(
                    "GoogleSheetsSyncHelper",
                    "mergeCloudDataIntoLocal: Parsing ${values.size} budgets...",
                )
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
                    repository.insertBudget(
                        Budget(
                            id = id,
                            categoryId = categoryId,
                            amount = (amount * 100).toLong(),
                            period = period,
                            currencyCode = currencyCode,
                        ),
                    )
                }
            } else if (range.contains("Investments")) {
                Log.d(
                    "GoogleSheetsSyncHelper",
                    "mergeCloudDataIntoLocal: Parsing ${values.size} investments...",
                )
                val localInvestments = repository.investmentDao.getAllInvestmentsSync().associateBy { it.id }
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

                    val localInv = localInvestments[id]
                    val symbol = sheetSymbol ?: localInv?.symbol
                    val quantity = sheetQuantity ?: localInv?.quantity
                    val interestRate = sheetInterestRate ?: localInv?.interestRate
                    val startDate = sheetStartDate ?: localInv?.startDate

                    if (name.isNotEmpty()) {
                        repository.insertInvestment(
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
                            ),
                        )
                    }
                }
            } else if (range.contains("Transactions")) {
                Log.d(
                    "GoogleSheetsSyncHelper",
                    "mergeCloudDataIntoLocal: Parsing ${values.size} transactions...",
                )
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val localTransactions = repository.transactionDao.getAllTransactionsSync().associateBy { it.id }
                for (row in values) {
                    val idStr = row.getOrNull(0)?.toString().orEmpty()
                    val id = idStr.toDoubleOrNull()?.toLong() ?: 0L
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

                    repository.insertTransaction(
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
                        ),
                    )
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
                        repository.insertEmergencyFund(
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
            }
        }
    }

    private suspend fun uploadLocalDataToSpreadsheet(
        token: String,
        spreadsheetId: String,
        repository: MoneyPilotRepository,
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

        val transactions = repository.transactionDao.getAllTransactionsSync()
        val categories = repository.categoryDao.getAllCategoriesSync()
        val budgets = repository.budgetDao.getAllBudgetsSync()
        val investments = repository.investmentDao.getAllInvestmentsSync()

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
                ),
            )
        }

        val cRows = mutableListOf<List<Any>>(listOf("ID", "Name", "Icon Name", "Color", "Is Expense"))
        for (c in categories) cRows.add(listOf(c.id, c.name, c.iconName, c.color, c.isExpense))

        val bRows = mutableListOf<List<Any>>(listOf("ID", "Category ID", "Amount", "Period", "Currency Code"))
        for (b in budgets) bRows.add(listOf(b.id, b.categoryId, b.amount / 100.0, b.period, b.currencyCode))

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
                ),
            )
        }

        val ef = repository.getEmergencyFundSync()
        val emergencyMonthlyExpenses = ef?.monthlyExpenses ?: 0.0
        val emergencyTargetMonths = ef?.targetMonths ?: 6
        val emergencyCurrentSaved = ef?.currentSaved ?: 0.0

        val efRows =
            mutableListOf<List<Any>>(
                listOf("Monthly Expenses", "Target Months", "Current Saved"),
                listOf(emergencyMonthlyExpenses, emergencyTargetMonths, emergencyCurrentSaved),
            )

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
                    listOf("Transactions", "Categories", "Budgets", "Investments", "EmergencyFund")
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
