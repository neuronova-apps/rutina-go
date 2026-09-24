package com.neuronovaapps.rutinago.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class ThemeMode(val storageValue: String) {
    DAY("day"),
    NIGHT("night"),
    SYSTEM("system");

    fun usesDarkTheme(systemInDarkTheme: Boolean): Boolean = when (this) {
        DAY -> false
        NIGHT -> true
        SYSTEM -> systemInDarkTheme
    }

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: DAY
    }
}

enum class TextScaleMode(
    val storageValue: String,
    val scaleFactor: Float,
) {
    NORMAL("normal", 1f),
    LARGE("large", 1.12f),
    VERY_LARGE("very_large", 1.24f);

    companion object {
        fun fromStorage(value: String?): TextScaleMode =
            entries.firstOrNull { it.storageValue == value } ?: NORMAL
    }
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DAY,
    val textScaleMode: TextScaleMode = TextScaleMode.NORMAL,
    val highContrast: Boolean = false,
)

// Keep the existing store name so preferences written in Phase 1.1 remain valid.
private val Context.userPreferencesDataStore by preferencesDataStore(name = "appearance_preferences")

class UserPreferencesRepository(context: Context) {
    private val appContext = context.applicationContext

    val preferences: Flow<UserPreferences> = appContext.userPreferencesDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            UserPreferences(
                themeMode = ThemeMode.fromStorage(preferences[THEME_MODE_KEY]),
                textScaleMode = TextScaleMode.fromStorage(preferences[TEXT_SCALE_KEY]),
                highContrast = preferences[HIGH_CONTRAST_KEY] ?: false,
            )
        }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        appContext.userPreferencesDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.storageValue
        }
    }

    suspend fun setTextScaleMode(mode: TextScaleMode) {
        appContext.userPreferencesDataStore.edit { preferences ->
            preferences[TEXT_SCALE_KEY] = mode.storageValue
        }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        appContext.userPreferencesDataStore.edit { preferences ->
            preferences[HIGH_CONTRAST_KEY] = enabled
        }
    }

    private companion object {
        val THEME_MODE_KEY: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        val TEXT_SCALE_KEY: Preferences.Key<String> = stringPreferencesKey("text_scale")
        val HIGH_CONTRAST_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("high_contrast")
    }
}
