package com.diego.twodo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diego.twodo.data.preferences.OnboardingPreferences
import com.diego.twodo.domain.manager.ModelManager
import com.diego.twodo.domain.model.ModelStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val onboardingPreferences: OnboardingPreferences,
    modelManager: ModelManager,
) : ViewModel() {

    val showModelSetup: StateFlow<Boolean> = combine(
        onboardingPreferences.isOnboardingDismissed,
        modelManager.modelStatus,
    ) { dismissed, status -> !dismissed && status !is ModelStatus.Ready }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = true)

    val isWelcomeCompleted: StateFlow<Boolean> = onboardingPreferences.isWelcomeCompleted

    fun completeWelcome() {
        viewModelScope.launch { onboardingPreferences.setWelcomeCompleted(true) }
    }
}
