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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.neuronovaapps.rutinago.data.preferences.UserPreferences
import com.neuronovaapps.rutinago.data.preferences.UserPreferencesRepository
import com.neuronovaapps.rutinago.domain.catalog.CatalogUiState
import com.neuronovaapps.rutinago.domain.execution.ProfileProgress
import com.neuronovaapps.rutinago.ui.navigation.RutinaGoApp
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first

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
            val catalogState by (application as RutinaGoApplication)
                .catalogContainer.uiState.collectAsState(initial = CatalogUiState.Loading)
            val preferences = storedPreferences ?: UserPreferences()
            val catalogContainer = (application as RutinaGoApplication).catalogContainer
            val activeProfileId = preferences.activeProfile?.id
            val userRoutinesFlow = remember(activeProfileId) {
                activeProfileId?.let(catalogContainer.userRoutines::activeRoutines)
                    ?: flowOf(emptyList())
            }
            val userRoutines by userRoutinesFlow.collectAsState(initial = emptyList())
            val activeExecutionFlow = remember(activeProfileId) {
                activeProfileId?.let(catalogContainer.executions::activeExecution)
                    ?: flowOf(null)
            }
            val progressFlow = remember(activeProfileId) {
                activeProfileId?.let(catalogContainer.executions::progress)
                    ?: flowOf(ProfileProgress())
            }
            val activeExecution by activeExecutionFlow.collectAsState(initial = null)
            val progress by progressFlow.collectAsState(initial = ProfileProgress())
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
                    catalogState = catalogState,
                    userRoutines = userRoutines,
                    activeExecution = activeExecution,
                    progress = progress,
                    onSaveUserRoutine = { draft ->
                        val profileId = requireNotNull(preferences.activeProfile?.id) {
                            "Selecciona un perfil antes de guardar."
                        }
                        catalogContainer.userRoutines.save(profileId, draft)
                    },
                    onArchiveUserRoutine = { routineId ->
                        val profileId = requireNotNull(preferences.activeProfile?.id) {
                            "Selecciona un perfil antes de archivar."
                        }
                        catalogContainer.userRoutines.archive(profileId, routineId)
                    },
                    onDeleteUserRoutine = { routineId ->
                        val profileId = requireNotNull(preferences.activeProfile?.id) {
                            "Selecciona un perfil antes de eliminar."
                        }
                        catalogContainer.userRoutines.delete(profileId, routineId)
                    },
                    onStartExecution = { routineId ->
                        val profileId = requireNotNull(preferences.activeProfile?.id) {
                            "Selecciona un perfil antes de iniciar."
                        }
                        catalogContainer.executions.start(profileId, routineId)
                    },
                    onLoadExecution = { executionId ->
                        catalogContainer.executions.execution(executionId).first()
                            ?.takeIf { it.profileId == preferences.activeProfile?.id }
                    },
                    onCompleteExecutionStep = { executionId, position ->
                        val profileId = requireNotNull(preferences.activeProfile?.id)
                        catalogContainer.executions.completeStep(profileId, executionId, position)
                    },
                    onSkipExecutionStep = { executionId, position ->
                        val profileId = requireNotNull(preferences.activeProfile?.id)
                        catalogContainer.executions.skipStep(profileId, executionId, position)
                    },
                    onFinishPartialExecution = { executionId ->
                        val profileId = requireNotNull(preferences.activeProfile?.id)
                        catalogContainer.executions.finishPartial(profileId, executionId)
                    },
                    onAbandonExecution = { executionId ->
                        val profileId = requireNotNull(preferences.activeProfile?.id)
                        catalogContainer.executions.abandon(profileId, executionId)
                    },
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
                        coroutineScope.launch {
                            val currentProfileId = preferences.activeProfile?.id
                            val pending = currentProfileId?.let {
                                catalogContainer.executions.activeExecution(it).first()
                            }
                            if (pending == null) userPreferencesRepository.selectProfile(id)
                        }
                    },
                )
            }
        }
    }
}
