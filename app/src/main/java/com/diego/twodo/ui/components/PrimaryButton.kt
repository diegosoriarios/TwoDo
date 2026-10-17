package com.diego.twodo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.diego.twodo.ui.theme.Coral
import com.diego.twodo.ui.theme.ControlHeights
import com.diego.twodo.ui.theme.ElevatedSurface
import com.diego.twodo.ui.theme.Radius
import com.diego.twodo.ui.theme.Spacing
import com.diego.twodo.ui.theme.TextPrimary
import com.diego.twodo.ui.theme.TypeTokens

@Composable
fun PrimaryButton(
    text: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    height: Dp = ControlHeights.primary,
    trailingIcon: (@Composable () -> Unit)? = null,
    progress: Float? = null,
) {
    if (progress == null) {
        Button(
            onClick = onClick ?: {},
            enabled = onClick != null,
            modifier = modifier.height(height),
            shape = RoundedCornerShape(Radius.control),
            colors = ButtonDefaults.buttonColors(
                containerColor = Coral,
                contentColor = TextPrimary,
            ),
        ) {
            Text(
                text = text,
                style = TypeTokens.PrimaryButtonLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(Spacing.sm))
                trailingIcon()
            }
        }
    } else {
        Box(
            modifier = modifier
                .height(height)
                .clip(RoundedCornerShape(Radius.control))
                .background(ElevatedSurface),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(Coral),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = text,
                    style = TypeTokens.PrimaryButtonLabel,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    trailingIcon()
                }
            }
        }
    }
}
