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
import com.neuronovaapps.rutinago.domain.communication.CommunicationNavigationState
import com.neuronovaapps.rutinago.domain.communication.CommunicationProfileSetup
import com.neuronovaapps.rutinago.domain.communication.CommunicationProfileSetupRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationPresentationPreferences
import com.neuronovaapps.rutinago.domain.communication.CommunicationPresentationRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationPresentationPolicy
import com.neuronovaapps.rutinago.domain.communication.CommunicationPersonalVocabularyRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationHomeVocabularyRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationSpeechPreferencesRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationSpeechRate
import com.neuronovaapps.rutinago.domain.communication.CommunicationSpeechEngine
import com.neuronovaapps.rutinago.ui.screens.communication.components.CommunicationPresentationSelector
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine

private sealed interface SetupLoadState {
    data object Loading : SetupLoadState
    data class Ready(val setup: CommunicationProfileSetup?,
        val presentation: CommunicationPresentationPreferences, val visibleIds: Set<String>?, val homeIds: Set<String>?) : SetupLoadState
    data object Failed : SetupLoadState
}

@Composable
fun CommunicationProfileEntryScreen(
    profileId: String,
    activeProfileName: String,
    setupRepository: CommunicationProfileSetupRepository? = null,
    presentationRepository: CommunicationPresentationRepository? = null,
    vocabularyRepository: CommunicationPersonalVocabularyRepository? = null,
    homeVocabularyRepository: CommunicationHomeVocabularyRepository? = null,
    speechPreferencesRepository: CommunicationSpeechPreferencesRepository? = null,
    speechEngineFactory: (() -> CommunicationSpeechEngine)? = null,
    onBackToModules: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val defaultRepository = remember(context) { UserPreferencesRepository(context) }
    val repository = setupRepository ?: defaultRepository
    val presentation = presentationRepository ?: defaultRepository
    val vocabulary = vocabularyRepository ?: defaultRepository
    val homeVocabulary = homeVocabularyRepository ?: defaultRepository
    key(profileId) {
        CommunicationProfileEntryContent(profileId, activeProfileName, repository, presentation, vocabulary, homeVocabulary,
            speechPreferencesRepository ?: defaultRepository, speechEngineFactory, onBackToModules)
    }
}

@Composable
private fun CommunicationProfileEntryContent(
    profileId: String,
    activeProfileName: String,
    repository: CommunicationProfileSetupRepository,
    presentationRepository: CommunicationPresentationRepository,
    vocabularyRepository: CommunicationPersonalVocabularyRepository,
    homeVocabularyRepository: CommunicationHomeVocabularyRepository,
    speechRepository: CommunicationSpeechPreferencesRepository,
    speechEngineFactory: (() -> CommunicationSpeechEngine)?,
    onBackToModules: () -> Unit,
) {
    var retry by remember { mutableIntStateOf(0) }
    var navigation by rememberSaveable(stateSaver = CommunicationNavigationSaver) { mutableStateOf(CommunicationNavigationState()) }
    var vocabularySaving by remember { mutableStateOf(false) }
    var vocabularyError by remember { mutableStateOf<String?>(null) }
    var homeSaving by remember { mutableStateOf(false) }
    var homeError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var presentationSaving by remember { mutableStateOf(false) }
    var presentationError by remember { mutableStateOf<String?>(null) }
    // A visible first-visit explanation survives recomposition, but not another module visit.
    var introductionVisible by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()
    val speech = rememberCommunicationSpeechController(profileId, speechEngineFactory)
    var speechRateSaving by remember { mutableStateOf(false) }
    var speechRateError by remember { mutableStateOf<String?>(null) }
    val speechRate by produceState<CommunicationSpeechRate?>(null, speechRepository, profileId, retry) {
        try { speechRepository.speechRateForProfile(profileId).collect { value = it } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            value = CommunicationSpeechRate.NORMAL
            speechRateError = "No se pudo recuperar la velocidad. Se usará Normal temporalmente."
        }
    }
    val messageSession = rememberSaveableStateHolder()
    val loaded by produceState<SetupLoadState>(SetupLoadState.Loading, repository, presentationRepository, vocabularyRepository, homeVocabularyRepository, profileId, retry) {
        value = SetupLoadState.Loading
        try {
            combine(repository.setupForProfile(profileId), presentationRepository.presentationForProfile(profileId),
                vocabularyRepository.vocabularyForProfile(profileId), homeVocabularyRepository.homeVocabularyForProfile(profileId)) {
                setup, presentation, ids, homeIds -> SetupLoadState.Ready(setup, presentation, ids, homeIds)
            }.collect { value = it }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            value = SetupLoadState.Failed
        }
    }
    fun setHomeWord(id: String, inHome: Boolean) {
        if (homeSaving) return
        homeSaving = true
        homeError = null
        scope.launch {
            try {
                homeVocabularyRepository.setHomeWord(profileId, id, inHome)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                homeError = "No se pudo guardar la selección de Inicio. Reintenta la acción."
            } finally {
                homeSaving = false
            }
        }
    }
    fun saveVocabulary(ids: Set<String>) {
        if (vocabularySaving) return
        vocabularySaving = true
        vocabularyError = null
        scope.launch {
            try {
                vocabularyRepository.saveVocabulary(profileId, ids)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                vocabularyError = "No se pudo guardar el vocabulario. La selección guardada no cambió. Reintenta la acción."
            } finally {
                vocabularySaving = false
            }
        }
    }
    BackHandler { if (!saving) onBackToModules() }
    val current = loaded
    if (current is SetupLoadState.Ready) {
        val initial = current.setup
        if (initial?.setupCompleted != true || navigation.personalizing) {
            CommunicationProfileSetupScreen(
                profileId, activeProfileName, initial, navigation.personalizing, saving, saveError,
                onSave = { age, oral ->
                    if (!saving) {
                        saving = true
                        saveError = null
                        scope.launch {
                            try {
                                repository.saveSetup(CommunicationProfileSetup(profileId, age, oral))
                                navigation = navigation.back() ?: CommunicationNavigationState()
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
                    navigation.back()?.let { navigation = it } ?: onBackToModules()
                },
            )
        } else if (current.visibleIds == null) {
            CommunicationVocabularyScreen(activeProfileName, current.visibleIds, vocabularySaving, vocabularyError,
                onSave = ::saveVocabulary,
                onBack = {
                    vocabularyError = null
                    onBackToModules()
                },
            )
        } else {
            messageSession.SaveableStateProvider("builder") {
                CommunicationHomeScreen(
                    speechContent = { items ->
                        speechRate?.let { CommunicationSpeechControls(speech, items, it) }
                            ?: Text("Cargando preferencias de voz…")
                    },
                    speechSettings = {
                        speechRate?.let { currentRate -> CommunicationSpeechSettings(speech, currentRate, speechRateSaving, speechRateError) { rate ->
                            if (!speechRateSaving) {
                                speechRateSaving = true
                                speechRateError = null
                                scope.launch {
                                    try { speechRepository.setSpeechRate(profileId, rate) }
                                    catch (cancelled: CancellationException) { throw cancelled }
                                    catch (_: Exception) { speechRateError = "No se pudo guardar la velocidad. Inténtalo de nuevo." }
                                    finally { speechRateSaving = false }
                                }
                            }
                        } } ?: Text("Cargando preferencias de voz…")
                    },
                    activeProfileName = activeProfileName,
                    onPersonalize = { navigation = navigation.personalize() },
                    navigation = navigation,
                    onNavigate = { navigation = it },
                    visibleVocabularyIds = current.visibleIds,
                    homeVocabularyIds = current.homeIds,
                    homeSaving = homeSaving,
                    homeError = homeError,
                    onSetHomeWord = ::setHomeWord,
                    profileSetup = initial,
                    vocabularyContent = { catalog ->
                        CommunicationVocabularyContent(activeProfileName, catalog, current.visibleIds,
                            vocabularySaving, vocabularyError,
                            onSave = ::saveVocabulary, onBack = onBackToModules, showBack = false,
                            homeIds = current.homeIds,
                            homeSaving = homeSaving, homeError = homeError, onSetHomeWord = ::setHomeWord,
                            onOrganizeHome = { navigation = navigation.organizeHome() })
                    },
                    presentationMode = CommunicationPresentationPolicy.resolve(initial, current.presentation),
                    presentationControls = {
                        CommunicationPresentationSelector(
                            selected = CommunicationPresentationPolicy.resolve(initial, current.presentation),
                            saving = presentationSaving,
                            error = presentationError,
                            showIntroduction = introductionVisible ?: !current.presentation.introductionSeen,
                            suggestionReason = CommunicationPresentationPolicy.suggest(initial).reason,
                            onSelect = { mode ->
                                if (!presentationSaving) {
                                    presentationSaving = true
                                    presentationError = null
                                    introductionVisible = false
                                    scope.launch {
                                        try {
                                            presentationRepository.setPresentationMode(profileId, mode)
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (_: Exception) {
                                            presentationError = "No se pudo guardar la presentación. Vuelve a seleccionarla para reintentar."
                                        } finally {
                                            presentationSaving = false
                                        }
                                    }
                                }
                            },
                            onIntroductionShown = {
                                if (introductionVisible == null) {
                                    introductionVisible = true
                                    scope.launch {
                                        try {
                                            presentationRepository.markPresentationIntroductionSeen(profileId)
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (_: Exception) {
                                            presentationError = "No se pudo guardar la presentación."
                                        }
                                    }
                                }
                            },
                            onDismissIntroduction = { introductionVisible = false },
                        )
                    },
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
