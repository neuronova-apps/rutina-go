package com.neuronovaapps.rutinago

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.neuronovaapps.rutinago.data.preferences.TextScaleMode
import com.neuronovaapps.rutinago.data.preferences.ThemeMode
import com.neuronovaapps.rutinago.data.preferences.UserPreferences
import com.neuronovaapps.rutinago.ui.theme.rutinaGoColorScheme
import com.neuronovaapps.rutinago.ui.theme.rutinaGoTypography
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPreferencesTest {
    @Test
    fun missingOrUnknownThemePreference_defaultsToDay() {
        assertEquals(ThemeMode.DAY, ThemeMode.fromStorage(null))
        assertEquals(ThemeMode.DAY, ThemeMode.fromStorage("unknown"))
    }

    @Test
    fun storedValues_restoreEveryThemeMode() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromStorage(mode.storageValue))
        }
    }

    @Test
    fun dayAndNightIgnoreSystemWhileSystemFollowsIt() {
        assertFalse(ThemeMode.DAY.usesDarkTheme(systemInDarkTheme = true))
        assertTrue(ThemeMode.NIGHT.usesDarkTheme(systemInDarkTheme = false))
        assertFalse(ThemeMode.SYSTEM.usesDarkTheme(systemInDarkTheme = false))
        assertTrue(ThemeMode.SYSTEM.usesDarkTheme(systemInDarkTheme = true))
    }

    @Test
    fun accessibilityDefaults_areNormalTextAndStandardContrast() {
        val preferences = UserPreferences()

        assertEquals(TextScaleMode.NORMAL, preferences.textScaleMode)
        assertEquals(1f, preferences.textScaleMode.scaleFactor)
        assertFalse(preferences.highContrast)
        assertEquals(TextScaleMode.NORMAL, TextScaleMode.fromStorage(null))
    }

    @Test
    fun textScaleValues_restoreAndIncreaseGlobalTypographyInThreeLevels() {
        TextScaleMode.entries.forEach { mode ->
            assertEquals(mode, TextScaleMode.fromStorage(mode.storageValue))
        }
        assertEquals(listOf(1f, 1.12f, 1.24f), TextScaleMode.entries.map { it.scaleFactor })

        val typographySizes = TextScaleMode.entries.map { mode ->
            rutinaGoTypography(mode.scaleFactor).bodyLarge.fontSize.value
        }
        assertTrue(typographySizes.zipWithNext().all { (current, next) -> next > current })
    }

    @Test
    fun highContrastSchemesStrengthenAndPreserveLegibilityInLightAndDarkModes() {
        val light = rutinaGoColorScheme(darkTheme = false, highContrast = false)
        val highContrastLight = rutinaGoColorScheme(darkTheme = false, highContrast = true)
        val dark = rutinaGoColorScheme(darkTheme = true, highContrast = false)
        val highContrastDark = rutinaGoColorScheme(darkTheme = true, highContrast = true)

        assertNotEquals(light.background, highContrastLight.background)
        assertNotEquals(light.outline, highContrastLight.outline)
        assertNotEquals(dark.background, highContrastDark.background)
        assertNotEquals(dark.outline, highContrastDark.outline)
        assertHighContrastPairs(highContrastLight)
        assertHighContrastPairs(highContrastDark)
    }

    private fun assertHighContrastPairs(scheme: ColorScheme) {
        assertTrue(contrastRatio(scheme.onBackground, scheme.background) >= 7.0)
        assertTrue(contrastRatio(scheme.onSurfaceVariant, scheme.surfaceVariant) >= 7.0)
        assertTrue(contrastRatio(scheme.onPrimary, scheme.primary) >= 7.0)
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val foregroundLuminance = foreground.luminance().toDouble()
        val backgroundLuminance = background.luminance().toDouble()
        return (max(foregroundLuminance, backgroundLuminance) + 0.05) /
            (min(foregroundLuminance, backgroundLuminance) + 0.05)
    }
}
