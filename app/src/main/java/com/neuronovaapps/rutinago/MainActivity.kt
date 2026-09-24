package com.neuronovaapps.rutinago

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.neuronovaapps.rutinago.ui.navigation.RutinaGoApp
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            var darkTheme by rememberSaveable { mutableStateOf(systemDarkTheme) }
            var textScale by rememberSaveable { mutableFloatStateOf(1f) }

            RutinaGoTheme(darkTheme = darkTheme, textScale = textScale) {
                RutinaGoApp(
                    darkTheme = darkTheme,
                    onDarkThemeChange = { darkTheme = it },
                    textScale = textScale,
                    onTextScaleChange = { textScale = it },
                )
            }
        }
    }
}
