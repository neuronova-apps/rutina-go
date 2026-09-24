package com.neuronovaapps.rutinago.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkPetroleumBlue,
    onPrimary = DarkOnPetroleumBlue,
    primaryContainer = DarkPetroleumContainer,
    onPrimaryContainer = DarkOnPetroleumContainer,
    secondary = DarkPetroleumBlue,
    onSecondary = DarkOnPetroleumBlue,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkOutline,
    error = DarkError,
)

private val LightColorScheme = lightColorScheme(
    primary = PetroleumBlue,
    onPrimary = OnPetroleumBlue,
    primaryContainer = PetroleumContainer,
    onPrimaryContainer = OnPetroleumContainer,
    secondary = PetroleumBlue,
    onSecondary = OnPetroleumBlue,
    background = LightBackground,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMuted,
    outline = LightOutline,
    error = LightError,
)

private val RutinaGoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun RutinaGoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val typography = remember(textScale) { rutinaGoTypography(textScale) }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = typography,
        shapes = RutinaGoShapes,
        content = content,
    )
}
