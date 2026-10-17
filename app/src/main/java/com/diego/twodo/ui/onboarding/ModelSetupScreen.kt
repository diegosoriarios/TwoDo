package com.diego.twodo.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diego.twodo.R
import com.diego.twodo.ui.components.BucketChip
import com.diego.twodo.ui.components.BottomSheet
import com.diego.twodo.ui.components.PrimaryButton
import com.diego.twodo.ui.components.TextButton
import com.diego.twodo.ui.theme.ControlHeights
import com.diego.twodo.ui.theme.Coral
import com.diego.twodo.ui.theme.Spacing
import com.diego.twodo.ui.theme.TextSecondary
import com.diego.twodo.ui.theme.TypeTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelSetupScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.setupUiState.collectAsState()
    val isLowRamDevice = remember { viewModel.isLowRamDevice }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(top = Spacing.lg),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                BucketChip(
                    label = stringResource(R.string.setup_chip_two_min),
                    iconTint = Coral,
                )
                BucketChip(
                    label = stringResource(R.string.setup_chip_thirty_min),
                    iconTint = TextSecondary,
                )
                BucketChip(
                    label = stringResource(R.string.setup_chip_long),
                    iconTint = TextSecondary,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xl))
            Text(
                text = stringResource(R.string.setup_title),
                style = TypeTokens.ScreenTitle,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = stringResource(R.string.setup_description),
                style = TypeTokens.Body,
            )
            if (isLowRamDevice) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary,
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = stringResource(R.string.setup_low_ram),
                        style = TypeTokens.Caption,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        SetupSheet(state = state, viewModel = viewModel)
    }
}

@Composable
private fun SetupSheet(state: SetupUiState, viewModel: OnboardingViewModel) {
    BottomSheet {
        val caption = when (state) {
            is SetupUiState.Downloading ->
                if (state.overMobileData) stringResource(R.string.setup_caption_downloading_mobile)
                else stringResource(R.string.setup_caption_downloading_wifi)
            SetupUiState.Done -> stringResource(R.string.setup_caption_done)
            SetupUiState.Skipped -> stringResource(R.string.setup_caption_skipped)
            is SetupUiState.Error -> stringResource(R.string.setup_error_format, state.message)
            SetupUiState.Idle -> stringResource(R.string.setup_download_hint)
        }
        Text(
            text = caption,
            style = TypeTokens.Caption,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ControlHeights.captionMinHeight),
        )
        Spacer(modifier = Modifier.height(Spacing.md))

        val label: String
        val onClick: (() -> Unit)?
        val progress: Float?
        when (state) {
            is SetupUiState.Downloading -> {
                label = stringResource(R.string.setup_progress_format, state.progress)
                onClick = null
                progress = state.progress / 100f
            }
            SetupUiState.Done -> {
                label = stringResource(R.string.setup_done)
                onClick = viewModel::acknowledgeDone
                progress = null
            }
            SetupUiState.Skipped -> {
                label = stringResource(R.string.setup_download)
                onClick = viewModel::downloadOverWifi
                progress = null
            }
            is SetupUiState.Error -> {
                label = stringResource(R.string.setup_retry)
                onClick = viewModel::retryDownload
                progress = null
            }
            SetupUiState.Idle -> {
                label = stringResource(R.string.setup_download)
                onClick = viewModel::downloadOverWifi
                progress = null
            }
        }
        PrimaryButton(
            text = label,
            onClick = onClick,
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Spacing.xs))
        TextButton(
            text = stringResource(R.string.setup_download_mobile),
            onClick = viewModel::downloadOverMobileData,
            enabled = state !is SetupUiState.Downloading,
        )
        Spacer(modifier = Modifier.height(Spacing.xxs))
        TextButton(
            text = stringResource(R.string.setup_skip),
            onClick = viewModel::skip,
            labelColor = TextSecondary,
        )
    }
}
