package prasad.vennam.moneypilot.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.squareup.moshi.Moshi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.data.repository.DataManagementRepository

@HiltWorker
class GoogleSheetsSyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val repository: DataManagementRepository,
        private val userPreferences: UserPreferences,
        private val analyticsHelper: AnalyticsHelper,
        private val moshi: Moshi,
    ) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val channelId = "sync_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Data Synchronization",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Syncing with Google Sheets")
            .setContentText("Keeping your financial data safe...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                1007,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(1007, notification)
        }
    }

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

        // Show foreground notification to user
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            Log.w("GoogleSheetsSyncWorker", "Failed to set foreground state", e)
        }

        Log.d("GoogleSheetsSyncWorker", "doWork: Performing 2-way sheet sync for ${userData.email}")

        // Show foreground notification to user
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            Log.w("GoogleSheetsSyncWorker", "Failed to set foreground state", e)
        }

        val syncResult =
            GoogleSheetsSyncHelper.performTwoWaySync(
                    context = appContext,
                    email = userData.email,
                    repository = repository,
                    userPreferences = userPreferences,
                    analyticsHelper = analyticsHelper,
                    moshi = moshi,
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
