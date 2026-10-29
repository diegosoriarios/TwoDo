package com.diego.twodo.ui.model

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton as Material3TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diego.twodo.R
import com.diego.twodo.domain.model.ModelSpec
import com.diego.twodo.domain.model.ModelStatus
import com.diego.twodo.ui.components.PrimaryButton
import com.diego.twodo.ui.components.TextButton
import com.diego.twodo.ui.theme.Coral
import com.diego.twodo.ui.theme.Spacing
import com.diego.twodo.ui.theme.SurfaceDark
import com.diego.twodo.ui.theme.TextPrimary
import com.diego.twodo.ui.theme.TextSecondary
import com.diego.twodo.ui.theme.TypeTokens

@Composable
fun ModelManagerScreen(
    onBack: () -> Unit,
    viewModel: ModelManagerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showCancelDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.model_manager_back),
                    tint = TextPrimary,
                )
            }
            Text(
                text = stringResource(R.string.model_manager_title),
                style = TypeTokens.ScreenTitle.copy(fontSize = 22.sp),
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ModelCard(
                state = state,
                expectedSize = Formatter.formatShortFileSize(context, state.expectedBytes),
                installedSize = Formatter.formatShortFileSize(context, state.installedBytes),
            )

            StatusSection(
                state = state,
                onDownloadWifi = viewModel::downloadOverWifi,
                onDownloadMobile = viewModel::downloadOverMobileData,
                onPause = viewModel::pauseDownload,
                onResumeWifi = { viewModel.resumeDownload(requireWifi = true) },
                onResumeMobile = { viewModel.resumeDownload(requireWifi = false) },
                onCancel = { showCancelDialog = true },
                onDelete = { showDeleteDialog = true },
                onRetryRuntime = viewModel::retryRuntime,
            )

            if (state.isLowRamDevice) {
                WarningText(stringResource(R.string.model_manager_low_ram_warning))
            }
            if (!state.isDeviceSupported) {
                WarningText(stringResource(R.string.model_manager_unsupported_warning))
            }
            if (state.runtimeFailed) {
                WarningText(stringResource(R.string.model_manager_runtime_failed))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.model_manager_delete_confirm_title)) },
            text = { Text(stringResource(R.string.model_manager_delete_confirm_text)) },
            confirmButton = {
                Material3TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteModel()
                }) {
                    Text(
                        text = stringResource(R.string.model_manager_delete_confirm_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                Material3TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.composer_cancel))
                }
            },
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(R.string.model_manager_cancel_confirm_title)) },
            text = { Text(stringResource(R.string.model_manager_cancel_confirm_text)) },
            confirmButton = {
                Material3TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.cancelDownload()
                }) {
                    Text(
                        text = stringResource(R.string.model_manager_cancel_confirm_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                Material3TextButton(onClick = { showCancelDialog = false }) {
                    Text(stringResource(R.string.composer_cancel))
                }
            },
        )
    }
}

@Composable
private fun ModelCard(
    state: ModelManagerUiState,
    expectedSize: String,
    installedSize: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = ModelSpec.DISPLAY_NAME,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = stringResource(R.string.model_manager_license_format, ModelSpec.LICENSE),
                style = TypeTokens.Caption,
            )
            val sizeText = if (state.status is ModelStatus.Ready) {
                stringResource(R.string.model_manager_installed_size_format, installedSize)
            } else {
                stringResource(R.string.model_manager_download_size_format, expectedSize)
            }
            Text(text = sizeText, style = TypeTokens.Caption)
        }
    }
}

@Composable
private fun StatusSection(
    state: ModelManagerUiState,
    onDownloadWifi: () -> Unit,
    onDownloadMobile: () -> Unit,
    onPause: () -> Unit,
    onResumeWifi: () -> Unit,
    onResumeMobile: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onRetryRuntime: () -> Unit,
) {
    val status = state.status
    val canDownload = state.isDeviceSupported

    when (status) {
        ModelStatus.NotDownloaded -> {
            if (state.hasPartialDownload) {
                Text(
                    text = stringResource(R.string.model_manager_status_paused),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                if (canDownload) {
                    PrimaryButton(
                        text = stringResource(R.string.model_manager_resume_wifi),
                        onClick = onResumeWifi,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        text = stringResource(R.string.model_manager_resume_mobile),
                        onClick = onResumeMobile,
                    )
                }
                TextButton(
                    text = stringResource(R.string.model_manager_cancel_download),
                    onClick = onCancel,
                    labelColor = TextSecondary,
                )
            } else {
                Text(
                    text = stringResource(R.string.model_manager_status_not_downloaded),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    text = stringResource(R.string.model_manager_not_downloaded_description),
                    style = TypeTokens.Body,
                )
                if (canDownload) {
                    PrimaryButton(
                        text = stringResource(R.string.model_manager_download_wifi),
                        onClick = onDownloadWifi,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        text = stringResource(R.string.model_manager_download_mobile),
                        onClick = onDownloadMobile,
                    )
                }
            }
        }

        ModelStatus.WaitingForNetwork -> {
            Text(
                text = stringResource(R.string.model_manager_status_waiting),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = stringResource(R.string.model_manager_waiting_description),
                style = TypeTokens.Body,
            )
            TextButton(
                text = stringResource(R.string.model_manager_use_any_network),
                onClick = onDownloadMobile,
            )
            TextButton(
                text = stringResource(R.string.model_manager_pause),
                onClick = onPause,
                labelColor = TextSecondary,
            )
        }

        is ModelStatus.Downloading -> {
            Text(
                text = stringResource(R.string.model_manager_status_downloading),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = stringResource(R.string.model_manager_progress_format, status.progress),
                style = TypeTokens.Body,
            )
            LinearProgressIndicator(
                progress = { status.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = Coral,
            )
            TextButton(
                text = stringResource(R.string.model_manager_pause),
                onClick = onPause,
            )
            TextButton(
                text = stringResource(R.string.model_manager_cancel_download),
                onClick = onCancel,
                labelColor = TextSecondary,
            )
        }

        is ModelStatus.Ready -> {
            Text(
                text = stringResource(R.string.model_manager_status_ready),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = stringResource(R.string.model_manager_ready_description),
                style = TypeTokens.Body,
            )
            if (state.runtimeFailed) {
                PrimaryButton(
                    text = stringResource(R.string.model_manager_retry_runtime),
                    onClick = onRetryRuntime,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TextButton(
                text = stringResource(R.string.model_manager_delete),
                onClick = onDelete,
                labelColor = MaterialTheme.colorScheme.error,
            )
        }

        is ModelStatus.Error -> {
            Text(
                text = stringResource(R.string.model_manager_status_error),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = stringResource(R.string.model_manager_error_format, status.message),
                style = TypeTokens.Body,
            )
            if (canDownload) {
                PrimaryButton(
                    text = stringResource(R.string.model_manager_retry_wifi),
                    onClick = onDownloadWifi,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    text = stringResource(R.string.model_manager_download_mobile),
                    onClick = onDownloadMobile,
                )
            }
            if (state.hasPartialDownload) {
                TextButton(
                    text = stringResource(R.string.model_manager_cancel_download),
                    onClick = onCancel,
                    labelColor = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun WarningText(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = TextSecondary,
        )
        Spacer(modifier = Modifier.size(Spacing.xs))
        Text(
            text = text,
            style = TypeTokens.Caption,
            modifier = Modifier.weight(1f),
        )
    }
}
