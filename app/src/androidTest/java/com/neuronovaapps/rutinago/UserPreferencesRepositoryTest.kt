package com.neuronovaapps.rutinago

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.neuronovaapps.rutinago.data.preferences.TextScaleMode
import com.neuronovaapps.rutinago.data.preferences.ThemeMode
import com.neuronovaapps.rutinago.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPreferencesRepositoryTest {
    @Test
    fun accessibilityPreferences_areRestoredFromDataStore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = UserPreferencesRepository(context)

        try {
            repository.setThemeMode(ThemeMode.NIGHT)
            repository.setTextScaleMode(TextScaleMode.VERY_LARGE)
            repository.setHighContrast(true)

            val saved = repository.preferences.first {
                it.themeMode == ThemeMode.NIGHT &&
                    it.textScaleMode == TextScaleMode.VERY_LARGE &&
                    it.highContrast
            }
            assertEquals(ThemeMode.NIGHT, saved.themeMode)
            assertEquals(TextScaleMode.VERY_LARGE, saved.textScaleMode)
            assertTrue(saved.highContrast)

            repository.setHighContrast(false)
            val restored = repository.preferences.first { !it.highContrast }
            assertEquals(ThemeMode.NIGHT, restored.themeMode)
            assertFalse(restored.highContrast)
        } finally {
            repository.setThemeMode(ThemeMode.DAY)
            repository.setTextScaleMode(TextScaleMode.NORMAL)
            repository.setHighContrast(false)
        }
    }
}
