package com.diego.twodo.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton as Material3TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.diego.twodo.ui.theme.ControlHeights
import com.diego.twodo.ui.theme.Coral
import com.diego.twodo.ui.theme.Radius
import com.diego.twodo.ui.theme.TypeTokens

@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = Coral,
    enabled: Boolean = true,
) {
    Material3TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(ControlHeights.secondary),
        shape = RoundedCornerShape(Radius.control),
        colors = ButtonDefaults.textButtonColors(
            contentColor = labelColor,
            disabledContentColor = labelColor.copy(alpha = 0.4f),
        ),
    ) {
        Text(
            text = text,
            style = TypeTokens.TextButtonLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
