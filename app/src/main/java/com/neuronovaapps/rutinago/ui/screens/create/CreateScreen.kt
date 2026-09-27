package com.neuronovaapps.rutinago.ui.screens.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant
import com.neuronovaapps.rutinago.data.arasaac.DefaultArasaacRepository
import com.neuronovaapps.rutinago.data.preferences.AppMode
import com.neuronovaapps.rutinago.domain.catalog.CatalogActivity
import com.neuronovaapps.rutinago.domain.catalog.CatalogRoutine
import com.neuronovaapps.rutinago.domain.catalog.CatalogSnapshot
import com.neuronovaapps.rutinago.domain.catalog.CatalogUiState
import com.neuronovaapps.rutinago.domain.catalog.activitiesFor
import com.neuronovaapps.rutinago.domain.catalog.catalogVariant
import com.neuronovaapps.rutinago.domain.catalog.routinesFor
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacRepository
import com.neuronovaapps.rutinago.domain.user.UserRoutine
import com.neuronovaapps.rutinago.domain.user.UserRoutineDraft
import com.neuronovaapps.rutinago.domain.user.UserRoutineDraftStep
import com.neuronovaapps.rutinago.domain.user.RoutineStepVisualCodec
import com.neuronovaapps.rutinago.domain.user.RoutineStepVisualReference
import com.neuronovaapps.rutinago.ui.components.EmptyState
import com.neuronovaapps.rutinago.ui.components.InfoCallout
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.VisualPlaceholder
import com.neuronovaapps.rutinago.ui.screens.arasaac.ArasaacSearchDialog
import com.neuronovaapps.rutinago.ui.components.ArasaacPictogramImage
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacImageResolution
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacImageUrls
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class CreatePage { LANDING, ORIGIN, TEMPLATE_LIST, TEMPLATE_EDITOR, SCRATCH_EDITOR }
private enum class RoutineSaveState { IDLE, SAVING, SAVED, ERROR }

@Composable
fun CreateScreen(
    appMode: AppMode,
    catalogState: CatalogUiState,
    suggestedTemplateId: String? = null,
    onSaveRoutine: suspend (UserRoutineDraft) -> UserRoutine,
    onSaved: (UserRoutine) -> Unit,
    arasaacRepository: ArasaacRepository = DefaultArasaacRepository,
) {
    when (catalogState) {
        CatalogUiState.Loading -> CatalogLoading()
        is CatalogUiState.Error -> CatalogError(catalogState.message)
        is CatalogUiState.Ready -> CreateReadyScreen(
            appMode = appMode,
            catalog = catalogState.catalog,
            suggestedTemplateId = suggestedTemplateId,
            onSaveRoutine = onSaveRoutine,
            onSaved = onSaved,
            arasaacRepository = arasaacRepository,
        )
    }
}

@Composable
private fun CreateReadyScreen(
    appMode: AppMode,
    catalog: CatalogSnapshot,
    suggestedTemplateId: String?,
    onSaveRoutine: suspend (UserRoutineDraft) -> UserRoutine,
    onSaved: (UserRoutine) -> Unit,
    arasaacRepository: ArasaacRepository,
) {
    var pageName by rememberSaveable(suggestedTemplateId) {
        mutableStateOf(
            if (suggestedTemplateId == null) CreatePage.LANDING.name
            else CreatePage.TEMPLATE_EDITOR.name,
        )
    }
    var selectedTemplateId by rememberSaveable(suggestedTemplateId) {
        mutableStateOf(suggestedTemplateId)
    }
    val page = CreatePage.valueOf(pageName)

    fun returnOneLevel() {
        pageName = when (page) {
            CreatePage.LANDING -> CreatePage.LANDING.name
            CreatePage.ORIGIN -> CreatePage.LANDING.name
            CreatePage.TEMPLATE_LIST -> CreatePage.ORIGIN.name
            CreatePage.TEMPLATE_EDITOR -> CreatePage.TEMPLATE_LIST.name
            CreatePage.SCRATCH_EDITOR -> CreatePage.ORIGIN.name
        }
    }

    if (page != CreatePage.TEMPLATE_EDITOR && page != CreatePage.SCRATCH_EDITOR) {
        BackHandler(enabled = page != CreatePage.LANDING) { returnOneLevel() }
    }

    when (page) {
        CreatePage.LANDING -> CreateLanding(appMode) { pageName = CreatePage.ORIGIN.name }
        CreatePage.ORIGIN -> OriginScreen(
            onBack = ::returnOneLevel,
            onTemplate = { pageName = CreatePage.TEMPLATE_LIST.name },
            onScratch = { pageName = CreatePage.SCRATCH_EDITOR.name },
        )
        CreatePage.TEMPLATE_LIST -> TemplateCatalogScreen(
            appMode = appMode,
            catalog = catalog,
            onBack = ::returnOneLevel,
            onSelect = { routineId ->
                selectedTemplateId = routineId
                pageName = CreatePage.TEMPLATE_EDITOR.name
            },
        )
        CreatePage.TEMPLATE_EDITOR -> {
            val routine = catalog.routine(selectedTemplateId)
            if (routine == null) {
                ScreenColumn {
                    TextButton(onClick = ::returnOneLevel) { Text("‹ Volver a Plantillas") }
                    EmptyState("Plantilla no disponible", "No se encontró la plantilla seleccionada.")
                }
            } else {
                val variant = appMode.catalogVariant()
                RoutineDraftEditor(
                    draftKey = "template-${routine.id}",
                    title = "Personalizar plantilla",
                    backLabel = "‹ Volver a Plantillas",
                    initialName = routine.name,
                    initialActivityIds = catalog.steps(routine.id, variant).map { it.activityId },
                    sourceTemplateId = routine.id,
                    optionalActivityIds = if (appMode == AppMode.ADVANCED) {
                        catalog.steps(routine.id, RoutineVariant.OPTIONAL).map { it.activityId }
                    } else {
                        emptyList()
                    },
                    appMode = appMode,
                    catalog = catalog,
                    onBack = ::returnOneLevel,
                    onSaveRoutine = onSaveRoutine,
                    onSaved = onSaved,
                    arasaacRepository = arasaacRepository,
                )
            }
        }
        CreatePage.SCRATCH_EDITOR -> RoutineDraftEditor(
            draftKey = "scratch",
            title = "Crear desde cero",
            backLabel = "‹ Volver a Elegir origen",
            initialName = "",
            initialActivityIds = emptyList(),
            sourceTemplateId = null,
            optionalActivityIds = emptyList(),
            appMode = appMode,
            catalog = catalog,
            onBack = ::returnOneLevel,
            onSaveRoutine = onSaveRoutine,
            onSaved = onSaved,
            arasaacRepository = arasaacRepository,
        )
    }
}

@Composable
fun UserRoutineEditorScreen(
    routine: UserRoutine,
    appMode: AppMode,
    catalogState: CatalogUiState,
    onBack: () -> Unit,
    onSaveRoutine: suspend (UserRoutineDraft) -> UserRoutine,
    onSaved: (UserRoutine) -> Unit,
    arasaacRepository: ArasaacRepository = DefaultArasaacRepository,
) {
    when (catalogState) {
        CatalogUiState.Loading -> CatalogLoading()
        is CatalogUiState.Error -> CatalogError(catalogState.message)
        is CatalogUiState.Ready -> RoutineDraftEditor(
            draftKey = "edit-${routine.id}",
            existingRoutineId = routine.id,
            title = "Editar rutina",
            backLabel = "‹ Volver a Mis rutinas",
            initialName = routine.name,
            initialActivityIds = routine.steps.sortedBy { it.position }.map { it.activityId },
            initialInstructions = routine.steps.associate { it.activityId to it.customInstruction },
            initialVisuals = routine.steps.mapNotNull { step ->
                step.futureVisualReference?.let { step.activityId to it }
            }.toMap(),
            sourceTemplateId = routine.sourceTemplateId,
            optionalActivityIds = emptyList(),
            appMode = appMode,
            catalog = catalogState.catalog,
            onBack = onBack,
            onSaveRoutine = onSaveRoutine,
            onSaved = onSaved,
            arasaacRepository = arasaacRepository,
        )
    }
}

@Composable
private fun RoutineDraftEditor(
    draftKey: String,
    title: String,
    backLabel: String,
    initialName: String,
    initialActivityIds: List<String>,
    sourceTemplateId: String?,
    optionalActivityIds: List<String>,
    appMode: AppMode,
    catalog: CatalogSnapshot,
    onBack: () -> Unit,
    onSaveRoutine: suspend (UserRoutineDraft) -> UserRoutine,
    onSaved: (UserRoutine) -> Unit,
    existingRoutineId: String? = null,
    initialInstructions: Map<String, String?> = emptyMap(),
    initialVisuals: Map<String, String> = emptyMap(),
    arasaacRepository: ArasaacRepository,
) {
    val scope = rememberCoroutineScope()
    val saveRequestInFlight = remember(draftKey) { AtomicBoolean(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var draftId by rememberSaveable(draftKey) {
        mutableStateOf(existingRoutineId ?: UUID.randomUUID().toString())
    }
    var routineName by rememberSaveable(draftKey) { mutableStateOf(initialName) }
    var selectedIds by rememberSaveable(draftKey) { mutableStateOf(initialActivityIds) }
    var baselineName by rememberSaveable(draftKey) { mutableStateOf(initialName) }
    var baselineIds by rememberSaveable(draftKey) { mutableStateOf(initialActivityIds) }
    var visualByActivityId by rememberSaveable(draftKey) { mutableStateOf(initialVisuals) }
    var baselineVisuals by rememberSaveable(draftKey) { mutableStateOf(initialVisuals) }
    var query by rememberSaveable(draftKey) { mutableStateOf("") }
    var category by rememberSaveable(draftKey) { mutableStateOf<String?>(null) }
    var confirmExit by rememberSaveable(draftKey) { mutableStateOf(false) }
    var saveState by remember(draftKey) { mutableStateOf(RoutineSaveState.IDLE) }
    var errorMessage by rememberSaveable(draftKey) { mutableStateOf<String?>(null) }
    var pictogramActivity by remember(draftKey) { mutableStateOf<CatalogActivity?>(null) }
    val hasChanges = routineName != baselineName || selectedIds != baselineIds ||
        visualByActivityId != baselineVisuals
    val filteredActivities = catalog.activitiesFor(query, category)

    fun requestBack() {
        if (hasChanges) confirmExit = true else onBack()
    }

    fun toggle(activityId: String) {
        if (saveState == RoutineSaveState.SAVING) return
        if (activityId in selectedIds) visualByActivityId = visualByActivityId - activityId
        selectedIds = if (activityId in selectedIds) selectedIds - activityId else selectedIds + activityId
        saveState = RoutineSaveState.IDLE
        errorMessage = null
    }

    fun move(activityId: String, offset: Int) {
        if (saveState == RoutineSaveState.SAVING) return
        val from = selectedIds.indexOf(activityId)
        val to = from + offset
        if (from < 0 || to !in selectedIds.indices) return
        selectedIds = selectedIds.toMutableList().also { list ->
            val moved = list.removeAt(from)
            list.add(to, moved)
        }
        saveState = RoutineSaveState.IDLE
        errorMessage = null
    }

    fun saveRoutine() {
        if (saveState == RoutineSaveState.SAVED) return
        if (!saveRequestInFlight.compareAndSet(false, true)) return
        saveState = RoutineSaveState.SAVING
        errorMessage = null
        scope.launch {
            val stored = try {
                onSaveRoutine(
                    UserRoutineDraft(
                        id = draftId,
                        sourceTemplateId = sourceTemplateId,
                        name = routineName,
                        steps = selectedIds.map { activityId ->
                            UserRoutineDraftStep(
                                activityId = activityId,
                                customInstruction = initialInstructions[activityId],
                                futureVisualReference = visualByActivityId[activityId],
                            )
                        },
                    ),
                )
            } catch (error: CancellationException) {
                saveState = RoutineSaveState.IDLE
                throw error
            } catch (error: Exception) {
                errorMessage = error.message ?: "No fue posible completar la transacción."
                saveState = RoutineSaveState.ERROR
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "No se pudo guardar la rutina",
                        duration = SnackbarDuration.Long,
                    )
                }
                return@launch
            } finally {
                saveRequestInFlight.set(false)
            }
            saveState = RoutineSaveState.SAVED
            onSaved(stored)
        }
    }

    BackHandler { requestBack() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        },
        bottomBar = {
            RoutineSaveActionBar(
                saving = saveState == RoutineSaveState.SAVING,
                enabled = routineName.isNotBlank() && selectedIds.isNotEmpty() &&
                    saveState != RoutineSaveState.SAVED,
                editing = existingRoutineId != null,
                onSave = ::saveRoutine,
            )
        },
    ) { editorPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(editorPadding)
                .consumeWindowInsets(editorPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 18.dp,
                end = 16.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        item { TextButton(onClick = ::requestBack) { Text(backLabel) } }
        item {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
        }
        errorMessage?.let { message ->
            item { InfoCallout("No se pudo guardar la rutina", message) }
        }
        if (sourceTemplateId != null) {
            item {
                InfoCallout(
                    "Copia personal",
                    "Los cambios se guardarán para el perfil activo sin modificar la plantilla maestra.",
                )
            }
        }
        item {
            OutlinedTextField(
                value = routineName,
                onValueChange = {
                    if (saveState != RoutineSaveState.SAVING) {
                        routineName = it
                        saveState = RoutineSaveState.IDLE
                        errorMessage = null
                    }
                },
                label = { Text("Nombre de la rutina") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                "Secuencia · ${selectedIds.size} actividades",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (selectedIds.isEmpty()) {
            item { EmptyState("Secuencia vacía", "Añade al menos una actividad para poder guardar.") }
        } else {
            items(selectedIds, key = { "selected-$it" }) { activityId ->
                val activity = catalog.activity(activityId)
                if (activity != null) {
                    SelectedStepCard(
                        activity = activity,
                        position = selectedIds.indexOf(activityId) + 1,
                        canMoveUp = selectedIds.first() != activityId,
                        canMoveDown = selectedIds.last() != activityId,
                        onMoveUp = { move(activityId, -1) },
                        onMoveDown = { move(activityId, 1) },
                        onRemove = { toggle(activityId) },
                        onSearchPictogram = { pictogramActivity = activity },
                        visual = RoutineStepVisualCodec.decode(visualByActivityId[activityId]),
                        onRemovePictogram = {
                            visualByActivityId = visualByActivityId - activityId
                            saveState = RoutineSaveState.IDLE
                            errorMessage = null
                        },
                    )
                }
            }
        }
        if (optionalActivityIds.any { it !in selectedIds }) {
            item {
                Text(
                    "Opcionales de la plantilla",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }
            items(optionalActivityIds.filterNot(selectedIds::contains), key = { "optional-$it" }) { id ->
                catalog.activity(id)?.let { activity ->
                    AddActivityCard(activity = activity, selected = false) { toggle(id) }
                }
            }
        }
        item {
            Text(
                "Añadir actividades",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (appMode == AppMode.ADVANCED) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar actividades") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { CategoryFilters(catalog, category) { category = it } }
        } else {
            item {
                InfoCallout(
                    "Modo simple",
                    "Usa Añadir o Quitar y los botones Subir y Bajar para preparar una secuencia breve.",
                )
            }
        }
        items(filteredActivities, key = CatalogActivity::id) { activity ->
            AddActivityCard(
                activity = activity,
                selected = activity.id in selectedIds,
                onToggle = { toggle(activity.id) },
            )
        }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("¿Deseas salir sin guardar los cambios?") },
            text = { Text("Los cambios de este borrador se perderán.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    onBack()
                }) { Text("Salir sin guardar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("Continuar editando") }
            },
        )
    }
    pictogramActivity?.let { activity ->
        ArasaacSearchDialog(
            activity = activity,
            repository = arasaacRepository,
            onDismiss = { pictogramActivity = null },
            onUsePictogram = { candidate ->
                if (candidate.id > 0 && candidate.source == RoutineStepVisualReference.SOURCE_ARASAAC) {
                    visualByActivityId = visualByActivityId + (activity.id to
                        RoutineStepVisualCodec.encode(RoutineStepVisualReference(
                            source = candidate.source,
                            pictogramId = candidate.id,
                            label = candidate.label,
                        )))
                    saveState = RoutineSaveState.IDLE
                    errorMessage = null
                    pictogramActivity = null
                }
            },
        )
    }
}

@Composable
private fun RoutineSaveActionBar(
    saving: Boolean,
    enabled: Boolean,
    editing: Boolean,
    onSave: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("routine-save-action-bar"),
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Button(
            onClick = onSave,
            enabled = enabled && !saving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("save-routine-action")
                .semantics {
                    contentDescription = when {
                        saving -> "Guardando rutina"
                        editing -> "Guardar cambios"
                        else -> "Guardar rutina"
                    }
                    stateDescription = when {
                        saving -> "Guardado en progreso"
                        enabled -> "Disponible"
                        else -> "No disponible: escribe un nombre y añade una actividad"
                    }
                    if (saving) liveRegion = LiveRegionMode.Polite
                },
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text("  Guardando…")
                } else {
                    Text(if (editing) "Guardar cambios" else "Guardar rutina")
                }
            }
        }
    }
}

@Composable
private fun SelectedStepCard(
    activity: CatalogActivity,
    position: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onSearchPictogram: () -> Unit,
    visual: RoutineStepVisualReference?,
    onRemovePictogram: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Paso $position · ${activity.name}", style = MaterialTheme.typography.titleMedium)
            Text(activity.simpleInstruction, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (visual == null) {
                VisualPlaceholder("Visual pendiente", size = 72.dp)
            } else {
                ArasaacPictogramImage(
                    ArasaacImageUrls.forPictogram(visual.pictogramId, ArasaacImageResolution.THUMBNAIL),
                    activity.name,
                    72,
                    offlineId = visual.pictogramId,
                )
                Text("${visual.label} · ARASAAC", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onSearchPictogram) {
                Text(if (visual == null) "Buscar pictograma" else "Cambiar pictograma")
            }
            if (visual != null) {
                TextButton(onClick = onRemovePictogram) { Text("Quitar pictograma") }
            }
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Subir ${activity.name}" },
                ) { Text("Subir") }
                TextButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Bajar ${activity.name}" },
                ) { Text("Bajar") }
                TextButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Quitar ${activity.name}" },
                ) { Text("Quitar") }
            }
        }
    }
}

@Composable
private fun AddActivityCard(activity: CatalogActivity, selected: Boolean, onToggle: () -> Unit) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (selected) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VisualPlaceholder("Visual pendiente", size = 56.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(activity.name, style = MaterialTheme.typography.titleMedium)
                Text(activity.simpleInstruction, style = MaterialTheme.typography.bodySmall)
            }
            Text(if (selected) "Quitar" else "Añadir", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CreateLanding(appMode: AppMode, onContinue: () -> Unit) {
    ScreenColumn {
        Text(
            "Crear una rutina",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        InfoCallout(
            if (appMode == AppMode.SIMPLE) "Creación sencilla" else "Personalización completa",
            "La rutina se guardará únicamente para el perfil activo.",
        )
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Elegir origen") }
    }
}

@Composable
private fun OriginScreen(onBack: () -> Unit, onTemplate: () -> Unit, onScratch: () -> Unit) {
    ScreenColumn {
        TextButton(onClick = onBack) { Text("‹ Volver a Crear") }
        Text(
            "Elegir origen",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        OriginCard("Usar una plantilla", "Personaliza una de las 52 rutinas maestras.", onTemplate)
        OriginCard("Crear desde cero", "Selecciona y ordena actividades del catálogo real.", onScratch)
    }
}

@Composable
private fun OriginCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TemplateCatalogScreen(
    appMode: AppMode,
    catalog: CatalogSnapshot,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val routines = catalog.routinesFor(query, category)
    val variant = appMode.catalogVariant()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { TextButton(onClick = onBack) { Text("‹ Volver a Elegir origen") } }
        item {
            Text(
                "Usar una plantilla",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
        }
        item { Text("${catalog.routines.size} plantillas reales disponibles") }
        if (appMode == AppMode.ADVANCED) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar plantillas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { CategoryFilters(catalog, category) { category = it } }
        }
        items(routines, key = CatalogRoutine::id) { routine ->
            Card(
                onClick = { onSelect(routine.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(routine.primaryArea, color = MaterialTheme.colorScheme.primary)
                    Text(routine.name, style = MaterialTheme.typography.titleMedium)
                    Text("${catalog.steps(routine.id, variant).size} pasos · ${routine.suggestedMoment}")
                    Text(routine.functionalObjective, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CategoryFilters(
    catalog: CatalogSnapshot,
    selected: String?,
    onSelected: (String?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelected(null) },
                label = { Text("Todas") },
            )
        }
        items(catalog.categories, key = { it.name }) { option ->
            FilterChip(
                selected = selected == option.name,
                onClick = { onSelected(option.name) },
                label = { Text(option.name) },
            )
        }
    }
}

@Composable
private fun CatalogLoading() {
    ScreenColumn {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("Preparando el catálogo para Crear…", modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun CatalogError(message: String) {
    ScreenColumn { EmptyState("No se pudo cargar el catálogo", message) }
}
