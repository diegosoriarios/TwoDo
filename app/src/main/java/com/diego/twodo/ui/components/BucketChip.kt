package com.diego.twodo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.diego.twodo.ui.theme.ControlHeights
import com.diego.twodo.ui.theme.Radius
import com.diego.twodo.ui.theme.Spacing
import com.diego.twodo.ui.theme.SurfaceDark
import com.diego.twodo.ui.theme.TypeTokens

@Composable
fun BucketChip(
    label: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(ControlHeights.chip)
            .clip(RoundedCornerShape(Radius.control))
            .background(SurfaceDark)
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClockIcon(tint = iconTint)
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(text = label, style = TypeTokens.ChipLabel)
    }
}

@Composable
fun ClockIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val stroke = 1.5.dp.toPx()
        val radius = (size.minDimension - stroke) / 2f
        drawCircle(
            color = tint,
            radius = radius,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawLine(
            color = tint,
            start = Offset(center.x, center.y),
            end = Offset(center.x, center.y - radius * 0.6f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(center.x, center.y),
            end = Offset(center.x + radius * 0.5f, center.y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
