package com.diego.twodo.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diego.twodo.data.preferences.OnboardingPreferences
import com.diego.twodo.domain.manager.ModelManager
import com.diego.twodo.domain.model.ModelStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val modelManager: ModelManager,
    private val onboardingPreferences: OnboardingPreferences,
) : ViewModel() {

    val modelStatus: StateFlow<ModelStatus> = modelManager.modelStatus

    val isLowRamDevice: Boolean
        get() = modelManager.isLowRamDevice()

    val hasPartialDownload: Boolean
        get() = modelManager.hasPartialDownload()

    fun downloadModel() = modelManager.startDownload()

    fun pauseDownload() = modelManager.pauseDownload()

    fun resumeDownload() = modelManager.resumeDownload()

    fun cancelDownload() = modelManager.cancelDownload()

    fun skip() {
        viewModelScope.launch { onboardingPreferences.setOnboardingDismissed(true) }
    }
}
