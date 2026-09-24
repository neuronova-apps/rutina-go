package com.neuronovaapps.rutinago.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkPetroleumBlue,
    onPrimary = DarkOnPetroleumBlue,
    primaryContainer = DarkPetroleumContainer,
    onPrimaryContainer = DarkOnPetroleumContainer,
    secondary = DarkPetroleumBlue,
    onSecondary = DarkOnPetroleumBlue,
    secondaryContainer = DarkPetroleumContainer,
    onSecondaryContainer = DarkOnPetroleumContainer,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceContainerHighest = DarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = DarkError,
)

private val LightColorScheme = lightColorScheme(
    primary = PetroleumBlue,
    onPrimary = OnPetroleumBlue,
    primaryContainer = PetroleumContainer,
    onPrimaryContainer = OnPetroleumContainer,
    secondary = PetroleumBlue,
    onSecondary = OnPetroleumBlue,
    secondaryContainer = PetroleumContainer,
    onSecondaryContainer = OnPetroleumContainer,
    background = LightBackground,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMuted,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightBackground,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceVariant,
    surfaceContainerHighest = LightSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = LightError,
)

private val HighContrastColorScheme = darkColorScheme(
    primary = HighContrastAccent,
    onPrimary = HighContrastOnAccent,
    primaryContainer = HighContrastSurfaceVariant,
    onPrimaryContainer = HighContrastInk,
    secondary = HighContrastAccent,
    onSecondary = HighContrastOnAccent,
    secondaryContainer = HighContrastAccent,
    onSecondaryContainer = HighContrastOnAccent,
    background = HighContrastBackground,
    onBackground = HighContrastInk,
    surface = HighContrastSurface,
    onSurface = HighContrastInk,
    surfaceVariant = HighContrastSurfaceVariant,
    onSurfaceVariant = HighContrastInk,
    surfaceContainerLowest = HighContrastBackground,
    surfaceContainerLow = HighContrastSurface,
    surfaceContainer = HighContrastSurface,
    surfaceContainerHigh = HighContrastSurfaceVariant,
    surfaceContainerHighest = HighContrastSurfaceVariant,
    outline = HighContrastOutline,
    outlineVariant = HighContrastOutline,
    error = DarkError,
    onError = HighContrastOnAccent,
    errorContainer = DarkError,
    onErrorContainer = HighContrastOnAccent,
)

internal val LocalHighContrast = staticCompositionLocalOf { false }

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
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val typography = remember(textScale) { rutinaGoTypography(textScale) }
    val colorScheme = rutinaGoColorScheme(darkTheme, highContrast)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = RutinaGoShapes,
    ) {
        CompositionLocalProvider(
            LocalHighContrast provides highContrast,
            content = content,
        )
    }
}

internal fun rutinaGoColorScheme(
    darkTheme: Boolean,
    highContrast: Boolean,
): ColorScheme = when {
    highContrast -> HighContrastColorScheme
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}
