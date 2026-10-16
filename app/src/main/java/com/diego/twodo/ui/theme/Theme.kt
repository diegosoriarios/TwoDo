package com.diego.twodo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TwoDoColorScheme = darkColorScheme(
    primary = Coral,
    onPrimary = TextPrimary,
    primaryContainer = Coral,
    onPrimaryContainer = TextPrimary,
    secondary = TextSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = SurfaceDark,
    onSecondaryContainer = TextPrimary,
    tertiary = Coral,
    onTertiary = TextPrimary,
    tertiaryContainer = SurfaceDark,
    onTertiaryContainer = TextSecondary,
    background = Charcoal,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Charcoal,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceDark,
    surfaceContainerHighest = SurfaceDark,
    outline = TextSecondary,
    outlineVariant = Color(0x33A0A0A0),
    error = Coral,
    onError = TextPrimary,
)

private val TwoDoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

@Composable
fun TwoDoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TwoDoColorScheme,
        typography = Typography,
        shapes = TwoDoShapes,
        content = content,
    )
}
