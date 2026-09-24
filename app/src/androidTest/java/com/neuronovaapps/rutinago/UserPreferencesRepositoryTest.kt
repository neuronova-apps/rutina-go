package com.neuronovaapps.rutinago

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.neuronovaapps.rutinago.data.preferences.TextScaleMode
import com.neuronovaapps.rutinago.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPreferencesRepositoryTest {
    @Test
    fun accessibilityPreferences_areRestoredFromDataStore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = UserPreferencesRepository(context)

        try {
            repository.setTextScaleMode(TextScaleMode.VERY_LARGE)
            repository.setHighContrast(true)

            val saved = repository.preferences.first {
                it.textScaleMode == TextScaleMode.VERY_LARGE && it.highContrast
            }
            assertEquals(TextScaleMode.VERY_LARGE, saved.textScaleMode)
            assertTrue(saved.highContrast)
        } finally {
            repository.setTextScaleMode(TextScaleMode.NORMAL)
            repository.setHighContrast(false)
        }
    }
}
