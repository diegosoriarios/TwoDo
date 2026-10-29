package com.diego.twodo.domain.manager

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
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
import com.diego.twodo.domain.model.ModelSpec
import com.diego.twodo.domain.model.ModelStatus
import com.diego.twodo.domain.repository.ClassificationScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localLlmClassifier: LocalLlmClassifier,
    private val classificationScheduler: ClassificationScheduler,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val workManager = WorkManager.getInstance(context)
    private val warmUpInProgress = AtomicBoolean(false)

    private val _modelStatus = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    private val _runtimeFailed = MutableStateFlow(false)
    val runtimeFailed: StateFlow<Boolean> = _runtimeFailed.asStateFlow()

    private val _partialDownload = MutableStateFlow(false)
    val partialDownload: StateFlow<Boolean> = _partialDownload.asStateFlow()

    init {
        checkInitialStatus()
        observeWorkManager()
    }

    fun isLowRamDevice(): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            activityManager.isLowRamDevice ||
                memoryInfo.lowMemory ||
                (memoryInfo.totalMem < LOW_RAM_THRESHOLD_BYTES)
        } catch (e: Exception) {
            false
        }
    }

    /** LiteRT-LM only ships arm64-v8a and x86_64 native libraries. */
    fun isDeviceSupported(): Boolean =
        Build.SUPPORTED_ABIS.any { it == "arm64-v8a" || it == "x86_64" }

    fun getModelFile(): File = File(modelsDirectory(), ModelSpec.FILE_NAME)

    fun getPartialModelFile(): File = File(modelsDirectory(), "${ModelSpec.FILE_NAME}.tmp")

    fun expectedModelSizeBytes(): Long = ModelSpec.SIZE_BYTES

    fun installedModelSizeBytes(): Long = getModelFile().length()

    /**
     * Whether the local LLM can run on this device. Low-RAM and unsupported-ABI devices
     * gracefully fall back to heuristics.
     */
    val canUseLlm: StateFlow<Boolean> = combine(
        _modelStatus,
        _runtimeFailed,
    ) { status, runtimeFailed ->
        status is ModelStatus.Ready &&
            !runtimeFailed &&
            !isLowRamDevice() &&
            isDeviceSupported()
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    fun startDownload(
        modelUrl: String = ModelSpec.DOWNLOAD_URL,
        expectedChecksum: String? = ModelSpec.SHA256,
        requireWifi: Boolean = true,
    ) {
        _runtimeFailed.value = false
        _partialDownload.value = hasPartialDownload()
        val networkType = if (requireWifi) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .build()

        val inputData = Data.Builder()
            .putString(ModelDownloadWorker.KEY_MODEL_URL, modelUrl)
            .putLong(ModelDownloadWorker.KEY_EXPECTED_SIZE, ModelSpec.SIZE_BYTES)
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
    fun resumeDownload(requireWifi: Boolean = true) {
        startDownload(requireWifi = requireWifi)
    }

    fun cancelDownload() {
        workManager.cancelUniqueWork(ModelDownloadWorker.WORK_TAG)
        workManager.pruneWork()
        getPartialModelFile().delete()
        _partialDownload.value = false
        _runtimeFailed.value = false
        checkInitialStatus()
    }

    fun deleteModel(): Boolean {
        workManager.cancelUniqueWork(ModelDownloadWorker.WORK_TAG)
        workManager.pruneWork()
        val deleted = getModelFile().let { if (it.exists()) it.delete() else true }
        getPartialModelFile().delete()
        localLlmClassifier.setModelPath(null)
        _partialDownload.value = false
        _runtimeFailed.value = false
        _modelStatus.value = ModelStatus.NotDownloaded
        return deleted
    }

    fun retryRuntime() {
        val file = getModelFile()
        if (!file.exists()) return
        if (!warmUpInProgress.compareAndSet(false, true)) return
        scope.launch {
            try {
                val ready = localLlmClassifier.reload()
                if (ready) {
                    _runtimeFailed.value = false
                    classificationScheduler.schedule()
                } else {
                    _runtimeFailed.value = true
                }
            } finally {
                warmUpInProgress.set(false)
            }
        }
    }

    private fun checkInitialStatus() {
        deleteLegacyModelFiles()
        _partialDownload.value = hasPartialDownload()

        val file = getModelFile()
        if (file.exists() && file.length() > 0) {
            markReady(file)
        } else {
            localLlmClassifier.setModelPath(null)
            _modelStatus.value = ModelStatus.NotDownloaded
        }
    }

    private fun markReady(file: File) {
        val usable = isDeviceSupported() && !isLowRamDevice()
        localLlmClassifier.setModelPath(if (usable) file.absolutePath else null)
        _partialDownload.value = false
        _modelStatus.value = ModelStatus.Ready(file.absolutePath)
        if (usable) {
            warmUpAsync()
        }
    }

    private fun warmUpAsync() {
        if (!warmUpInProgress.compareAndSet(false, true)) return
        scope.launch {
            try {
                val ready = localLlmClassifier.warmUp()
                if (ready) {
                    _runtimeFailed.value = false
                    classificationScheduler.schedule()
                } else {
                    Log.w(TAG, "LiteRT-LM warm-up failed")
                    _runtimeFailed.value = true
                }
            } finally {
                warmUpInProgress.set(false)
            }
        }
    }

    private fun modelsDirectory(): File =
        File(context.filesDir, MODELS_DIRECTORY).apply { if (!exists()) mkdirs() }

    private fun deleteLegacyModelFiles() {
        val directory = modelsDirectory()
        ModelSpec.LEGACY_FILE_NAMES.forEach { name -> File(directory, name).delete() }
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
                        _partialDownload.value = hasPartialDownload()
                        _modelStatus.value = ModelStatus.Downloading(progress)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val file = getModelFile()
                        if (file.exists() && file.length() > 0) {
                            markReady(file)
                        } else {
                            checkInitialStatus()
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        val file = getModelFile()
                        if (file.exists() && file.length() > 0) {
                            markReady(file)
                        } else {
                            _partialDownload.value = hasPartialDownload()
                            val status = _modelStatus.value
                            if (status is ModelStatus.Downloading || status is ModelStatus.WaitingForNetwork) {
                                val error = workInfo.outputData.getString(ModelDownloadWorker.KEY_ERROR)
                                    ?: "Model download failed"
                                _modelStatus.value = ModelStatus.Error(error)
                            } else {
                                checkInitialStatus()
                            }
                        }
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
        private const val TAG = "ModelManager"
        private const val MODELS_DIRECTORY = "models"
        private const val LOW_RAM_THRESHOLD_BYTES = 3L * 1024 * 1024 * 1024 // 3 GB
        private const val BACKOFF_DELAY_SECONDS = 30L
    }
}
