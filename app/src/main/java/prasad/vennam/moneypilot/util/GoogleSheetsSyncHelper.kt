package prasad.vennam.moneypilot.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
        spreadsheetId: String?,
        isRestore: Boolean = false,
        onSpreadsheetIdFound: suspend (String) -> Unit,
    ): SyncResult =
        withContext(Dispatchers.IO) {
            try {
                val account = android.accounts.Account(email, "com.google")
                val scopeString = "oauth2:https://www.googleapis.com/auth/spreadsheets https://www.googleapis.com/auth/drive.metadata.readonly"
                val token = GoogleAuthUtil.getToken(context, account, scopeString)

                var currentSpreadsheetId = spreadsheetId

                if (currentSpreadsheetId.isNullOrEmpty()) {
                    currentSpreadsheetId = findExistingSpreadsheet(token)
                    if (currentSpreadsheetId != null) {
                        onSpreadsheetIdFound(currentSpreadsheetId)
                    } else {
                        currentSpreadsheetId = createSpreadsheet(token)
                        onSpreadsheetIdFound(currentSpreadsheetId)
                    }
                }

                try {
                    if (isRestore) {
                        // Restore/login: download and merge first to populate local empty DB
                        val valueRanges = downloadSpreadsheetData(token, currentSpreadsheetId)
                        if (valueRanges != null) {
                            mergeCloudDataIntoLocal(repository, valueRanges)
                        }
                        // Then upload
                        uploadLocalDataToSpreadsheet(token, currentSpreadsheetId, repository)
                    } else {
                        // Normal sync: upload local changes first so cloud backup gets updated, preventing local edits from being overwritten
                        uploadLocalDataToSpreadsheet(token, currentSpreadsheetId, repository)
                        // Then download/merge
                        val valueRanges = downloadSpreadsheetData(token, currentSpreadsheetId)
                        if (valueRanges != null) {
                            mergeCloudDataIntoLocal(repository, valueRanges)
                        }
                    }
                } catch (e: SheetStructureBrokenException) {
                    // If it's broken, we just recreate it
                    val newId = createSpreadsheet(token)
                    onSpreadsheetIdFound(newId)
                    uploadLocalDataToSpreadsheet(token, newId, repository)
                }

                SyncResult.Success
            } catch (e: UserRecoverableAuthException) {
                SyncResult.NeedAuthorization(e.intent ?: Intent())
            } catch (e: Exception) {
                Log.e("GoogleSheetsSyncHelper", "Sync Exception", e)
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
                { "properties": { "title": "Investments" } }
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
        val url =
            "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values:batchGet" +
                "?ranges=Transactions!A2:Z10000" +
                "&ranges=Categories!A2:Z10000" +
                "&ranges=Budgets!A2:Z10000" +
                "&ranges=Investments!A2:Z10000"

        val request =
            Request
                .Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 400 || response.code == 404) {
                throw SheetStructureBrokenException("Spreadsheet format invalid")
            }
            if (!response.isSuccessful) throw Exception("Failed to fetch values")
            val bodyStr = response.body.string()
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val batchResponse = moshi.adapter(BatchGetSpreadsheetResponse::class.java).fromJson(bodyStr)
            return batchResponse?.valueRanges
        }
    }

    private suspend fun mergeCloudDataIntoLocal(
        repository: MoneyPilotRepository,
        valueRanges: List<ValueRange>,
    ) {
        for (vr in valueRanges) {
            val range = vr.range ?: continue
            val values = vr.values ?: continue

            if (range.contains("Categories")) {
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
                    val sheetStartDate = row.getOrNull(9)?.toString()?.toDoubleOrNull()?.toLong()

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
            }
        }
    }

    private suspend fun uploadLocalDataToSpreadsheet(
        token: String,
        spreadsheetId: String,
        repository: MoneyPilotRepository,
    ) {
        val clearBody =
            """
            {
              "ranges": [
                "Transactions!A1:Z10000",
                "Categories!A1:Z10000",
                "Budgets!A1:Z10000",
                "Investments!A1:Z10000"
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
            if (response.code == 400 || response.code == 404) throw SheetStructureBrokenException("Spreadsheet invalid")
            if (!response.isSuccessful) throw Exception("Failed to clear")
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val transactions = repository.transactionDao.getAllTransactionsSync()
        val categories = repository.categoryDao.getAllCategoriesSync()
        val budgets = repository.budgetDao.getAllBudgetsSync()
        val investments = repository.investmentDao.getAllInvestmentsSync()

        val tRows = mutableListOf<List<Any>>(
            listOf(
                "ID", "Date", "Type", "Category ID", "Payment Mode", "Amount", "Note", "Currency Code", "Subcategory"
            )
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
                    t.subCategory
                )
            )
        }

        val cRows = mutableListOf<List<Any>>(listOf("ID", "Name", "Icon Name", "Color", "Is Expense"))
        for (c in categories) cRows.add(listOf(c.id, c.name, c.iconName, c.color, c.isExpense))

        val bRows = mutableListOf<List<Any>>(listOf("ID", "Category ID", "Amount", "Period", "Currency Code"))
        for (b in budgets) bRows.add(listOf(b.id, b.categoryId, b.amount / 100.0, b.period, b.currencyCode))

        val iRows = mutableListOf<List<Any>>(
            listOf(
                "ID", "Name", "Type", "Invested Amount", "Current Value", "Currency Code",
                "Symbol", "Quantity", "Interest Rate", "Start Date"
            )
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
                    i.startDate ?: ""
                )
            )
        }

        val bodyMap =
            mapOf(
                "valueInputOption" to "USER_ENTERED",
                "data" to
                    listOf(
                        mapOf("range" to "Transactions!A1", "values" to tRows),
                        mapOf("range" to "Categories!A1", "values" to cRows),
                        mapOf("range" to "Budgets!A1", "values" to bRows),
                        mapOf("range" to "Investments!A1", "values" to iRows),
                    ),
            )
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val updateBody = moshi.adapter(Map::class.java).toJson(bodyMap)

        val updateReq =
            Request
                .Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values:batchUpdate")
                .post(updateBody.toRequestBody(mediaType))
                .addHeader("Authorization", "Bearer $token")
                .build()
        client.newCall(updateReq).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to upload values")
        }
    }
}

data class DriveFilesResponse(
    val files: List<DriveFile>?,
)

data class DriveFile(
    val id: String?,
    val name: String?,
)

data class BatchGetSpreadsheetResponse(
    val valueRanges: List<ValueRange>?,
)

data class ValueRange(
    val range: String?,
    val values: List<List<Any>>?,
)
