package com.diego.twodo.ui.onboarding

sealed interface SetupUiState {
    data object Idle : SetupUiState
    data class Downloading(val progress: Int, val overMobileData: Boolean) : SetupUiState
    data object Done : SetupUiState
    data object Skipped : SetupUiState
    data class Error(val message: String) : SetupUiState
}
