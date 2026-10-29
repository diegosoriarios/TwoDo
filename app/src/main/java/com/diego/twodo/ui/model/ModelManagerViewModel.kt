package com.diego.twodo.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diego.twodo.domain.manager.ModelManager
import com.diego.twodo.domain.model.ModelStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ModelManagerUiState(
    val status: ModelStatus,
    val hasPartialDownload: Boolean,
    val isLowRamDevice: Boolean,
    val isDeviceSupported: Boolean,
    val installedBytes: Long,
    val expectedBytes: Long,
    val runtimeFailed: Boolean,
)

@HiltViewModel
class ModelManagerViewModel @Inject constructor(
    private val modelManager: ModelManager,
) : ViewModel() {

    val uiState: StateFlow<ModelManagerUiState> = combine(
        modelManager.modelStatus,
        modelManager.runtimeFailed,
        modelManager.partialDownload,
    ) { status, runtimeFailed, hasPartialDownload ->
        buildState(status, runtimeFailed, hasPartialDownload)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildState(
            modelManager.modelStatus.value,
            modelManager.runtimeFailed.value,
            modelManager.partialDownload.value,
        ),
    )

    fun downloadOverWifi() {
        modelManager.startDownload(requireWifi = true)
    }

    fun downloadOverMobileData() {
        modelManager.startDownload(requireWifi = false)
    }

    fun pauseDownload() {
        modelManager.pauseDownload()
    }

    fun resumeDownload(requireWifi: Boolean) {
        modelManager.resumeDownload(requireWifi)
    }

    fun cancelDownload() {
        modelManager.cancelDownload()
    }

    fun deleteModel() {
        modelManager.deleteModel()
    }

    fun retryRuntime() {
        modelManager.retryRuntime()
    }

    private fun buildState(
        status: ModelStatus,
        runtimeFailed: Boolean,
        hasPartialDownload: Boolean,
    ) = ModelManagerUiState(
        status = status,
        hasPartialDownload = hasPartialDownload,
        isLowRamDevice = modelManager.isLowRamDevice(),
        isDeviceSupported = modelManager.isDeviceSupported(),
        installedBytes = modelManager.installedModelSizeBytes(),
        expectedBytes = modelManager.expectedModelSizeBytes(),
        runtimeFailed = runtimeFailed,
    )
}
