package prasad.vennam.moneypilot.worker

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
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

@HiltWorker
class ModelDownloadWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
    ) : CoroutineWorker(context, params) {
        private val downloadClient =
            OkHttpClient
                .Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(180, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()

        override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo(0f)

        private fun createForegroundInfo(progress: Float): ForegroundInfo {
            val channelId = "llm_model_download_channel"
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel =
                    NotificationChannel(
                        channelId,
                        "Model Downloads",
                        NotificationManager.IMPORTANCE_LOW,
                    )
                notificationManager.createNotificationChannel(channel)
            }

            val progressPercent = (progress * 100).toInt()
            val notification =
                NotificationCompat
                    .Builder(applicationContext, channelId)
                    .setContentTitle("Downloading AI Model")
                    .setContentText("Downloading local AI scanner model ($progressPercent%)")
                    .setSmallIcon(prasad.vennam.moneypilot.R.mipmap.ic_launcher)
                    .setProgress(100, progressPercent, false)
                    .setOngoing(true)
                    .build()

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(1006, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                ForegroundInfo(1006, notification)
            }
        }

        override suspend fun doWork(): Result {
            val modelUrl = inputData.getString("model_url") ?: return Result.failure()
            val modelFileName = inputData.getString("model_file_name") ?: return Result.failure()

            Log.d(TAG, "Starting background LLM download: $modelFileName from $modelUrl")
            try {
                setForeground(createForegroundInfo(0f))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start work in foreground, continuing as background task", e)
            }

            val destinationDir = applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir
            val modelFile = File(destinationDir, modelFileName)
            val tempFile = File(destinationDir, "$modelFileName.tmp")

            try {
                val request =
                    Request
                        .Builder()
                        .url(modelUrl)
                        .header("User-Agent", "Mozilla/5.0 MoneyPilot/1.0")
                        .build()

                downloadClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Server error: ${response.code} ${response.message}")
                        return Result.failure()
                    }

                    val body = response.body ?: return Result.failure()
                    val totalBytes = body.contentLength()
                    Log.d(TAG, "Content length: $totalBytes bytes")

                    tempFile.parentFile?.mkdirs()
                    if (tempFile.exists()) tempFile.delete()

                    var bytesCopied = 0L
                    val buffer = ByteArray(64 * 1024)
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
                                    if (now - lastProgressUpdate > 500 || progress >= 1f) {
                                        setProgress(workDataOf("progress" to progress))
                                        try {
                                            setForeground(createForegroundInfo(progress))
                                        } catch (e: Exception) {
                                            // Ignore transient notification update issues
                                        }
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
                        Log.d(TAG, "Rename successful.")
                        return Result.success()
                    } else {
                        Log.e(TAG, "Rename failed. Attempting manual copy...")
                        try {
                            tempFile.copyTo(modelFile, overwrite = true)
                            tempFile.delete()
                            Log.d(TAG, "Manual copy successful.")
                            return Result.success()
                        } catch (copyEx: Exception) {
                            Log.e(TAG, "Manual copy failed", copyEx)
                            return Result.failure()
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network/IO error during download", e)
                if (tempFile.exists()) tempFile.delete()
                return Result.failure()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during download", e)
                if (tempFile.exists()) tempFile.delete()
                return Result.failure()
            }
        }

        companion object {
            private const val TAG = "ModelDownloadWorker"
        }
    }
