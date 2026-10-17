package com.diego.twodo.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.diego.twodo.R
import com.diego.twodo.ui.components.BucketIllustration
import com.diego.twodo.ui.components.PrimaryButton
import com.diego.twodo.ui.theme.ControlHeights
import com.diego.twodo.ui.theme.Spacing
import com.diego.twodo.ui.theme.TextPrimary
import com.diego.twodo.ui.theme.TypeTokens

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg),
    ) {
        Spacer(modifier = Modifier.height(Spacing.lg))
        BucketIllustration(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(312f / 250f),
        )
        Spacer(modifier = Modifier.height(Spacing.xl))
        Text(
            text = stringResource(R.string.welcome_title),
            style = TypeTokens.ScreenTitle,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.welcome_body),
            style = TypeTokens.Body,
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(R.string.welcome_start),
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            height = ControlHeights.primaryLarge,
            trailingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = TextPrimary,
                )
            },
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
