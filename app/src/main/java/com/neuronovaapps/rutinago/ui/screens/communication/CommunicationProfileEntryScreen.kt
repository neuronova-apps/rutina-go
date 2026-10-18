package com.neuronovaapps.rutinago.ui.screens.communication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.preferences.UserPreferencesRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationProfileSetup
import com.neuronovaapps.rutinago.domain.communication.CommunicationProfileSetupRepository
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private sealed interface SetupLoadState {
    data object Loading : SetupLoadState
    data class Ready(val setup: CommunicationProfileSetup?) : SetupLoadState
    data object Failed : SetupLoadState
}

@Composable
fun CommunicationProfileEntryScreen(
    profileId: String,
    activeProfileName: String,
    setupRepository: CommunicationProfileSetupRepository? = null,
    onBackToModules: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val repository = remember(context, setupRepository) { setupRepository ?: UserPreferencesRepository(context) }
    key(profileId) {
        CommunicationProfileEntryContent(profileId, activeProfileName, repository, onBackToModules)
    }
}

@Composable
private fun CommunicationProfileEntryContent(
    profileId: String,
    activeProfileName: String,
    repository: CommunicationProfileSetupRepository,
    onBackToModules: () -> Unit,
) {
    var retry by remember { mutableIntStateOf(0) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val messageSession = rememberSaveableStateHolder()
    val loaded by produceState<SetupLoadState>(SetupLoadState.Loading, repository, profileId, retry) {
        value = SetupLoadState.Loading
        try {
            repository.setupForProfile(profileId).collect { value = SetupLoadState.Ready(it) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            value = SetupLoadState.Failed
        }
    }
    BackHandler { if (!saving) onBackToModules() }
    val current = loaded
    if (current is SetupLoadState.Ready) {
        val initial = current.setup
        if (initial?.setupCompleted != true || editing) {
            CommunicationProfileSetupScreen(
                profileId, activeProfileName, initial, editing, saving, saveError,
                onSave = { age, oral ->
                    if (!saving) {
                        saving = true
                        saveError = null
                        scope.launch {
                            try {
                                repository.saveSetup(CommunicationProfileSetup(profileId, age, oral))
                                editing = false
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                saveError = "No se pudo guardar la configuración. Inténtalo de nuevo."
                            } finally {
                                saving = false
                            }
                        }
                    }
                },
                onBack = {
                    saveError = null
                    if (editing && initial?.setupCompleted == true) editing = false else onBackToModules()
                },
            )
        } else {
            messageSession.SaveableStateProvider("builder") {
                CommunicationHomeScreen(
                    activeProfileName = activeProfileName,
                    onPersonalize = { editing = true },
                    onBackToModules = onBackToModules,
                )
            }
        }
    } else {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            ScreenColumn(Modifier.safeDrawingPadding().testTag("communication-setup-loading")) {
                OutlinedButton(onClick = onBackToModules, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Volver al selector")
                }
                Text("Perfil activo: $activeProfileName")
                Text(if (current == SetupLoadState.Loading) "Cargando configuración…"
                    else "No se pudo recuperar la configuración de ComunicaGo.")
                if (current == SetupLoadState.Failed) {
                    OutlinedButton(onClick = { retry++ }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Reintentar") }
                }
            }
        }
    }
}
