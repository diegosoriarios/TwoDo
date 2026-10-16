package com.diego.twodo.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diego.twodo.R
import com.diego.twodo.domain.model.ModelStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSetupScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val status by viewModel.modelStatus.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "⚡⏱️🏗️", style = MaterialTheme.typography.displayMedium)
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.setup_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            ModelStatusCard(
                status = status,
                hasPartialDownload = viewModel.hasPartialDownload,
                onDownload = viewModel::downloadModel,
                onDownloadOverMobileData = viewModel::downloadModelOverMobileData,
                onResume = viewModel::resumeDownload,
                onPause = viewModel::pauseDownload,
                onCancel = viewModel::cancelDownload,
                onSkip = viewModel::skip,
            )
            if (viewModel.isLowRamDevice) {
                LowRamCard()
            }
        }
    }
}

@Composable
private fun ModelStatusCard(
    status: ModelStatus,
    hasPartialDownload: Boolean,
    onDownload: () -> Unit,
    onDownloadOverMobileData: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onCancel: () -> Unit,
    onSkip: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (status) {
                ModelStatus.NotDownloaded -> {
                    if (hasPartialDownload) {
                        Text(
                            text = stringResource(R.string.setup_partial),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.setup_resume))
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.setup_download_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.setup_download))
                        }
                        TextButton(onClick = onDownloadOverMobileData) {
                            Text(stringResource(R.string.setup_download_mobile))
                        }
                    }
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.setup_skip))
                    }
                }
                ModelStatus.WaitingForNetwork -> {
                    Text(
                        text = stringResource(R.string.setup_waiting_network),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onPause) {
                            Text(stringResource(R.string.setup_pause))
                        }
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.setup_cancel))
                        }
                    }
                    TextButton(onClick = onDownloadOverMobileData) {
                        Text(stringResource(R.string.setup_download_mobile))
                    }
                }
                is ModelStatus.Downloading -> {
                    Text(
                        text = stringResource(R.string.setup_progress_format, status.progress),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LinearProgressIndicator(
                        progress = { status.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onPause) {
                            Text(stringResource(R.string.setup_pause))
                        }
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.setup_cancel))
                        }
                    }
                }
                is ModelStatus.Ready -> {
                    Text(
                        text = stringResource(R.string.setup_ready),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                is ModelStatus.Error -> {
                    Text(
                        text = stringResource(R.string.setup_error_format, status.message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.setup_retry))
                    }
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.setup_skip))
                    }
                }
            }
        }
    }
}

@Composable
private fun LowRamCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = Icons.Filled.Warning, contentDescription = null)
            Text(
                text = stringResource(R.string.setup_low_ram),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
