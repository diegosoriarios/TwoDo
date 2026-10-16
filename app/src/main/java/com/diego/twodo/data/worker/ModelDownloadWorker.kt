package com.diego.twodo.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.security.MessageDigest

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelUrl = inputData.getString(KEY_MODEL_URL)
            ?: return@withContext Result.failure(workDataOf(KEY_ERROR to "Missing model URL"))
        val expectedChecksum = inputData.getString(KEY_EXPECTED_CHECKSUM)

        val modelsDir = File(applicationContext.filesDir, "models").apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(modelsDir, MODEL_FILE_NAME)
        val tempFile = File(modelsDir, "$MODEL_FILE_NAME.tmp")

        try {
            val connection = URL(modelUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS

            // Resume a partial download left behind by a paused/stopped attempt
            val alreadyDownloaded = if (tempFile.exists()) tempFile.length() else 0L
            if (alreadyDownloaded > 0L) {
                connection.setRequestProperty("Range", "bytes=$alreadyDownloaded-")
            }
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK &&
                connection.responseCode != HttpURLConnection.HTTP_PARTIAL
            ) {
                return@withContext if (connection.responseCode in SERVER_ERROR_RANGE && runAttemptCount < MAX_RETRIES) {
                    Result.retry()
                } else {
                    Result.failure(
                        workDataOf(KEY_ERROR to "HTTP Error: ${connection.responseCode} ${connection.responseMessage}"),
                    )
                }
            }

            // Server ignored the Range request; discard the partial file and start over
            val resumed = alreadyDownloaded > 0L && connection.responseCode == HttpURLConnection.HTTP_PARTIAL
            if (alreadyDownloaded > 0L && !resumed) {
                tempFile.delete()
            }

            val fileLength = if (resumed) alreadyDownloaded + connection.contentLengthLong else connection.contentLengthLong

            connection.inputStream.use { inputStream ->
                FileOutputStream(tempFile, resumed).use { outputStream ->
                    val buffer = ByteArray(BUFFER_SIZE_BYTES)
                    var bytesRead: Int
                    var totalRead = if (resumed) alreadyDownloaded else 0L
                    var lastReportedProgress = -1

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            // Keep the partial file so a later run can resume from it
                            return@withContext Result.failure(workDataOf(KEY_ERROR to "Download stopped"))
                        }

                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        if (fileLength > 0) {
                            val progress = ((totalRead * 100) / fileLength).toInt()
                            if (progress != lastReportedProgress) {
                                lastReportedProgress = progress
                                setProgress(workDataOf(KEY_PROGRESS to progress.coerceIn(0, 100)))
                            }
                        }
                    }
                }
            }

            // SHA-256 verification over the completed file if a checksum was provided
            if (!expectedChecksum.isNullOrBlank()) {
                val downloadedHash = tempFile.sha256()
                if (!downloadedHash.equals(expectedChecksum.trim(), ignoreCase = true)) {
                    tempFile.delete()
                    return@withContext Result.failure(
                        workDataOf(KEY_ERROR to "Checksum verification failed"),
                    )
                }
            }

            // Atomically replace target file
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            Result.success(workDataOf(KEY_MODEL_PATH to targetFile.absolutePath))
        } catch (e: Exception) {
            // Keep the partial file so a retry/resume can pick up where this attempt left off
            if (e is IOException && e !is MalformedURLException && runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure(workDataOf(KEY_ERROR to (e.localizedMessage ?: "Download failed")))
            }
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
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        const val KEY_MODEL_PATH = "model_path"
        const val MODEL_FILE_NAME = "twodo_llm_model.bin"
        const val WORK_TAG = "twodo_model_download"

        private const val MAX_RETRIES = 3
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val BUFFER_SIZE_BYTES = 8192
        private val SERVER_ERROR_RANGE = 500..599
    }
}
