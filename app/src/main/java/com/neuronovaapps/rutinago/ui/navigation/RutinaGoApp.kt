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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.demo.DemoContent
import com.neuronovaapps.rutinago.data.preferences.ThemeMode
import com.neuronovaapps.rutinago.data.preferences.TextScaleMode
import com.neuronovaapps.rutinago.data.preferences.AppMode
import com.neuronovaapps.rutinago.data.preferences.UserPreferences
import com.neuronovaapps.rutinago.domain.catalog.CatalogUiState
import com.neuronovaapps.rutinago.domain.model.Routine
import com.neuronovaapps.rutinago.ui.screens.about.AboutScreen
import com.neuronovaapps.rutinago.ui.screens.create.CreateScreen
import com.neuronovaapps.rutinago.ui.screens.execution.RoutineExecutionScreen
import com.neuronovaapps.rutinago.ui.screens.home.HomeScreen
import com.neuronovaapps.rutinago.ui.screens.home.NowNextScreen
import com.neuronovaapps.rutinago.ui.screens.progress.ProgressScreen
import com.neuronovaapps.rutinago.ui.screens.profiles.FirstProfileScreen
import com.neuronovaapps.rutinago.ui.screens.profiles.ProfilesScreen
import com.neuronovaapps.rutinago.ui.screens.routines.CatalogRoutineDetailScreen
import com.neuronovaapps.rutinago.ui.screens.routines.LegacyRoutineDetailScreen
import com.neuronovaapps.rutinago.ui.screens.routines.RoutinesScreen
import com.neuronovaapps.rutinago.ui.screens.settings.SettingsScreen
import com.neuronovaapps.rutinago.ui.screens.welcome.WelcomeScreen
import com.neuronovaapps.rutinago.ui.theme.LocalHighContrast

@Composable
fun RutinaGoApp(
    preferences: UserPreferences,
    catalogState: CatalogUiState,
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
    var destinationName by rememberSaveable { mutableStateOf(AppDestination.HOME.name) }
    var previousMainName by rememberSaveable { mutableStateOf(AppDestination.HOME.name) }
    var selectedRoutineId by rememberSaveable { mutableStateOf(DemoContent.routines.first().id) }
    var selectedCatalogRoutineId by rememberSaveable { mutableStateOf<String?>(null) }
    var suggestedTemplateId by rememberSaveable { mutableStateOf<String?>(null) }
    var executionReturnName by rememberSaveable { mutableStateOf(AppDestination.HOME.name) }
    val destination = AppDestination.valueOf(destinationName)
    val selectedRoutine = (DemoContent.routines + DemoContent.templates)
        .firstOrNull { it.id == selectedRoutineId }
        ?: DemoContent.routines.first()

    fun selectRoutine(routine: Routine) {
        selectedRoutineId = routine.id
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

    fun startExecution(routine: Routine) {
        selectRoutine(routine)
        executionReturnName = destination.name
        destinationName = AppDestination.EXECUTION.name
    }

    fun returnFromExecution() {
        val safeDestination = AppDestination.entries.firstOrNull { it.name == executionReturnName }
            ?.takeUnless { it == AppDestination.EXECUTION }
            ?: AppDestination.HOME
        destinationName = safeDestination.name
        if (safeDestination.isMain) previousMainName = safeDestination.name
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

    if (destination == AppDestination.EXECUTION) {
        RoutineExecutionScreen(
            routine = selectedRoutine,
            onExit = ::returnFromExecution,
            onCloseResult = ::returnFromExecution,
            onViewProgress = { navigateMain(AppDestination.PROGRESS) },
        )
        return
    }

    BackHandler(enabled = !destination.isMain) {
        destinationName = previousMainName
    }

    Scaffold(
        topBar = {
            AppHeader(
                destination = destination,
                activeProfileName = activeProfileName,
                onBack = { destinationName = previousMainName },
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
                    appMode = preferences.appMode,
                    onViewNowNext = { navigateTo(AppDestination.NOW_NEXT) },
                    onOpenRoutine = { routine ->
                        selectRoutine(routine)
                        selectedCatalogRoutineId = null
                        navigateTo(AppDestination.ROUTINE_DETAIL)
                    },
                    onStartRoutine = { routine ->
                        startExecution(routine)
                    },
                    onCreateRoutine = { navigateMain(AppDestination.CREATE) },
                )

                AppDestination.ROUTINES -> RoutinesScreen(
                    appMode = preferences.appMode,
                    catalogState = catalogState,
                    onOpenRoutine = { routineId ->
                        selectedCatalogRoutineId = routineId
                        navigateTo(AppDestination.ROUTINE_DETAIL)
                    },
                    onUseTemplate = { routineId ->
                        navigateMain(AppDestination.CREATE, routineId)
                    },
                )

                AppDestination.CREATE -> CreateScreen(
                    appMode = preferences.appMode,
                    catalogState = catalogState,
                    suggestedTemplateId = suggestedTemplateId,
                )

                AppDestination.PROGRESS -> ProgressScreen(
                    activeProfileName = activeProfileName,
                    appMode = preferences.appMode,
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
                )

                AppDestination.PROFILES -> ProfilesScreen(
                    profiles = preferences.profiles,
                    activeProfileId = preferences.activeProfile?.id,
                    onCreateProfile = onCreateProfile,
                    onRenameProfile = onRenameProfile,
                    onSelectProfile = onSelectProfile,
                )

                AppDestination.ABOUT -> AboutScreen()

                AppDestination.NOW_NEXT -> NowNextScreen(
                    onStartRoutine = {
                        startExecution(DemoContent.routines.first())
                    },
                )

                AppDestination.ROUTINE_DETAIL -> {
                    if (selectedCatalogRoutineId != null) {
                        CatalogRoutineDetailScreen(
                            catalogState = catalogState,
                            routineId = selectedCatalogRoutineId,
                            appMode = preferences.appMode,
                            onUseTemplate = { routineId ->
                                navigateMain(AppDestination.CREATE, routineId)
                            },
                        )
                    } else {
                        LegacyRoutineDetailScreen(
                            routine = selectedRoutine,
                            onStart = { startExecution(selectedRoutine) },
                            onUseTemplate = { navigateMain(AppDestination.CREATE) },
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
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .heightIn(min = 64.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!destination.isMain) {
                TextButton(onClick = onBack) { Text("‹ Volver") }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(destination.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    when (destination) {
                        AppDestination.HOME -> "NeuroNova Apps"
                        AppDestination.PROGRESS -> "Perfil $activeProfileName · datos demo"
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
