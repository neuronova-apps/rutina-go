package com.neuronovaapps.rutinago.ui.screens.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.neuronovaapps.rutinago.data.demo.DemoContent
import com.neuronovaapps.rutinago.data.preferences.AppMode
import com.neuronovaapps.rutinago.domain.model.ActivityStep
import com.neuronovaapps.rutinago.ui.components.InfoCallout
import com.neuronovaapps.rutinago.ui.components.RutinaGoOutlinedButton
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.VisualPlaceholder

private enum class CreatePage { LANDING, ORIGIN, TEMPLATE, SCRATCH }

@Composable
fun CreateScreen(
    appMode: AppMode,
    suggestedTemplateTitle: String? = null,
) {
    val activities = (DemoContent.morningSteps + DemoContent.routines[1].steps.take(1)).distinctBy { it.id }
    val defaultTemplate = suggestedTemplateTitle ?: DemoContent.templates.first().title
    var pageName by rememberSaveable(suggestedTemplateTitle) {
        mutableStateOf(if (suggestedTemplateTitle == null) CreatePage.LANDING.name else CreatePage.TEMPLATE.name)
    }
    var routineName by rememberSaveable { mutableStateOf(if (suggestedTemplateTitle == null) "" else defaultTemplate) }
    var selectedIds by rememberSaveable {
        mutableStateOf<List<String>>(
            if (suggestedTemplateTitle == null) emptyList() else activities.take(2).map { it.id },
        )
    }
    var schedule by rememberSaveable { mutableStateOf("Según necesidad") }
    var reminder by rememberSaveable { mutableStateOf(false) }
    var baselineName by rememberSaveable { mutableStateOf(routineName) }
    var baselineIds by rememberSaveable { mutableStateOf(selectedIds) }
    var baselineSchedule by rememberSaveable { mutableStateOf(schedule) }
    var baselineReminder by rememberSaveable { mutableStateOf(reminder) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    var prepared by rememberSaveable { mutableStateOf(false) }
    val page = CreatePage.valueOf(pageName)
    val isEditor = page == CreatePage.TEMPLATE || page == CreatePage.SCRATCH
    val hasChanges = isEditor && (
        routineName != baselineName || selectedIds != baselineIds ||
            schedule != baselineSchedule || reminder != baselineReminder
        )

    fun openEditor(target: CreatePage) {
        pageName = target.name
        routineName = if (target == CreatePage.TEMPLATE) defaultTemplate else ""
        selectedIds = if (target == CreatePage.TEMPLATE) activities.take(2).map { it.id } else emptyList()
        schedule = "Según necesidad"
        reminder = false
        baselineName = routineName
        baselineIds = selectedIds
        baselineSchedule = schedule
        baselineReminder = reminder
        prepared = false
    }

    fun returnOneLevel(force: Boolean = false) {
        when (page) {
            CreatePage.LANDING -> Unit
            CreatePage.ORIGIN -> pageName = CreatePage.LANDING.name
            CreatePage.TEMPLATE, CreatePage.SCRATCH -> {
                if (hasChanges && !force) confirmExit = true else pageName = CreatePage.ORIGIN.name
            }
        }
    }

    BackHandler(enabled = page != CreatePage.LANDING) { returnOneLevel() }

    ScreenColumn {
        when (page) {
            CreatePage.LANDING -> CreateLanding(
                appMode = appMode,
                onContinue = { pageName = CreatePage.ORIGIN.name },
            )

            CreatePage.ORIGIN -> OriginScreen(
                onBack = { returnOneLevel() },
                onTemplate = { openEditor(CreatePage.TEMPLATE) },
                onScratch = { openEditor(CreatePage.SCRATCH) },
            )

            CreatePage.TEMPLATE, CreatePage.SCRATCH -> EditorScreen(
                page = page,
                appMode = appMode,
                activities = activities,
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
                schedule = schedule,
                onScheduleChange = {
                    schedule = it
                    prepared = false
                },
                reminder = reminder,
                onReminderChange = {
                    reminder = it
                    prepared = false
                },
                prepared = prepared,
                onBack = { returnOneLevel() },
                onPrepare = {
                    prepared = true
                    baselineName = routineName
                    baselineIds = selectedIds
                    baselineSchedule = schedule
                    baselineReminder = reminder
                },
            )
        }
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
    Text(
        "Crear una rutina",
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { heading() },
    )
    InfoCallout(
        title = if (appMode == AppMode.SIMPLE) "Tres decisiones sencillas" else "Configuración completa",
        body = if (appMode == AppMode.SIMPLE) {
            "Elige un origen, un nombre y las actividades principales."
        } else {
            "También podrás ajustar actividades, horario y recordatorio."
        },
    )
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Elegir origen") }
}

@Composable
private fun OriginScreen(onBack: () -> Unit, onTemplate: () -> Unit, onScratch: () -> Unit) {
    TextButton(onClick = onBack) { Text("‹ Volver a Crear") }
    Text(
        "Elegir origen",
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { heading() },
    )
    OriginCard(
        title = "Usar una plantilla",
        description = "Parte de una estructura demostrativa editable.",
        onClick = onTemplate,
    )
    OriginCard(
        title = "Crear desde cero",
        description = "Elige las actividades principales de tu rutina.",
        onClick = onScratch,
    )
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
private fun EditorScreen(
    page: CreatePage,
    appMode: AppMode,
    activities: List<ActivityStep>,
    routineName: String,
    onRoutineNameChange: (String) -> Unit,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    schedule: String,
    onScheduleChange: (String) -> Unit,
    reminder: Boolean,
    onReminderChange: (Boolean) -> Unit,
    prepared: Boolean,
    onBack: () -> Unit,
    onPrepare: () -> Unit,
) {
    TextButton(onClick = onBack) { Text("‹ Volver a Elegir origen") }
    Text(
        if (page == CreatePage.TEMPLATE) "Usar una plantilla" else "Crear desde cero",
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { heading() },
    )
    if (page == CreatePage.TEMPLATE) {
        InfoCallout(
            title = "Plantilla demostrativa",
            body = "Puedes personalizarla. El banco maestro se incorporará en la siguiente fase.",
        )
    }
    OutlinedTextField(
        value = routineName,
        onValueChange = onRoutineNameChange,
        label = { Text("Nombre de la rutina") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Text("Actividades", style = MaterialTheme.typography.titleMedium)
    activities.take(if (appMode == AppMode.SIMPLE) 2 else activities.size).forEach { activity ->
        ActivityCard(activity, activity.id in selectedIds) { onToggle(activity.id) }
    }
    if (appMode == AppMode.ADVANCED) {
        Text("Cuándo aparece", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(3) { index ->
                val option = listOf("Según necesidad", "Mañana", "Tarde")[index]
                FilterChip(
                    selected = schedule == option,
                    onClick = { onScheduleChange(option) },
                    label = { Text(option) },
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Recordatorio", style = MaterialTheme.typography.titleMedium)
                    Text("Opcional", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = reminder, onCheckedChange = onReminderChange)
            }
        }
        InfoCallout(
            title = "Orden de actividades",
            body = selectedIds.mapNotNull { id -> activities.find { it.id == id }?.title }
                .joinToString(" → ").ifBlank { "Selecciona al menos una actividad." },
        )
    }
    if (prepared) {
        InfoCallout(
            title = "Borrador preparado",
            body = "La rutina aún no se guarda de forma permanente; esa persistencia llegará con el banco maestro.",
        )
    }
    Button(
        onClick = onPrepare,
        enabled = routineName.isNotBlank() && selectedIds.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Preparar rutina") }
}

@Composable
private fun ActivityCard(activity: ActivityStep, selected: Boolean, onToggle: () -> Unit) {
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
            VisualPlaceholder(activity.visualLabel, size = if (selected) 68.dp else 56.dp)
            Text(activity.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
        }
    }
}
