package com.neuronovaapps.rutinago

import com.neuronovaapps.rutinago.data.preferences.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun missingOrUnknownPreference_defaultsToDay() {
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
}
