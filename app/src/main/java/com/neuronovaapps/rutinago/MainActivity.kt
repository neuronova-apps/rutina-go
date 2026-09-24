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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.neuronovaapps.rutinago.data.preferences.ThemeMode
import com.neuronovaapps.rutinago.data.preferences.ThemePreferencesRepository
import com.neuronovaapps.rutinago.ui.navigation.RutinaGoApp
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val themePreferences by lazy { ThemePreferencesRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.DAY)
            val darkTheme = themeMode.usesDarkTheme(systemDarkTheme)
            var textScale by rememberSaveable { mutableFloatStateOf(1f) }
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

            RutinaGoTheme(darkTheme = darkTheme, textScale = textScale) {
                RutinaGoApp(
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        coroutineScope.launch { themePreferences.setThemeMode(mode) }
                    },
                    textScale = textScale,
                    onTextScaleChange = { textScale = it },
                )
            }
        }
    }
}
