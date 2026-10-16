package com.diego.twodo.domain.model

sealed interface ModelStatus {
    data object NotDownloaded : ModelStatus
    data object WaitingForNetwork : ModelStatus
    data class Downloading(val progress: Int) : ModelStatus
    data class Ready(val modelPath: String) : ModelStatus
    data class Error(val message: String) : ModelStatus
}
