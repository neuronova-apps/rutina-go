package com.neuronovaapps.rutinago

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.neuronovaapps.rutinago.data.preferences.TextScaleMode
import com.neuronovaapps.rutinago.data.preferences.ThemeMode
import com.neuronovaapps.rutinago.data.preferences.AppMode
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

    @Test
    fun profilesAndMode_areCreatedEditedSelectedAndRestoredWithoutLosingData() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = UserPreferencesRepository(context)
        val firstId = "profile-test-first"
        val secondId = "profile-test-second"

        repository.createProfile("Inicial", firstId)
        repository.renameProfile(firstId, "Nombre editado")
        repository.createProfile("Segundo perfil", secondId)
        repository.selectProfile(secondId)
        repository.setAppMode(AppMode.ADVANCED)

        val advanced = repository.preferences.first {
            it.activeProfileId == secondId && it.appMode == AppMode.ADVANCED
        }
        assertEquals("Nombre editado", advanced.profiles.first { it.id == firstId }.name)
        assertEquals("Segundo perfil", advanced.activeProfile?.name)

        repository.setAppMode(AppMode.SIMPLE)
        val restored = UserPreferencesRepository(context).preferences.first {
            it.appMode == AppMode.SIMPLE && it.activeProfileId == secondId
        }
        assertEquals(2, restored.profiles.count { it.id == firstId || it.id == secondId })
        assertEquals("Nombre editado", restored.profiles.first { it.id == firstId }.name)
    }
}
