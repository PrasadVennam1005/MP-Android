package prasad.vennam.moneypilot.util

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.repository.DataManagementRepository
import prasad.vennam.moneypilot.util.AnalyticsHelper

@HiltWorker
class GoogleSheetsSyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val repository: DataManagementRepository,
        private val userPreferences: UserPreferences,
        private val analyticsHelper: AnalyticsHelper,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            Log.d("GoogleSheetsSyncWorker", "doWork: Sync worker started")
            val appContext = applicationContext

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
                    userPreferences = userPreferences,
                    analyticsHelper = analyticsHelper,
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
