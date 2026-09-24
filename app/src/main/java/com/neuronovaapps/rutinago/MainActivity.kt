package com.neuronovaapps.rutinago

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.neuronovaapps.rutinago.data.preferences.UserPreferences
import com.neuronovaapps.rutinago.data.preferences.UserPreferencesRepository
import com.neuronovaapps.rutinago.ui.navigation.RutinaGoApp
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val userPreferencesRepository by lazy {
        UserPreferencesRepository(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            val storedPreferences by userPreferencesRepository.preferences.collectAsState(initial = null)
            val preferences = storedPreferences ?: UserPreferences()
            val darkTheme = preferences.themeMode.usesDarkTheme(systemDarkTheme)
            val useDarkSystemBars = darkTheme || preferences.highContrast
            val coroutineScope = rememberCoroutineScope()

            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (useDarkSystemBars) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                    navigationBarStyle = if (useDarkSystemBars) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.argb(0x80, 0x1B, 0x1B, 0x1B),
                        )
                    },
                )
            }

            RutinaGoTheme(
                darkTheme = darkTheme,
                textScale = preferences.textScaleMode.scaleFactor,
                highContrast = preferences.highContrast,
            ) {
                if (storedPreferences != null) RutinaGoApp(
                    preferences = preferences,
                    onThemeModeChange = { mode ->
                        coroutineScope.launch { userPreferencesRepository.setThemeMode(mode) }
                    },
                    onTextScaleModeChange = { mode ->
                        coroutineScope.launch { userPreferencesRepository.setTextScaleMode(mode) }
                    },
                    onHighContrastChange = { enabled ->
                        coroutineScope.launch { userPreferencesRepository.setHighContrast(enabled) }
                    },
                    onAppModeChange = { mode ->
                        coroutineScope.launch { userPreferencesRepository.setAppMode(mode) }
                    },
                    onCreateProfile = { name ->
                        coroutineScope.launch { userPreferencesRepository.createProfile(name) }
                    },
                    onRenameProfile = { id, name ->
                        coroutineScope.launch { userPreferencesRepository.renameProfile(id, name) }
                    },
                    onSelectProfile = { id ->
                        coroutineScope.launch { userPreferencesRepository.selectProfile(id) }
                    },
                )
            }
        }
    }
}
