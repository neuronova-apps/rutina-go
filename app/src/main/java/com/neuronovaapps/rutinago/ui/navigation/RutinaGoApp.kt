package com.neuronovaapps.rutinago.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.preferences.ThemeMode
import com.neuronovaapps.rutinago.data.preferences.TextScaleMode
import com.neuronovaapps.rutinago.data.preferences.AppMode
import com.neuronovaapps.rutinago.data.preferences.UserPreferences
import com.neuronovaapps.rutinago.domain.catalog.CatalogUiState
import com.neuronovaapps.rutinago.domain.execution.ProfileProgress
import com.neuronovaapps.rutinago.domain.execution.RoutineExecution
import com.neuronovaapps.rutinago.domain.user.UserRoutine
import com.neuronovaapps.rutinago.domain.user.UserRoutineDraft
import com.neuronovaapps.rutinago.ui.screens.about.AboutScreen
import com.neuronovaapps.rutinago.ui.screens.create.CreateScreen
import com.neuronovaapps.rutinago.ui.screens.create.UserRoutineEditorScreen
import com.neuronovaapps.rutinago.ui.screens.execution.RoutineExecutionScreen
import com.neuronovaapps.rutinago.ui.screens.home.HomeScreen
import com.neuronovaapps.rutinago.ui.screens.progress.ProgressScreen
import com.neuronovaapps.rutinago.ui.screens.profiles.FirstProfileScreen
import com.neuronovaapps.rutinago.ui.screens.profiles.ProfilesScreen
import com.neuronovaapps.rutinago.ui.screens.routines.CatalogRoutineDetailScreen
import com.neuronovaapps.rutinago.ui.screens.routines.RoutinesScreen
import com.neuronovaapps.rutinago.ui.screens.settings.SettingsScreen
import com.neuronovaapps.rutinago.ui.screens.welcome.WelcomeScreen
import com.neuronovaapps.rutinago.ui.screens.modules.ModuleSelectorScreen
import com.neuronovaapps.rutinago.ui.screens.communication.CommunicationHomeScreen
import com.neuronovaapps.rutinago.ui.theme.LocalHighContrast
import com.neuronovaapps.rutinago.ui.components.EmptyState
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import kotlinx.coroutines.launch

@Composable
fun RutinaGoApp(
    preferences: UserPreferences,
    catalogState: CatalogUiState,
    userRoutines: List<UserRoutine>,
    activeExecution: RoutineExecution?,
    progress: ProfileProgress,
    onSaveUserRoutine: suspend (UserRoutineDraft) -> UserRoutine,
    onArchiveUserRoutine: suspend (String) -> Unit,
    onDeleteUserRoutine: suspend (String) -> Unit,
    onStartExecution: suspend (String) -> RoutineExecution,
    onLoadExecution: suspend (String) -> RoutineExecution?,
    onCompleteExecutionStep: suspend (String, Int) -> RoutineExecution,
    onSkipExecutionStep: suspend (String, Int) -> RoutineExecution,
    onFinishPartialExecution: suspend (String) -> RoutineExecution,
    onAbandonExecution: suspend (String) -> RoutineExecution,
    onThemeModeChange: (ThemeMode) -> Unit,
    onTextScaleModeChange: (TextScaleMode) -> Unit,
    onHighContrastChange: (Boolean) -> Unit,
    onAppModeChange: (AppMode) -> Unit,
    onCreateProfile: (String) -> Unit,
    onRenameProfile: (String, String) -> Unit,
    onSelectProfile: (String) -> Unit,
) {
    val highContrast = LocalHighContrast.current
    var showWelcome by rememberSaveable { mutableStateOf(true) }
    var selectedModule by rememberSaveable { mutableStateOf<String?>(null) }
    val communicationSession = rememberSaveableStateHolder()
    var destinationName by rememberSaveable { mutableStateOf(AppDestination.HOME.name) }
    var previousMainName by rememberSaveable { mutableStateOf(AppDestination.HOME.name) }
    var selectedCatalogRoutineId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUserRoutineId by rememberSaveable { mutableStateOf<String?>(null) }
    var suggestedTemplateId by rememberSaveable { mutableStateOf<String?>(null) }
    var executionReturnName by rememberSaveable { mutableStateOf(AppDestination.HOME.name) }
    var selectedExecutionId by rememberSaveable { mutableStateOf<String?>(null) }
    var executionSession by remember { mutableStateOf<RoutineExecution?>(null) }
    var executionLoadError by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSavedRoutine by remember { mutableStateOf<UserRoutine?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val destination = AppDestination.valueOf(destinationName)

    val visibleUserRoutines = pendingSavedRoutine
        ?.takeIf { it.profileId == preferences.activeProfile?.id }
        ?.let { stored -> listOf(stored) + userRoutines.filterNot { it.id == stored.id } }
        ?: userRoutines

    LaunchedEffect(userRoutines, pendingSavedRoutine, preferences.activeProfile?.id) {
        val stored = pendingSavedRoutine
        if (stored != null && (
                stored.profileId != preferences.activeProfile?.id ||
                    userRoutines.any { it == stored }
            )
        ) pendingSavedRoutine = null
    }

    fun navigateTo(target: AppDestination) {
        if (destination.isMain) previousMainName = destination.name
        destinationName = target.name
    }

    fun navigateMain(target: AppDestination, templateId: String? = null) {
        destinationName = target.name
        previousMainName = target.name
        suggestedTemplateId = if (target == AppDestination.CREATE) templateId else null
    }

    fun completeRoutineSave(stored: UserRoutine, edited: Boolean) {
        pendingSavedRoutine = stored
        navigateMain(AppDestination.ROUTINES)
        scope.launch {
            snackbarHostState.showSnackbar(
                message = if (edited) {
                    "Cambios guardados correctamente"
                } else {
                    "Rutina guardada correctamente"
                },
                duration = SnackbarDuration.Long,
            )
        }
    }

    suspend fun startExecution(routineId: String) {
        val started = onStartExecution(routineId)
        executionSession = started
        selectedExecutionId = started.id
        executionLoadError = null
        executionReturnName = destination.name
        destinationName = AppDestination.EXECUTION.name
    }

    fun resumeExecution() {
        val pending = activeExecution ?: return
        executionSession = pending
        selectedExecutionId = pending.id
        executionLoadError = null
        executionReturnName = destination.name
        destinationName = AppDestination.EXECUTION.name
    }

    fun returnFromExecution() {
        val safeDestination = AppDestination.entries.firstOrNull { it.name == executionReturnName }
            ?.takeUnless { it == AppDestination.EXECUTION }
            ?: AppDestination.HOME
        destinationName = safeDestination.name
        if (safeDestination.isMain) previousMainName = safeDestination.name
        executionSession = null
        selectedExecutionId = null
        executionLoadError = null
    }

    LaunchedEffect(destination, selectedExecutionId) {
        val executionId = selectedExecutionId
        if (destination == AppDestination.EXECUTION && executionId != null &&
            executionSession?.id != executionId
        ) {
            runCatching { onLoadExecution(executionId) }
                .onSuccess { loaded ->
                    executionSession = loaded
                    executionLoadError = if (loaded == null) "No se encontró la ejecución guardada." else null
                }
                .onFailure {
                    executionLoadError = it.message ?: "No fue posible recuperar la ejecución."
                }
        }
    }

    if (showWelcome) {
        WelcomeScreen(
            onStart = {
                destinationName = AppDestination.HOME.name
                previousMainName = AppDestination.HOME.name
                showWelcome = false
            },
        )
        return
    }

    if (preferences.profiles.isEmpty()) {
        FirstProfileScreen(onCreateProfile = onCreateProfile)
        return
    }

    val activeProfileName = preferences.activeProfile?.name.orEmpty()

    if (selectedModule == null) {
        ModuleSelectorScreen(
            activeProfileName = activeProfileName,
            onOpenRutinaGo = { selectedModule = "rutinago" },
            onOpenCommunication = { selectedModule = "communication" },
        )
        return
    }

    if (selectedModule == "communication") {
        communicationSession.SaveableStateProvider("communication:${preferences.activeProfile?.id}") {
            CommunicationHomeScreen(
                activeProfileName = activeProfileName,
                onBackToModules = { selectedModule = null },
            )
        }
        return
    }

    if (destination == AppDestination.EXECUTION) {
        val currentExecution = executionSession
        if (currentExecution == null) {
            ScreenColumn {
                if (executionLoadError == null) {
                    Text("Recuperando la rutina…")
                } else {
                    EmptyState(
                        title = "No se pudo recuperar la rutina",
                        body = executionLoadError.orEmpty(),
                        actionLabel = "Volver",
                        onAction = ::returnFromExecution,
                    )
                }
            }
        } else {
            RoutineExecutionScreen(
                execution = currentExecution,
                onCompleteStep = { position ->
                    onCompleteExecutionStep(currentExecution.id, position)
                },
                onSkipStep = { position -> onSkipExecutionStep(currentExecution.id, position) },
                onFinishPartial = { onFinishPartialExecution(currentExecution.id) },
                onAbandon = { onAbandonExecution(currentExecution.id) },
                onExecutionChanged = { executionSession = it },
                onExit = ::returnFromExecution,
                onCloseResult = ::returnFromExecution,
                onViewProgress = {
                    executionSession = null
                    selectedExecutionId = null
                    navigateMain(AppDestination.PROGRESS)
                },
            )
        }
        return
    }

    fun backFromRutinaGo() {
        if (destination.isMain) selectedModule = null
        else destinationName = previousMainName
    }

    BackHandler(onBack = ::backFromRutinaGo)

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .testTag("routine-save-confirmation")
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        },
        topBar = {
            AppHeader(
                destination = destination,
                activeProfileName = activeProfileName,
                showBack = destination != AppDestination.USER_ROUTINE_EDIT,
                onBack = ::backFromRutinaGo,
            )
        },
        bottomBar = {
            if (destination.isMain) {
                NavigationBar(
                    modifier = if (highContrast) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline)
                    } else {
                        Modifier
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    mainDestinations.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { navigateMain(item) },
                            modifier = Modifier.testTag("destination-${item.name}"),
                            icon = {
                                Text(
                                    item.symbol,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.clearAndSetSemantics {},
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = if (highContrast) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = if (highContrast) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                },
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (destination) {
                AppDestination.HOME -> HomeScreen(
                    activeProfileName = activeProfileName,
                    activeExecution = activeExecution,
                    progress = progress,
                    onResumeExecution = ::resumeExecution,
                    onOpenRoutines = { navigateMain(AppDestination.ROUTINES) },
                    onCreateRoutine = { navigateMain(AppDestination.CREATE) },
                )

                AppDestination.ROUTINES -> RoutinesScreen(
                    appMode = preferences.appMode,
                    catalogState = catalogState,
                    activeProfileName = activeProfileName,
                    userRoutines = visibleUserRoutines,
                    activeExecution = activeExecution,
                    onOpenRoutine = { routineId ->
                        selectedCatalogRoutineId = routineId
                        navigateTo(AppDestination.ROUTINE_DETAIL)
                    },
                    onUseTemplate = { routineId ->
                        navigateMain(AppDestination.CREATE, routineId)
                    },
                    onEditUserRoutine = { routineId ->
                        selectedUserRoutineId = routineId
                        navigateTo(AppDestination.USER_ROUTINE_EDIT)
                    },
                    onStartUserRoutine = ::startExecution,
                    onResumeExecution = ::resumeExecution,
                    onArchiveUserRoutine = { routineId ->
                        onArchiveUserRoutine(routineId)
                        if (pendingSavedRoutine?.id == routineId) pendingSavedRoutine = null
                    },
                    onDeleteUserRoutine = { routineId ->
                        onDeleteUserRoutine(routineId)
                        if (pendingSavedRoutine?.id == routineId) pendingSavedRoutine = null
                    },
                )

                AppDestination.CREATE -> CreateScreen(
                    appMode = preferences.appMode,
                    catalogState = catalogState,
                    suggestedTemplateId = suggestedTemplateId,
                    onSaveRoutine = onSaveUserRoutine,
                    onSaved = { completeRoutineSave(it, edited = false) },
                )

                AppDestination.PROGRESS -> ProgressScreen(
                    activeProfileName = activeProfileName,
                    progress = progress,
                )

                AppDestination.SETTINGS -> SettingsScreen(
                    themeMode = preferences.themeMode,
                    onThemeModeChange = onThemeModeChange,
                    textScaleMode = preferences.textScaleMode,
                    onTextScaleModeChange = onTextScaleModeChange,
                    highContrast = preferences.highContrast,
                    onHighContrastChange = onHighContrastChange,
                    appMode = preferences.appMode,
                    onAppModeChange = onAppModeChange,
                    activeProfileName = activeProfileName,
                    onManageProfiles = { navigateTo(AppDestination.PROFILES) },
                    onOpenAbout = { navigateTo(AppDestination.ABOUT) },
                    onChangeModule = { selectedModule = null },
                )

                AppDestination.PROFILES -> ProfilesScreen(
                    profiles = preferences.profiles,
                    activeProfileId = preferences.activeProfile?.id,
                    onCreateProfile = onCreateProfile,
                    onRenameProfile = onRenameProfile,
                    onSelectProfile = onSelectProfile,
                    profileSwitchingEnabled = activeExecution == null,
                )

                AppDestination.ABOUT -> AboutScreen()

                AppDestination.ROUTINE_DETAIL -> {
                    CatalogRoutineDetailScreen(
                        catalogState = catalogState,
                        routineId = selectedCatalogRoutineId,
                        appMode = preferences.appMode,
                        onUseTemplate = { routineId ->
                            navigateMain(AppDestination.CREATE, routineId)
                        },
                    )
                }

                AppDestination.USER_ROUTINE_EDIT -> {
                    val routine = visibleUserRoutines.firstOrNull { it.id == selectedUserRoutineId }
                    if (routine == null) {
                        ScreenColumn {
                            EmptyState(
                                "Rutina no disponible",
                                "La rutina no pertenece al perfil activo o fue archivada.",
                                "Volver a Mis rutinas",
                            ) { navigateMain(AppDestination.ROUTINES) }
                        }
                    } else {
                        UserRoutineEditorScreen(
                            routine = routine,
                            appMode = preferences.appMode,
                            catalogState = catalogState,
                            onBack = { navigateMain(AppDestination.ROUTINES) },
                            onSaveRoutine = onSaveUserRoutine,
                            onSaved = { completeRoutineSave(it, edited = true) },
                        )
                    }
                }

                AppDestination.EXECUTION -> Unit
            }
        }
    }
}

@Composable
private fun AppHeader(
    destination: AppDestination,
    activeProfileName: String,
    showBack: Boolean,
    onBack: () -> Unit,
) {
    val highContrast = LocalHighContrast.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (highContrast) 1.dp else 0.dp,
            MaterialTheme.colorScheme.outline,
        ),
        shadowElevation = 1.dp,
    ) {
        Column {
            // Keep the module exit on its own row so large text cannot squeeze the title/profile.
            if (destination == AppDestination.HOME) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp)
                        .testTag("rutinago-back-to-modules"),
                ) {
                    Text("Volver al selector", textAlign = TextAlign.Center)
                }
            }
            Row(
                modifier = Modifier
                    .then(if (destination == AppDestination.HOME) Modifier else Modifier.statusBarsPadding())
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!destination.isMain && showBack) {
                    TextButton(onClick = onBack) { Text("‹ Volver") }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(destination.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        when (destination) {
                            AppDestination.HOME -> "NeuroNova Apps"
                            AppDestination.PROGRESS -> "Perfil $activeProfileName · datos guardados"
                            AppDestination.SETTINGS -> "Preferencias guardadas en este dispositivo"
                            AppDestination.ABOUT -> "Información de la aplicación"
                            else -> "RutinaGo"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (destination == AppDestination.HOME || destination == AppDestination.PROGRESS) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Perfil", style = MaterialTheme.typography.labelSmall)
                            Text(activeProfileName, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
