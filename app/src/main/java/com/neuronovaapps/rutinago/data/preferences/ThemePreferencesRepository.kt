package com.neuronovaapps.rutinago.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
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

private val Context.appearanceDataStore by preferencesDataStore(name = "appearance_preferences")

class ThemePreferencesRepository(context: Context) {
    private val appContext = context.applicationContext

    val themeMode: Flow<ThemeMode> = appContext.appearanceDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> ThemeMode.fromStorage(preferences[THEME_MODE_KEY]) }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        appContext.appearanceDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.storageValue
        }
    }

    private companion object {
        val THEME_MODE_KEY: Preferences.Key<String> = stringPreferencesKey("theme_mode")
    }
}
