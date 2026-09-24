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
            val preferences by userPreferencesRepository.preferences.collectAsState(
                initial = UserPreferences(),
            )
            val darkTheme = preferences.themeMode.usesDarkTheme(systemDarkTheme)
            val coroutineScope = rememberCoroutineScope()

            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                    navigationBarStyle = if (darkTheme) {
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
                RutinaGoApp(
                    themeMode = preferences.themeMode,
                    onThemeModeChange = { mode ->
                        coroutineScope.launch { userPreferencesRepository.setThemeMode(mode) }
                    },
                    textScaleMode = preferences.textScaleMode,
                    onTextScaleModeChange = { mode ->
                        coroutineScope.launch { userPreferencesRepository.setTextScaleMode(mode) }
                    },
                    highContrast = preferences.highContrast,
                    onHighContrastChange = { enabled ->
                        coroutineScope.launch { userPreferencesRepository.setHighContrast(enabled) }
                    },
                )
            }
        }
    }
}
