package com.diego.twodo.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diego.twodo.data.preferences.OnboardingPreferences
import com.diego.twodo.domain.manager.ModelManager
import com.diego.twodo.domain.model.ModelStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val modelManager: ModelManager,
    private val onboardingPreferences: OnboardingPreferences,
) : ViewModel() {

    private val overMobileData = MutableStateFlow(false)
    private val doneAcknowledged = MutableStateFlow(false)

    val isLowRamDevice: Boolean
        get() = modelManager.isLowRamDevice()

    val setupUiState: StateFlow<SetupUiState> = combine(
        modelManager.modelStatus,
        onboardingPreferences.isSetupSkipped,
        overMobileData,
        doneAcknowledged,
    ) { status, skipped, mobile, doneAck ->
        when (status) {
            is ModelStatus.Ready -> if (doneAck) SetupUiState.Idle else SetupUiState.Done
            is ModelStatus.Downloading -> SetupUiState.Downloading(status.progress, mobile)
            is ModelStatus.WaitingForNetwork -> SetupUiState.Downloading(0, false)
            is ModelStatus.Error -> SetupUiState.Error(status.message)
            ModelStatus.NotDownloaded -> if (skipped) SetupUiState.Skipped else SetupUiState.Idle
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SetupUiState.Idle)

    init {
        viewModelScope.launch {
            modelManager.modelStatus.collect { status ->
                if (status !is ModelStatus.Ready) doneAcknowledged.value = false
            }
        }
    }

    fun downloadOverWifi() {
        overMobileData.value = false
        clearSkipped()
        modelManager.startDownload(requireWifi = true)
    }

    fun downloadOverMobileData() {
        overMobileData.value = true
        clearSkipped()
        modelManager.startDownload(requireWifi = false)
    }

    fun retryDownload() {
        if (overMobileData.value) downloadOverMobileData() else downloadOverWifi()
    }

    fun acknowledgeDone() {
        doneAcknowledged.value = true
    }

    fun skip() {
        modelManager.cancelDownload()
        viewModelScope.launch {
            onboardingPreferences.setSetupSkipped(true)
            onboardingPreferences.setOnboardingDismissed(true)
        }
    }

    private fun clearSkipped() {
        viewModelScope.launch { onboardingPreferences.setSetupSkipped(false) }
    }
}
