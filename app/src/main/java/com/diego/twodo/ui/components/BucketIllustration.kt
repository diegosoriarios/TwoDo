package com.diego.twodo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.diego.twodo.R
import com.diego.twodo.ui.theme.Coral
import com.diego.twodo.ui.theme.IllustrationBaseline
import com.diego.twodo.ui.theme.IllustrationNavy
import com.diego.twodo.ui.theme.NavyLight
import com.diego.twodo.ui.theme.TextSecondary

private const val CANVAS_WIDTH = 312f
private const val CANVAS_HEIGHT = 250f
private const val BASELINE_Y = 200f

@Composable
fun BucketIllustration(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.illustration_cd)
    val textMeasurer = rememberTextMeasurer()
    val labels = listOf(
        stringResource(R.string.setup_chip_two_min),
        stringResource(R.string.setup_chip_thirty_min),
        stringResource(R.string.setup_chip_long),
    )
    val labelStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        color = TextSecondary,
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
    ) {
        val sx = size.width / CANVAS_WIDTH
        val sy = size.height / CANVAS_HEIGHT

        drawLine(
            color = IllustrationBaseline,
            start = Offset(0f, BASELINE_Y * sy),
            end = Offset(size.width, BASELINE_Y * sy),
            strokeWidth = 2f * sy,
        )

        drawCircle(
            color = Coral,
            radius = 36f * sx,
            center = Offset(38f * sx, 164f * sy),
        )
        drawCircle(
            color = IllustrationNavy,
            radius = 72f * sx,
            center = Offset(150f * sx, 128f * sy),
        )
        drawArc(
            color = Coral,
            startAngle = -85f,
            sweepAngle = 70f,
            useCenter = true,
            topLeft = Offset(78f * sx, 56f * sy),
            size = Size(144f * sx, 144f * sy),
        )

        drawRoundRect(
            color = IllustrationNavy,
            topLeft = Offset(228f * sx, 28f * sy),
            size = Size(84f * sx, 172f * sy),
            cornerRadius = CornerRadius(8f * sx, 8f * sx),
        )
        val topSection = Path().apply {
            moveTo(228f * sx, 80f * sy)
            lineTo(228f * sx, 36f * sy)
            quadraticTo(228f * sx, 28f * sy, 236f * sx, 28f * sy)
            lineTo(304f * sx, 28f * sy)
            quadraticTo(312f * sx, 28f * sy, 312f * sx, 36f * sy)
            lineTo(312f * sx, 80f * sy)
            close()
        }
        drawPath(topSection, NavyLight)
        drawRoundRect(
            color = Coral,
            topLeft = Offset(252f * sx, 92f * sy),
            size = Size(36f * sx, 8f * sy),
            cornerRadius = CornerRadius(4f * sx, 4f * sx),
        )

        val labelCentersX = listOf(38f, 150f, 270f)
        labels.forEachIndexed { index, label ->
            val measured = textMeasurer.measure(AnnotatedString(label), labelStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    labelCentersX[index] * sx - measured.size.width / 2f,
                    (BASELINE_Y + 10f) * sy,
                ),
            )
        }
    }
}
