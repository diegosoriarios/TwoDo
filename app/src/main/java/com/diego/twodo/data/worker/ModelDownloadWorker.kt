package com.diego.twodo.data.worker

import android.content.Context
import android.os.StatFs
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.diego.twodo.domain.model.ModelSpec
import com.diego.twodo.notifications.ModelDownloadNotification
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelUrl = inputData.getString(KEY_MODEL_URL)
            ?: return@withContext Result.failure(workDataOf(KEY_ERROR to "Missing model URL"))
        val expectedChecksum = inputData.getString(KEY_EXPECTED_CHECKSUM)
        val expectedSize = inputData.getLong(KEY_EXPECTED_SIZE, ModelSpec.SIZE_BYTES)

        val modelsDir = File(applicationContext.filesDir, MODELS_DIRECTORY).apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(modelsDir, ModelSpec.FILE_NAME)
        val tempFile = File(modelsDir, "${ModelSpec.FILE_NAME}.tmp")

        ModelDownloadNotification.createChannel(applicationContext)
        publishForeground(progress = null)

        try {
            val connection = URL(modelUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS

            val alreadyDownloaded = if (tempFile.exists()) tempFile.length() else 0L
            if (alreadyDownloaded > 0L) {
                connection.setRequestProperty("Range", "bytes=$alreadyDownloaded-")
            }
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK &&
                connection.responseCode != HttpURLConnection.HTTP_PARTIAL
            ) {
                return@withContext if (connection.responseCode in SERVER_ERROR_RANGE &&
                    runAttemptCount < MAX_RETRIES
                ) {
                    Result.retry()
                } else {
                    Result.failure(
                        workDataOf(KEY_ERROR to "HTTP Error: ${connection.responseCode} ${connection.responseMessage}"),
                    )
                }
            }

            val resumed = alreadyDownloaded > 0L && connection.responseCode == HttpURLConnection.HTTP_PARTIAL
            if (alreadyDownloaded > 0L && !resumed) {
                tempFile.delete()
            }

            val totalSize = when {
                resumed -> alreadyDownloaded + connection.contentLengthLong
                connection.contentLengthLong > 0 -> connection.contentLengthLong
                expectedSize > 0 -> expectedSize
                else -> 0L
            }
            val remainingBytes = (totalSize - alreadyDownloaded).coerceAtLeast(0L)
            if (totalSize > 0 && !hasEnoughStorage(remainingBytes)) {
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR to "Not enough free storage for the model"),
                )
            }

            connection.inputStream.use { inputStream ->
                FileOutputStream(tempFile, resumed).use { outputStream ->
                    val buffer = ByteArray(BUFFER_SIZE_BYTES)
                    var bytesRead: Int
                    var totalRead = if (resumed) alreadyDownloaded else 0L
                    var lastReportedProgress = -1

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            return@withContext Result.failure(workDataOf(KEY_ERROR to "Download stopped"))
                        }

                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        if (totalSize > 0) {
                            val progress = ((totalRead * 100) / totalSize).toInt().coerceIn(0, 100)
                            if (progress != lastReportedProgress) {
                                lastReportedProgress = progress
                                setProgress(workDataOf(KEY_PROGRESS to progress))
                                publishForeground(progress)
                            }
                        }
                    }
                }
            }

            if (expectedSize > 0 && tempFile.length() != expectedSize) {
                tempFile.delete()
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR to "Downloaded model has an unexpected size"),
                )
            }

            if (!expectedChecksum.isNullOrBlank()) {
                val downloadedHash = tempFile.sha256()
                if (!downloadedHash.equals(expectedChecksum.trim(), ignoreCase = true)) {
                    tempFile.delete()
                    return@withContext Result.failure(
                        workDataOf(KEY_ERROR to "Checksum verification failed"),
                    )
                }
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            Result.success(workDataOf(KEY_MODEL_PATH to targetFile.absolutePath))
        } catch (e: Exception) {
            Log.w(TAG, "Model download failed", e)
            if (e is IOException && e !is MalformedURLException && runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure(workDataOf(KEY_ERROR to (e.localizedMessage ?: "Download failed")))
            }
        }
    }

    private suspend fun publishForeground(progress: Int?) {
        try {
            setForeground(
                ForegroundInfo(
                    ModelDownloadNotification.NOTIFICATION_ID,
                    ModelDownloadNotification.build(applicationContext, progress),
                ),
            )
        } catch (throwable: Throwable) {
            Log.w(TAG, "Unable to publish download foreground notification", throwable)
        }
    }

    private fun hasEnoughStorage(remainingBytes: Long): Boolean {
        return try {
            val stat = StatFs(applicationContext.filesDir.absolutePath)
            stat.availableBytes >= remainingBytes + STORAGE_HEADROOM_BYTES
        } catch (e: Exception) {
            true
        }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { stream ->
            val buffer = ByteArray(BUFFER_SIZE_BYTES)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val KEY_MODEL_URL = "key_model_url"
        const val KEY_EXPECTED_CHECKSUM = "key_expected_checksum"
        const val KEY_EXPECTED_SIZE = "key_expected_size"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        const val KEY_MODEL_PATH = "model_path"
        const val WORK_TAG = "twodo_model_download"

        private const val TAG = "ModelDownloadWorker"
        private const val MODELS_DIRECTORY = "models"
        private const val MAX_RETRIES = 3
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val BUFFER_SIZE_BYTES = 8192
        private const val STORAGE_HEADROOM_BYTES = 64L * 1024 * 1024
        private val SERVER_ERROR_RANGE = 500..599
    }
}
