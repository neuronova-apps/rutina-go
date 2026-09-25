package com.neuronovaapps.rutinago.ui.screens.routines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant
import com.neuronovaapps.rutinago.data.preferences.AppMode
import com.neuronovaapps.rutinago.domain.catalog.CatalogRoutine
import com.neuronovaapps.rutinago.domain.catalog.CatalogSnapshot
import com.neuronovaapps.rutinago.domain.catalog.CatalogStepActivity
import com.neuronovaapps.rutinago.domain.catalog.CatalogUiState
import com.neuronovaapps.rutinago.domain.catalog.catalogVariant
import com.neuronovaapps.rutinago.domain.catalog.routinesFor
import com.neuronovaapps.rutinago.domain.model.Routine
import com.neuronovaapps.rutinago.domain.user.UserRoutine
import com.neuronovaapps.rutinago.ui.components.EmptyState
import com.neuronovaapps.rutinago.ui.components.InfoCallout
import com.neuronovaapps.rutinago.ui.components.RutinaGoOutlinedButton
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.StatusPill
import com.neuronovaapps.rutinago.ui.components.VisualPlaceholder
import kotlinx.coroutines.launch

@Composable
fun RoutinesScreen(
    appMode: AppMode,
    catalogState: CatalogUiState,
    activeProfileName: String,
    userRoutines: List<UserRoutine>,
    onOpenRoutine: (String) -> Unit,
    onUseTemplate: (String) -> Unit,
    onEditUserRoutine: (String) -> Unit,
    onArchiveUserRoutine: suspend (String) -> Unit,
) {
    when (catalogState) {
        CatalogUiState.Loading -> CatalogLoading("Preparando las rutinas del catálogo…")
        is CatalogUiState.Error -> CatalogError(catalogState.message)
        is CatalogUiState.Ready -> RoutinesCatalog(
            appMode = appMode,
            catalog = catalogState.catalog,
            activeProfileName = activeProfileName,
            userRoutines = userRoutines,
            onOpenRoutine = onOpenRoutine,
            onUseTemplate = onUseTemplate,
            onEditUserRoutine = onEditUserRoutine,
            onArchiveUserRoutine = onArchiveUserRoutine,
        )
    }
}

@Composable
private fun RoutinesCatalog(
    appMode: AppMode,
    catalog: CatalogSnapshot,
    activeProfileName: String,
    userRoutines: List<UserRoutine>,
    onOpenRoutine: (String) -> Unit,
    onUseTemplate: (String) -> Unit,
    onEditUserRoutine: (String) -> Unit,
    onArchiveUserRoutine: suspend (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var archiveId by rememberSaveable { mutableStateOf<String?>(null) }
    var archiveError by rememberSaveable { mutableStateOf<String?>(null) }
    var archivedName by rememberSaveable { mutableStateOf<String?>(null) }
    val routines = catalog.routinesFor(query, category)
    val variant = appMode.catalogVariant()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "Mis rutinas",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
        }
        item {
            Text(
                "Perfil $activeProfileName · ${userRoutines.size} guardadas",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        archiveError?.let { message -> item { InfoCallout("No se pudo archivar", message) } }
        archivedName?.let { name -> item { InfoCallout("Rutina archivada", "$name dejó de aparecer en Mis rutinas.") } }
        if (userRoutines.isEmpty()) {
            item {
                EmptyState(
                    title = "Aún no hay rutinas guardadas",
                    body = "Crea una desde cero o adapta una plantilla para este perfil.",
                )
            }
        } else {
            items(userRoutines, key = UserRoutine::id) { routine ->
                UserRoutineCard(
                    routine = routine,
                    catalog = catalog,
                    onEdit = { onEditUserRoutine(routine.id) },
                    onArchive = { archiveId = routine.id },
                )
            }
        }
        item {
            Text(
                "Explorar plantillas",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
        }
        item {
            Text(
                "${catalog.routines.size} rutinas maestras · ${catalog.categories.size} categorías",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (appMode == AppMode.ADVANCED) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar rutinas o actividades") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { CategoryFilters(catalog, category) { category = it } }
        } else {
            item {
                InfoCallout(
                    title = "Modo simple",
                    body = "Explora el mismo catálogo maestro con la secuencia esencial de cada rutina.",
                )
            }
        }
        if (routines.isEmpty()) {
            item {
                EmptyState(
                    title = "No encontramos rutinas",
                    body = "Prueba otra palabra o elige una categoría diferente.",
                )
            }
        } else {
            items(routines, key = CatalogRoutine::id) { routine ->
                CatalogRoutineCard(
                    routine = routine,
                    stepCount = catalog.steps(routine.id, variant).size,
                    showTemplateAction = appMode == AppMode.ADVANCED,
                    onOpen = { onOpenRoutine(routine.id) },
                    onUseTemplate = { onUseTemplate(routine.id) },
                )
            }
        }
    }

    val routineToArchive = userRoutines.firstOrNull { it.id == archiveId }
    if (routineToArchive != null) {
        AlertDialog(
            onDismissRequest = { archiveId = null },
            title = { Text("¿Archivar rutina?") },
            text = { Text("${routineToArchive.name} dejará de aparecer en la lista principal.") },
            confirmButton = {
                TextButton(onClick = {
                    archiveId = null
                    archiveError = null
                    scope.launch {
                        runCatching { onArchiveUserRoutine(routineToArchive.id) }
                            .onSuccess { archivedName = routineToArchive.name }
                            .onFailure {
                                archiveError = it.message ?: "No fue posible completar la operación."
                            }
                    }
                }) { Text("Archivar") }
            },
            dismissButton = {
                TextButton(onClick = { archiveId = null }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun UserRoutineCard(
    routine: UserRoutine,
    catalog: CatalogSnapshot,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
) {
    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("RUTINA PERSONAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(routine.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${routine.steps.size} pasos" + if (routine.sourceTemplateId != null) " · Adaptada de plantilla" else " · Creada desde cero",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                routine.steps.mapNotNull { catalog.activity(it.activityId)?.name }
                    .take(3).joinToString(" → "),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RutinaGoOutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text("Editar")
                }
                RutinaGoOutlinedButton(onClick = onArchive, modifier = Modifier.weight(1f)) {
                    Text("Archivar")
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
private fun CatalogRoutineCard(
    routine: CatalogRoutine,
    stepCount: Int,
    showTemplateAction: Boolean,
    onOpen: () -> Unit,
    onUseTemplate: () -> Unit,
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    routine.primaryArea,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Text("$stepCount pasos", style = MaterialTheme.typography.labelLarge)
            }
            Text(routine.name, style = MaterialTheme.typography.titleMedium)
            Text(
                routine.functionalObjective,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                routine.suggestedMoment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showTemplateAction) {
                RutinaGoOutlinedButton(onClick = onUseTemplate, modifier = Modifier.fillMaxWidth()) {
                    Text("Usar como plantilla")
                }
            }
        }
    }
}

@Composable
fun CatalogRoutineDetailScreen(
    catalogState: CatalogUiState,
    routineId: String?,
    appMode: AppMode,
    onUseTemplate: (String) -> Unit,
) {
    when (catalogState) {
        CatalogUiState.Loading -> CatalogLoading("Cargando el detalle de la rutina…")
        is CatalogUiState.Error -> CatalogError(catalogState.message)
        is CatalogUiState.Ready -> {
            val routine = catalogState.catalog.routine(routineId)
            if (routine == null) {
                CatalogError("No se encontró la rutina seleccionada.")
            } else {
                CatalogRoutineDetail(
                    catalog = catalogState.catalog,
                    routine = routine,
                    appMode = appMode,
                    onUseTemplate = { onUseTemplate(routine.id) },
                )
            }
        }
    }
}

@Composable
private fun CatalogRoutineDetail(
    catalog: CatalogSnapshot,
    routine: CatalogRoutine,
    appMode: AppMode,
    onUseTemplate: () -> Unit,
) {
    val mainVariant = appMode.catalogVariant()
    val mainSteps = catalog.activitiesFor(routine.id, mainVariant)
    val optionalSteps = if (appMode == AppMode.ADVANCED) {
        catalog.activitiesFor(routine.id, RoutineVariant.OPTIONAL)
    } else {
        emptyList()
    }
    ScreenColumn {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill("Plantilla maestra")
            Text(
                routine.primaryArea,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            routine.name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "${mainSteps.size} pasos · ${routine.suggestedMoment}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        InfoCallout("Objetivo", routine.functionalObjective)
        InfoCallout("Contexto", routine.context)
        if (routine.keySupervision.isNotBlank()) InfoCallout("Supervisión", routine.keySupervision)
        Text(
            if (mainVariant == RoutineVariant.SIMPLE) "Secuencia simple" else "Secuencia completa",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        mainSteps.forEach { item -> CatalogStepCard(item) }
        if (appMode == AppMode.ADVANCED && optionalSteps.isNotEmpty()) {
            Text(
                "Actividades opcionales",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            InfoCallout(
                "Opcionales",
                "Se muestran aparte y no se añaden automáticamente a la secuencia completa.",
            )
            optionalSteps.forEach { item -> CatalogStepCard(item, optional = true) }
        }
        RutinaGoOutlinedButton(onClick = onUseTemplate, modifier = Modifier.fillMaxWidth()) {
            Text("Usar esta plantilla")
        }
        InfoCallout(
            title = "Inicio pendiente",
            body = "La ejecución con datos maestros se habilitará en la siguiente fase. Aquí no se generan resultados demostrativos.",
        )
    }
}

@Composable
private fun CatalogStepCard(item: CatalogStepActivity, optional: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = item.step.position.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            VisualPlaceholder("Visual pendiente", size = 56.dp)
            Column(modifier = Modifier.weight(1f)) {
                if (optional) {
                    Text(
                        "OPCIONAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(item.activity.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    item.activity.simpleInstruction,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CatalogLoading(message: String) {
    ScreenColumn {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(message, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun CatalogError(message: String) {
    ScreenColumn {
        EmptyState(title = "No se pudo cargar el catálogo", body = message)
    }
}

@Composable
fun LegacyRoutineDetailScreen(
    routine: Routine,
    onStart: () -> Unit,
    onUseTemplate: () -> Unit,
) {
    ScreenColumn {
        StatusPill(if (routine.isTemplate) "Plantilla demo" else "Rutina demo")
        Text(
            routine.title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        routine.steps.forEachIndexed { index, step ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("${index + 1}", style = MaterialTheme.typography.titleMedium)
                    VisualPlaceholder(step.visualLabel, size = 56.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(step.title, style = MaterialTheme.typography.titleMedium)
                        Text(step.instruction, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        TextButton(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Iniciar rutina demo") }
        RutinaGoOutlinedButton(onClick = onUseTemplate, modifier = Modifier.fillMaxWidth()) {
            Text("Explorar plantillas maestras")
        }
    }
}
