package com.diego.twodo.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.diego.twodo.R
import com.diego.twodo.ui.components.PrimaryButton
import com.diego.twodo.ui.theme.Coral
import com.diego.twodo.ui.theme.IllustrationNavy
import com.diego.twodo.ui.theme.TextPrimary
import com.diego.twodo.ui.theme.TextSecondary

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            OnboardingIllustration(modifier = Modifier.fillMaxWidth().height(300.dp))
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.welcome_body),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.weight(1f))
            PrimaryButton(
                text = stringResource(R.string.welcome_start),
                onClick = onStart,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OnboardingIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = IllustrationNavy,
            radius = w * 0.30f,
            center = Offset(w * 0.32f, h * 0.34f),
        )
        drawCircle(
            color = IllustrationNavy,
            radius = w * 0.24f,
            center = Offset(w * 0.74f, h * 0.48f),
        )
        drawCircle(
            color = Coral,
            radius = w * 0.34f,
            center = Offset(w * 0.56f, h * 0.40f),
        )
        val triangle = Path().apply {
            moveTo(w * 0.16f, h * 0.90f)
            lineTo(w * 0.50f, h * 0.54f)
            lineTo(w * 0.84f, h * 0.90f)
            close()
        }
        drawPath(triangle, IllustrationNavy)
    }
}
