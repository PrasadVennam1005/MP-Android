package prasad.vennam.moneypilot.util

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Investment
import prasad.vennam.moneypilot.data.entity.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import prasad.vennam.moneypilot.data.entity.TransactionType

sealed interface SyncResult {
    object Success : SyncResult
    data class NeedAuthorization(val intent: Intent) : SyncResult
    data class Error(val exception: Throwable) : SyncResult
}

sealed interface RestoreResult {
    object Success : RestoreResult
    object NoBackupFound : RestoreResult
    data class NeedAuthorization(val intent: Intent) : RestoreResult
    data class Error(val exception: Throwable) : RestoreResult
}

class SheetStructureBrokenException(message: String) : Exception(message)

object GoogleSheetsSyncHelper {

    const val SYNC_WORK_NAME = "google_sheets_sync_work"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun performSync(
        context: Context,
        email: String,
        transactions: List<Transaction>,
        categories: List<Category>,
        budgets: List<Budget>,
        investments: List<Investment>,
        spreadsheetId: String?,
        onSpreadsheetIdCreated: suspend (String) -> Unit,
        onSpreadsheetIdCleared: suspend () -> Unit
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            val account = Account(email, "com.google")
            val scopeString = "oauth2:https://www.googleapis.com/auth/spreadsheets https://www.googleapis.com/auth/drive.metadata.readonly"
            
            val token = GoogleAuthUtil.getToken(context, account, scopeString)

            var currentSpreadsheetId = spreadsheetId

            if (currentSpreadsheetId.isNullOrEmpty()) {
                currentSpreadsheetId = createSpreadsheet(token)
                onSpreadsheetIdCreated(currentSpreadsheetId)
            }

            try {
                syncData(token, currentSpreadsheetId, transactions, categories, budgets, investments)
            } catch (e: SheetStructureBrokenException) {
                onSpreadsheetIdCleared()
                val newSpreadsheetId = createSpreadsheet(token)
                onSpreadsheetIdCreated(newSpreadsheetId)
                syncData(token, newSpreadsheetId, transactions, categories, budgets, investments)
            }

            SyncResult.Success
        } catch (e: UserRecoverableAuthException) {
            SyncResult.NeedAuthorization(e.intent ?: Intent())
        } catch (e: Exception) {
            SyncResult.Error(e)
        }
    }

    private fun createSpreadsheet(token: String): String {
        val createBody = """
        {
          "properties": {
            "title": "MoneyPilot Backup"
          },
          "sheets": [
            { "properties": { "title": "Transactions" } },
            { "properties": { "title": "Categories" } },
            { "properties": { "title": "Budgets" } },
            { "properties": { "title": "Investments" } }
          ]
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://sheets.googleapis.com/v4/spreadsheets")
            .post(createBody.toRequestBody(mediaType))
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string()
            if (!response.isSuccessful) {
                val errorMsg = extractErrorMessage(bodyStr, "${response.code} ${response.message}")
                throw Exception("Failed to create spreadsheet: $errorMsg")
            }
            if (bodyStr.isNullOrEmpty()) throw Exception("Empty response body when creating spreadsheet")
            val match = "\"spreadsheetId\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(bodyStr)
            return match?.groupValues?.get(1) ?: throw Exception("Could not parse spreadsheetId from response")
        }
    }

    private fun syncData(
        token: String,
        spreadsheetId: String,
        transactions: List<Transaction>,
        categories: List<Category>,
        budgets: List<Budget>,
        investments: List<Investment>
    ) {
        val clearBody = """
        {
          "ranges": [
            "Transactions!A1:Z10000",
            "Categories!A1:Z10000",
            "Budgets!A1:Z10000",
            "Investments!A1:Z10000"
          ]
        }
        """.trimIndent()

        val clearRequest = Request.Builder()
            .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values:batchClear")
            .post(clearBody.toRequestBody(mediaType))
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(clearRequest).execute().use { response ->
            if (response.code == 400 || response.code == 404) {
                throw SheetStructureBrokenException("Spreadsheet not found or format invalid")
            }
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                val errorMsg = extractErrorMessage(errorBody, "${response.code} ${response.message}")
                throw Exception("Failed to clear values: $errorMsg")
            }
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val transactionsRows = mutableListOf<List<Any>>()
        transactionsRows.add(listOf("Date", "Type", "Category", "Payment Mode", "Amount", "Note"))
        for (t in transactions) {
            val dateStr = dateFormat.format(Date(t.timestamp))
            val categoryName = categories.find { it.id == t.categoryId }?.name ?: "Other"
            transactionsRows.add(listOf(dateStr, t.type.name, categoryName, t.paymentMode, t.amount, t.note))
        }

        val categoriesRows = mutableListOf<List<Any>>()
        categoriesRows.add(listOf("ID", "Name", "Icon Name", "Color", "Is Expense"))
        for (c in categories) {
            categoriesRows.add(listOf(c.id, c.name, c.iconName, c.color, c.isExpense))
        }

        val budgetsRows = mutableListOf<List<Any>>()
        budgetsRows.add(listOf("Category", "Amount", "Period"))
        for (b in budgets) {
            val categoryName = categories.find { it.id == b.categoryId }?.name ?: "Other"
            budgetsRows.add(listOf(categoryName, b.amount, b.period))
        }

        val investmentsRows = mutableListOf<List<Any>>()
        investmentsRows.add(listOf("Name", "Type", "Invested Amount", "Current Value"))
        for (i in investments) {
            investmentsRows.add(listOf(i.name, i.type, i.investedAmount, i.currentValue))
        }

        val batchUpdateBody = """
        {
          "valueInputOption": "USER_ENTERED",
          "data": [
            {
              "range": "Transactions!A1",
              "values": [
                ${transactionsRows.joinToString(",") { formatRow(it) }}
              ]
            },
            {
              "range": "Categories!A1",
              "values": [
                ${categoriesRows.joinToString(",") { formatRow(it) }}
              ]
            },
            {
              "range": "Budgets!A1",
              "values": [
                ${budgetsRows.joinToString(",") { formatRow(it) }}
              ]
            },
            {
              "range": "Investments!A1",
              "values": [
                ${investmentsRows.joinToString(",") { formatRow(it) }}
              ]
            }
          ]
        }
        """.trimIndent()

        val updateRequest = Request.Builder()
            .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values:batchUpdate")
            .post(batchUpdateBody.toRequestBody(mediaType))
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(updateRequest).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                val errorMsg = extractErrorMessage(errorBody, "${response.code} ${response.message}")
                throw Exception("Failed to write values: $errorMsg")
            }
        }
    }

    private fun extractErrorMessage(responseBody: String?, fallbackMessage: String): String {
        if (responseBody.isNullOrEmpty()) return fallbackMessage
        val match = "\"message\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(responseBody)
        return match?.groupValues?.get(1)?.replace("\\n", "\n") ?: responseBody
    }

    private fun formatRow(row: List<Any>): String {
        return row.joinToString(separator = ",", prefix = "[", postfix = "]") { value ->
            when (value) {
                is Number -> value.toString()
                is Boolean -> value.toString()
                else -> "\"${escapeJsonString(value.toString())}\""
            }
        }
    }

    private fun escapeJsonString(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    suspend fun findExistingSpreadsheet(token: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = java.net.URLEncoder.encode("name = 'MoneyPilot Backup' and trashed = false", "UTF-8").replace("+", "%20")
            val fields = "files(id,name)"
            val url = "https://www.googleapis.com/drive/v3/files?q=$query&spaces=drive&fields=$fields"

            Log.d("MoneyPilotRestore", "findExistingSpreadsheet: Searching Drive with URL: $url")
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer $token")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                Log.d("MoneyPilotRestore", "findExistingSpreadsheet: response.code=${response.code}, body=$bodyStr")
                if (!response.isSuccessful || bodyStr.isNullOrEmpty()) {
                    Log.w("MoneyPilotRestore", "findExistingSpreadsheet: API call failed or body is empty. code=${response.code}")
                    return@withContext null
                }
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val filesResponse = moshi.adapter(DriveFilesResponse::class.java).fromJson(bodyStr)
                val files = filesResponse?.files
                Log.d("MoneyPilotRestore", "findExistingSpreadsheet: Files found quantity: ${files?.size ?: 0}")
                if (!files.isNullOrEmpty()) {
                    Log.d("MoneyPilotRestore", "findExistingSpreadsheet: Found spreadsheet match! ID=${files[0].id}, Name=${files[0].name}")
                    return@withContext files[0].id
                } else {
                    Log.d("MoneyPilotRestore", "findExistingSpreadsheet: No spreadsheets matched the query in the files list.")
                }
            }
        } catch (e: Exception) {
            Log.e("MoneyPilotRestore", "Error searching Drive for spreadsheet", e)
        }
        null
    }

    suspend fun checkForRestoreAndExecute(
        context: Context,
        email: String,
        onSpreadsheetIdFound: suspend (String) -> Unit,
        onRestoreData: suspend (List<Category>, List<Transaction>, List<Budget>, List<Investment>) -> Unit
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: Triggered restore sequence for $email")
            val account = Account(email, "com.google")
            val scopeString = "oauth2:https://www.googleapis.com/auth/spreadsheets https://www.googleapis.com/auth/drive.metadata.readonly"
            
            val token = try {
                val t = GoogleAuthUtil.getToken(context, account, scopeString)
                Log.d("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: Token successfully fetched.")
                t
            } catch (e: UserRecoverableAuthException) {
                Log.w("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: UserRecoverableAuthException encountered. Intent path needed.")
                return@withContext RestoreResult.NeedAuthorization(e.intent ?: Intent())
            } catch (e: Exception) {
                Log.e("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: GoogleAuthUtil token fetch failed", e)
                return@withContext RestoreResult.Error(e)
            }

            val spreadsheetId = findExistingSpreadsheet(token)
            if (spreadsheetId == null) {
                Log.d("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: Backup spreadsheet 'MoneyPilot Backup' not found on Drive.")
                return@withContext RestoreResult.NoBackupFound
            }
            Log.d("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: Found backup spreadsheetId=$spreadsheetId")
            onSpreadsheetIdFound(spreadsheetId)

            val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values:batchGet" +
                    "?ranges=Categories!A2:E10000" +
                    "&ranges=Transactions!A2:F10000" +
                    "&ranges=Budgets!A2:C10000" +
                    "&ranges=Investments!A2:D10000"

            Log.d("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: Fetching ranges with URL: $url")
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer $token")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                Log.d("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: BatchGet response. code=${response.code}, body.length=${bodyStr?.length ?: 0}")
                if (!response.isSuccessful || bodyStr.isNullOrEmpty()) {
                    Log.w("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: API call was unsuccessful. code=${response.code}")
                    return@withContext RestoreResult.Error(Exception("Failed to fetch worksheets: ${response.code}"))
                }

                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val batchResponse = moshi.adapter(BatchGetSpreadsheetResponse::class.java).fromJson(bodyStr)
                val valueRanges = batchResponse?.valueRanges
                Log.d("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: Parsed valueRanges quantity: ${valueRanges?.size ?: 0}")
                if (valueRanges.isNullOrEmpty()) {
                    Log.d("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: Empty value ranges returned.")
                    return@withContext RestoreResult.NoBackupFound
                }

                val restoredCategories = mutableListOf<Category>()
                val rawTransactions = mutableListOf<List<Any>>()
                val rawBudgets = mutableListOf<List<Any>>()
                val restoredInvestments = mutableListOf<Investment>()

                for (vr in valueRanges) {
                    val range = vr.range ?: continue
                    val values = vr.values ?: continue
                    Log.d("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: Reading range=$range containing ${values.size} rows")
                    if (range.contains("Categories")) {
                        for (row in values) {
                            val id = row.getOrNull(0)?.toString()?.toDoubleOrNull()?.toLong()
                                ?: row.getOrNull(0)?.toString()?.toLongOrNull() ?: 0L
                            val name = row.getOrNull(1)?.toString().orEmpty()
                            val iconName = row.getOrNull(2)?.toString().orEmpty()
                            val color = row.getOrNull(3)?.toString()?.toDoubleOrNull()?.toLong()
                                ?: row.getOrNull(3)?.toString()?.toLongOrNull() ?: 0L
                            val isExpense = row.getOrNull(4)?.toString()?.toBoolean() ?: true
                            if (name.isNotEmpty()) {
                                restoredCategories.add(
                                    Category(id = id, name = name, iconName = iconName, color = color, isExpense = isExpense)
                                )
                            }
                        }
                    } else if (range.contains("Transactions")) {
                        rawTransactions.addAll(values)
                    } else if (range.contains("Budgets")) {
                        rawBudgets.addAll(values)
                    } else if (range.contains("Investments")) {
                        for (row in values) {
                            val name = row.getOrNull(0)?.toString().orEmpty()
                            val type = row.getOrNull(1)?.toString().orEmpty()
                            val investedAmount = row.getOrNull(2)?.toString()?.toDoubleOrNull() ?: 0.0
                            val currentValue = row.getOrNull(3)?.toString()?.toDoubleOrNull() ?: 0.0
                            if (name.isNotEmpty()) {
                                restoredInvestments.add(
                                    Investment(name = name, type = type, investedAmount = investedAmount, currentValue = currentValue)
                                )
                            }
                        }
                    }
                }

                Log.d("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: Restoring categories quantity: ${restoredCategories.size}")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val restoredTransactions = mutableListOf<Transaction>()
                for (row in rawTransactions) {
                    val dateStr = row.getOrNull(0)?.toString().orEmpty()
                    val typeStr = row.getOrNull(1)?.toString().orEmpty()
                    val categoryName = row.getOrNull(2)?.toString().orEmpty()
                    val paymentMode = row.getOrNull(3)?.toString().orEmpty()
                    val amount = row.getOrNull(4)?.toString()?.toDoubleOrNull() ?: 0.0
                    val note = row.getOrNull(5)?.toString().orEmpty()

                    val timestamp = try {
                        dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        Log.w("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: Failed parsing date string $dateStr", e)
                        System.currentTimeMillis()
                    }
                    val type = try {
                        TransactionType.valueOf(typeStr)
                    } catch (e: Exception) {
                        TransactionType.EXPENSE
                    }
                    val categoryId = restoredCategories.find { it.name.equals(categoryName, ignoreCase = true) }?.id

                    restoredTransactions.add(
                        Transaction(
                            amount = amount,
                            timestamp = timestamp,
                            categoryId = categoryId,
                            paymentMode = paymentMode,
                            note = note,
                            type = type
                        )
                    )
                }

                Log.d("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: Restoring transactions quantity: ${restoredTransactions.size}")
                val restoredBudgets = mutableListOf<Budget>()
                for (row in rawBudgets) {
                    val categoryName = row.getOrNull(0)?.toString().orEmpty()
                    val amount = row.getOrNull(1)?.toString()?.toDoubleOrNull() ?: 0.0
                    val period = row.getOrNull(2)?.toString().orEmpty()

                    val categoryId = restoredCategories.find { it.name.equals(categoryName, ignoreCase = true) }?.id
                    if (categoryId != null) {
                        restoredBudgets.add(
                            Budget(
                                categoryId = categoryId,
                                amount = amount,
                                period = period
                            )
                        )
                    }
                }

                Log.d("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: Triggering onRestoreData callback. Categories=${restoredCategories.size}, Transactions=${restoredTransactions.size}, Budgets=${restoredBudgets.size}, Investments=${restoredInvestments.size}")
                onRestoreData(restoredCategories, restoredTransactions, restoredBudgets, restoredInvestments)
                Log.d("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: Restore database execution complete.")
                return@withContext RestoreResult.Success
            }
        } catch (e: UserRecoverableAuthException) {
            Log.w("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: UserRecoverableAuthException caught", e)
            RestoreResult.NeedAuthorization(e.intent ?: Intent())
        } catch (e: Exception) {
            Log.e("GoogleSheetsSyncHelper", "checkForRestoreAndExecute: Exception in restore processing", e)
            RestoreResult.Error(e)
        }
    }
}

data class DriveFilesResponse(val files: List<DriveFile>?)
data class DriveFile(val id: String?, val name: String?)

data class BatchGetSpreadsheetResponse(val valueRanges: List<ValueRange>?)
data class ValueRange(val range: String?, val values: List<List<Any>>?)
