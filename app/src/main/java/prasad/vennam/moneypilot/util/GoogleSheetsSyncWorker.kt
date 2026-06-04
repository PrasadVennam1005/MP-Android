package prasad.vennam.moneypilot.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository

class GoogleSheetsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun repository(): MoneyPilotRepository
        fun userPreferences(): UserPreferences
    }

    override suspend fun doWork(): Result {
        Log.d("GoogleSheetsSyncWorker", "doWork: Sync worker started")
        val appContext = applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, WorkerEntryPoint::class.java)
        val repository = entryPoint.repository()
        val userPreferences = entryPoint.userPreferences()

        val userData = userPreferences.userData.first()
        if (userData == null || userData.email.isEmpty() || userData.email == "guest@moneypilot.app") {
            Log.d("GoogleSheetsSyncWorker", "doWork: User not logged in or is guest, skipping background sync")
            return Result.success()
        }

        val spreadsheetId = userPreferences.spreadsheetId.first()
        val isAlreadySynced = userPreferences.isSynced.first()
        if (isAlreadySynced) {
            Log.d("GoogleSheetsSyncWorker", "doWork: Already synced, skipping")
            return Result.success()
        }

        val transactions = repository.allTransactions.first()
        val categories = repository.allCategories.first()
        val budgets = repository.allBudgets.first()
        val investments = repository.allInvestments.first()

        Log.d("GoogleSheetsSyncWorker", "doWork: Performing sheet sync for ${userData.email}")
        val syncResult = GoogleSheetsSyncHelper.performSync(
            context = appContext,
            email = userData.email,
            transactions = transactions,
            categories = categories,
            budgets = budgets,
            investments = investments,
            spreadsheetId = spreadsheetId,
            onSpreadsheetIdCreated = { id ->
                userPreferences.saveSpreadsheetId(id)
            },
            onSpreadsheetIdCleared = {
                userPreferences.clearSpreadsheetId()
            }
        )

        return when (syncResult) {
            is SyncResult.Success -> {
                Log.d("GoogleSheetsSyncWorker", "doWork: Sync success")
                userPreferences.setSynced(true)
                Result.success()
            }
            is SyncResult.NeedAuthorization -> {
                Log.w("GoogleSheetsSyncWorker", "doWork: Authorization needed. Failing background sync.")
                Result.failure()
            }
            is SyncResult.Error -> {
                Log.e("GoogleSheetsSyncWorker", "doWork: Sync failed with exception", syncResult.exception)
                if (syncResult.exception is java.io.IOException) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }
}
