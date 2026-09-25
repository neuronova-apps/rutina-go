package com.neuronovaapps.rutinago.ui.screens.create

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant
import com.neuronovaapps.rutinago.data.preferences.AppMode
import com.neuronovaapps.rutinago.domain.catalog.CatalogActivity
import com.neuronovaapps.rutinago.domain.catalog.CatalogRoutine
import com.neuronovaapps.rutinago.domain.catalog.CatalogSnapshot
import com.neuronovaapps.rutinago.domain.catalog.CatalogStepActivity
import com.neuronovaapps.rutinago.domain.catalog.CatalogUiState
import com.neuronovaapps.rutinago.domain.catalog.activitiesFor
import com.neuronovaapps.rutinago.domain.catalog.catalogVariant
import com.neuronovaapps.rutinago.domain.catalog.routinesFor
import com.neuronovaapps.rutinago.ui.components.EmptyState
import com.neuronovaapps.rutinago.ui.components.InfoCallout
import com.neuronovaapps.rutinago.ui.components.RutinaGoOutlinedButton
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.VisualPlaceholder

private enum class CreatePage { LANDING, ORIGIN, TEMPLATE_LIST, TEMPLATE_DETAIL, SCRATCH }

@Composable
fun CreateScreen(
    appMode: AppMode,
    catalogState: CatalogUiState,
    suggestedTemplateId: String? = null,
) {
    when (catalogState) {
        CatalogUiState.Loading -> CatalogLoading()
        is CatalogUiState.Error -> CatalogError(catalogState.message)
        is CatalogUiState.Ready -> CreateReadyScreen(
            appMode = appMode,
            catalog = catalogState.catalog,
            suggestedTemplateId = suggestedTemplateId,
        )
    }
}

@Composable
private fun CreateReadyScreen(
    appMode: AppMode,
    catalog: CatalogSnapshot,
    suggestedTemplateId: String?,
) {
    var pageName by rememberSaveable(suggestedTemplateId) {
        mutableStateOf(
            if (suggestedTemplateId == null) CreatePage.LANDING.name
            else CreatePage.TEMPLATE_DETAIL.name,
        )
    }
    var selectedTemplateId by rememberSaveable(suggestedTemplateId) {
        mutableStateOf(suggestedTemplateId)
    }
    var routineName by rememberSaveable { mutableStateOf("") }
    var selectedIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var baselineName by rememberSaveable { mutableStateOf("") }
    var baselineIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var prepared by rememberSaveable { mutableStateOf(false) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val page = CreatePage.valueOf(pageName)
    val hasScratchChanges = page == CreatePage.SCRATCH &&
        (routineName != baselineName || selectedIds != baselineIds)

    fun openScratch() {
        pageName = CreatePage.SCRATCH.name
        routineName = ""
        selectedIds = emptyList()
        baselineName = routineName
        baselineIds = selectedIds
        prepared = false
    }

    fun openTemplate(routineId: String) {
        selectedTemplateId = routineId
        pageName = CreatePage.TEMPLATE_DETAIL.name
        prepared = false
    }

    fun returnOneLevel(force: Boolean = false) {
        when (page) {
            CreatePage.LANDING -> Unit
            CreatePage.ORIGIN -> pageName = CreatePage.LANDING.name
            CreatePage.TEMPLATE_LIST -> pageName = CreatePage.ORIGIN.name
            CreatePage.TEMPLATE_DETAIL -> pageName = CreatePage.TEMPLATE_LIST.name
            CreatePage.SCRATCH -> {
                if (hasScratchChanges && !force) confirmExit = true
                else pageName = CreatePage.ORIGIN.name
            }
        }
    }

    BackHandler(enabled = page != CreatePage.LANDING) { returnOneLevel() }

    when (page) {
        CreatePage.LANDING -> CreateLanding(
            appMode = appMode,
            onContinue = { pageName = CreatePage.ORIGIN.name },
        )
        CreatePage.ORIGIN -> OriginScreen(
            onBack = { returnOneLevel() },
            onTemplate = { pageName = CreatePage.TEMPLATE_LIST.name },
            onScratch = ::openScratch,
        )
        CreatePage.TEMPLATE_LIST -> TemplateCatalogScreen(
            appMode = appMode,
            catalog = catalog,
            onBack = { returnOneLevel() },
            onSelect = ::openTemplate,
        )
        CreatePage.TEMPLATE_DETAIL -> TemplateDetailScreen(
            appMode = appMode,
            catalog = catalog,
            routineId = selectedTemplateId,
            prepared = prepared,
            onBack = { returnOneLevel() },
            onPrepare = { prepared = true },
        )
        CreatePage.SCRATCH -> ScratchScreen(
            appMode = appMode,
            catalog = catalog,
            routineName = routineName,
            onRoutineNameChange = {
                routineName = it
                prepared = false
            },
            selectedIds = selectedIds,
            onToggle = { id ->
                selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                prepared = false
            },
            prepared = prepared,
            onBack = { returnOneLevel() },
            onPrepare = {
                prepared = true
                baselineName = routineName
                baselineIds = selectedIds
            },
        )
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("¿Deseas salir sin guardar los cambios?") },
            text = { Text("Los datos introducidos en este borrador se perderán.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    returnOneLevel(force = true)
                }) { Text("Salir sin guardar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("Continuar editando") }
            },
        )
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
            title = if (appMode == AppMode.SIMPLE) "Tres decisiones sencillas" else "Catálogo completo",
            body = if (appMode == AppMode.SIMPLE) {
                "Elige un origen, un nombre y las actividades principales."
            } else {
                "Busca entre las plantillas y actividades reales del catálogo maestro."
            },
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
        OriginCard(
            title = "Usar una plantilla",
            description = "Elige una de las 52 rutinas del catálogo maestro.",
            onClick = onTemplate,
        )
        OriginCard(
            title = "Crear desde cero",
            description = "Elige entre las 203 actividades reales disponibles.",
            onClick = onScratch,
        )
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
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            TemplateCard(
                routine = routine,
                stepCount = catalog.steps(routine.id, variant).size,
                onClick = { onSelect(routine.id) },
            )
        }
    }
}

@Composable
private fun TemplateCard(routine: CatalogRoutine, stepCount: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(routine.primaryArea, color = MaterialTheme.colorScheme.primary)
            Text(routine.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "$stepCount pasos · ${routine.suggestedMoment}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(routine.functionalObjective, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TemplateDetailScreen(
    appMode: AppMode,
    catalog: CatalogSnapshot,
    routineId: String?,
    prepared: Boolean,
    onBack: () -> Unit,
    onPrepare: () -> Unit,
) {
    val routine = catalog.routine(routineId)
    if (routine == null) {
        ScreenColumn {
            TextButton(onClick = onBack) { Text("‹ Volver a Plantillas") }
            EmptyState("Plantilla no disponible", "No se encontró la plantilla seleccionada.")
        }
        return
    }
    val variant = appMode.catalogVariant()
    val mainSteps = catalog.activitiesFor(routine.id, variant)
    val optional = if (appMode == AppMode.ADVANCED) {
        catalog.activitiesFor(routine.id, RoutineVariant.OPTIONAL)
    } else {
        emptyList()
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { TextButton(onClick = onBack) { Text("‹ Volver a Plantillas") } }
        item {
            Text(
                routine.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
        }
        item { InfoCallout("Plantilla maestra", routine.functionalObjective) }
        item {
            Text(
                if (variant == RoutineVariant.SIMPLE) "Secuencia simple" else "Secuencia completa",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
        items(mainSteps, key = { "${it.step.variant}-${it.step.position}" }) { item ->
            TemplateStepCard(item)
        }
        if (optional.isNotEmpty()) {
            item {
                InfoCallout(
                    "Actividades opcionales",
                    "Se consultan por separado y no se agregan automáticamente al borrador.",
                )
            }
            items(optional, key = { "optional-${it.step.position}" }) { item ->
                TemplateStepCard(item, optional = true)
            }
        }
        if (prepared) {
            item {
                InfoCallout(
                    "Borrador preparado",
                    "La plantilla se mantiene en memoria. Todavía no se guarda como rutina personal.",
                )
            }
        }
        item {
            Button(onClick = onPrepare, modifier = Modifier.fillMaxWidth()) {
                Text("Preparar borrador con esta plantilla")
            }
        }
    }
}

@Composable
private fun ScratchScreen(
    appMode: AppMode,
    catalog: CatalogSnapshot,
    routineName: String,
    onRoutineNameChange: (String) -> Unit,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    prepared: Boolean,
    onBack: () -> Unit,
    onPrepare: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val activities = catalog.activitiesFor(query, category)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { TextButton(onClick = onBack) { Text("‹ Volver a Elegir origen") } }
        item {
            Text(
                "Crear desde cero",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
        }
        item {
            OutlinedTextField(
                value = routineName,
                onValueChange = onRoutineNameChange,
                label = { Text("Nombre de la rutina") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                "Actividades · ${catalog.activities.size} disponibles · ${selectedIds.size} seleccionadas",
                style = MaterialTheme.typography.titleMedium,
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
                    "Selecciona solo las actividades esenciales; todas provienen del mismo catálogo maestro.",
                )
            }
        }
        if (activities.isEmpty()) {
            item { EmptyState("Sin resultados", "Prueba otra búsqueda o categoría.") }
        }
        items(activities, key = CatalogActivity::id) { activity ->
            ActivityCard(
                activity = activity,
                selected = activity.id in selectedIds,
                onToggle = { onToggle(activity.id) },
            )
        }
        if (appMode == AppMode.ADVANCED && selectedIds.isNotEmpty()) {
            item {
                InfoCallout(
                    "Orden de actividades",
                    selectedIds.mapNotNull { catalog.activity(it)?.name }.joinToString(" → "),
                )
            }
        }
        if (prepared) {
            item {
                InfoCallout(
                    "Borrador preparado",
                    "La rutina se mantiene en memoria y todavía no se guarda permanentemente.",
                )
            }
        }
        item {
            Button(
                onClick = onPrepare,
                enabled = routineName.isNotBlank() && selectedIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Preparar borrador") }
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
private fun ActivityCard(activity: CatalogActivity, selected: Boolean, onToggle: () -> Unit) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (selected) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VisualPlaceholder("Visual pendiente", size = if (selected) 68.dp else 56.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(activity.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    activity.simpleInstruction,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun TemplateStepCard(item: CatalogStepActivity, optional: Boolean = false) {
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
            Text(item.step.position.toString(), color = MaterialTheme.colorScheme.primary)
            VisualPlaceholder("Visual pendiente", size = 56.dp)
            Column(modifier = Modifier.weight(1f)) {
                if (optional) Text("OPCIONAL", style = MaterialTheme.typography.labelSmall)
                Text(item.activity.name, style = MaterialTheme.typography.titleMedium)
                Text(item.activity.simpleInstruction, style = MaterialTheme.typography.bodySmall)
            }
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
    ScreenColumn {
        EmptyState("No se pudo cargar el catálogo", message)
    }
}
