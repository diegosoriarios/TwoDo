package com.diego.twodo.domain.manager

import android.app.ActivityManager
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.diego.twodo.data.classifier.LocalLlmClassifier
import com.diego.twodo.data.worker.ModelDownloadWorker
import com.diego.twodo.domain.model.ModelStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localLlmClassifier: LocalLlmClassifier,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val workManager = WorkManager.getInstance(context)

    private val _modelStatus = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    init {
        checkInitialStatus()
        observeWorkManager()
    }

    fun isLowRamDevice(): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            memoryInfo.lowMemory || (memoryInfo.totalMem < LOW_RAM_THRESHOLD_BYTES)
        } catch (e: Exception) {
            false
        }
    }

    fun getModelFile(): File {
        val modelsDir = File(context.filesDir, "models")
        return File(modelsDir, ModelDownloadWorker.MODEL_FILE_NAME)
    }

    private fun getPartialModelFile(): File {
        val modelsDir = File(context.filesDir, "models")
        return File(modelsDir, "${ModelDownloadWorker.MODEL_FILE_NAME}.tmp")
    }

    /** Whether the local LLM can run on this device; low-RAM devices gracefully fall back to heuristics. */
    fun canUseLlm(): Boolean = _modelStatus.value is ModelStatus.Ready && !isLowRamDevice()

    fun startDownload(
        modelUrl: String = DEFAULT_MODEL_URL,
        expectedChecksum: String? = null,
        requireWifi: Boolean = true,
    ) {
        val networkType = if (requireWifi) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .build()

        val inputData = Data.Builder()
            .putString(ModelDownloadWorker.KEY_MODEL_URL, modelUrl)
            .apply {
                if (!expectedChecksum.isNullOrBlank()) {
                    putString(ModelDownloadWorker.KEY_EXPECTED_CHECKSUM, expectedChecksum)
                }
            }
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
            .setInputData(inputData)
            .addTag(ModelDownloadWorker.WORK_TAG)
            .build()

        _modelStatus.value = ModelStatus.Downloading(0)
        workManager.enqueueUniqueWork(
            ModelDownloadWorker.WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            downloadRequest,
        )
    }

    /** True when a partial download exists that can be resumed. */
    fun hasPartialDownload(): Boolean = getPartialModelFile().let { it.exists() && it.length() > 0 }

    /** Stops the running download while keeping the partial file so it can be resumed later. */
    fun pauseDownload() {
        workManager.cancelUniqueWork(ModelDownloadWorker.WORK_TAG)
        checkInitialStatus()
    }

    /** Resumes a paused (or interrupted) download; the worker picks up from the partial file. */
    fun resumeDownload() {
        startDownload()
    }

    fun cancelDownload() {
        workManager.cancelUniqueWork(ModelDownloadWorker.WORK_TAG)
        getPartialModelFile().delete()
        checkInitialStatus()
    }

    fun deleteModel(): Boolean {
        val file = getModelFile()
        val deleted = if (file.exists()) file.delete() else true
        localLlmClassifier.setModelInitialized(false)
        _modelStatus.value = ModelStatus.NotDownloaded
        return deleted
    }

    private fun checkInitialStatus() {
        val file = getModelFile()
        if (file.exists() && file.length() > 0) {
            localLlmClassifier.setModelInitialized(true)
            _modelStatus.value = ModelStatus.Ready(file.absolutePath)
        } else {
            localLlmClassifier.setModelInitialized(false)
            _modelStatus.value = ModelStatus.NotDownloaded
        }
    }

    private fun observeWorkManager() {
        scope.launch {
            workManager.getWorkInfosByTagFlow(ModelDownloadWorker.WORK_TAG).collect { workInfos ->
                val workInfo = workInfos.pickActiveWorkInfo() ?: return@collect

                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> {
                        _modelStatus.value = ModelStatus.WaitingForNetwork
                    }
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                        _modelStatus.value = ModelStatus.Downloading(progress)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val file = getModelFile()
                        if (file.exists()) {
                            localLlmClassifier.setModelInitialized(true)
                            _modelStatus.value = ModelStatus.Ready(file.absolutePath)
                        } else {
                            _modelStatus.value = ModelStatus.Error("Downloaded file not found")
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString(ModelDownloadWorker.KEY_ERROR)
                            ?: "Model download failed"
                        _modelStatus.value = ModelStatus.Error(error)
                    }
                    WorkInfo.State.CANCELLED -> {
                        checkInitialStatus()
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Picks the WorkInfo that best represents the current download run, preferring live
     * states over terminal ones. With [ExistingWorkPolicy.REPLACE] the tag query can
     * briefly contain stale terminal entries alongside the fresh work.
     */
    private fun List<WorkInfo>.pickActiveWorkInfo(): WorkInfo? {
        val priority = mapOf(
            WorkInfo.State.RUNNING to 0,
            WorkInfo.State.ENQUEUED to 1,
        )
        return minByOrNull { priority[it.state] ?: 2 }
    }

    companion object {
        private const val LOW_RAM_THRESHOLD_BYTES = 3L * 1024 * 1024 * 1024 // 3 GB
        private const val BACKOFF_DELAY_SECONDS = 30L

        /**
         * Default model: Qwen3 0.6B from the LiteRT Community collection.
         * Ungated (anonymous download allowed) and supports Range requests for resume.
         */
        const val DEFAULT_MODEL_URL =
            "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm"
    }
}
