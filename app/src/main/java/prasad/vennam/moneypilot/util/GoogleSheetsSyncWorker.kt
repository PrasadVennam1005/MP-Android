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
    params: WorkerParameters,
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

        Log.d("GoogleSheetsSyncWorker", "doWork: Performing 2-way sheet sync for ${userData.email}")
        val syncResult =
            GoogleSheetsSyncHelper.performTwoWaySync(
                context = appContext,
                email = userData.email,
                repository = repository,
                spreadsheetId = spreadsheetId,
                isRestore = false,
                onSpreadsheetIdFound = { id ->
                    userPreferences.saveSpreadsheetId(id)
                },
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
            is SyncResult.NoBackupFound -> {
                Log.w("GoogleSheetsSyncWorker", "doWork: No backup found.")
                Result.failure()
            }
        }
    }
}
