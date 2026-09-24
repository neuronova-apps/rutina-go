package com.neuronovaapps.rutinago.ui.screens.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.demo.DemoContent
import com.neuronovaapps.rutinago.domain.model.ActivityStep
import com.neuronovaapps.rutinago.ui.components.InfoCallout
import com.neuronovaapps.rutinago.ui.components.RutinaGoOutlinedButton
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.VisualPlaceholder
import com.neuronovaapps.rutinago.ui.theme.LocalHighContrast

private enum class CreateOrigin(val label: String, val description: String) {
    TEMPLATE("Usar una plantilla", "Empieza con una estructura demostrativa editable."),
    SCRATCH("Crear desde cero", "Elige actividades y ordénalas a tu manera."),
}

@Composable
fun CreateScreen(suggestedTemplateTitle: String? = null) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var originName by rememberSaveable(suggestedTemplateTitle) {
        mutableStateOf(if (suggestedTemplateTitle == null) null else CreateOrigin.TEMPLATE.name)
    }
    var selectedIds by rememberSaveable {
        mutableStateOf(DemoContent.morningSteps.take(2).map { it.id })
    }
    var orderedIds by rememberSaveable {
        mutableStateOf(DemoContent.morningSteps.take(2).map { it.id })
    }
    var routineName by rememberSaveable { mutableStateOf(suggestedTemplateTitle ?: "Mi rutina") }
    var schedule by rememberSaveable { mutableStateOf("Según necesidad") }
    var reminder by rememberSaveable { mutableStateOf(false) }
    var showDemoResult by rememberSaveable { mutableStateOf(false) }
    val activities = DemoContent.morningSteps + DemoContent.routines[1].steps.take(1)

    ScreenColumn {
        Text(
            "Paso $step de 4",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            createStepTitle(step),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        if (suggestedTemplateTitle != null) {
            InfoCallout(
                title = "Plantilla elegida",
                body = suggestedTemplateTitle,
            )
        }

        when (step) {
            1 -> OriginStep(
                selected = originName?.let(CreateOrigin::valueOf),
                onSelected = { originName = it.name },
            )

            2 -> ActivitySelectionStep(
                activities = activities,
                selectedIds = selectedIds,
                onToggle = { id ->
                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                    orderedIds = orderedIds.filter { it in selectedIds } +
                        selectedIds.filter { it !in orderedIds }
                },
            )

            3 -> OrderingStep(
                orderedSteps = orderedIds.mapNotNull { id -> activities.find { it.id == id } },
                onMove = { index, delta ->
                    val target = index + delta
                    if (target in orderedIds.indices) {
                        val updated = orderedIds.toMutableList()
                        val moved = updated.removeAt(index)
                        updated.add(target, moved)
                        orderedIds = updated
                    }
                },
            )

            4 -> ScheduleStep(
                routineName = routineName,
                onRoutineNameChange = { routineName = it },
                schedule = schedule,
                onScheduleChange = { schedule = it },
                reminder = reminder,
                onReminderChange = { reminder = it },
            )
        }

        if (showDemoResult) {
            InfoCallout(
                title = "Rutina preparada para esta demostración",
                body = "“$routineName” no se guardó de forma permanente. La persistencia se incorporará en una fase posterior.",
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (step > 1) {
                RutinaGoOutlinedButton(
                    onClick = { step-- },
                    modifier = Modifier.weight(1f),
                ) { Text("Anterior") }
            }
            Button(
                onClick = {
                    if (step < 4) step++ else showDemoResult = true
                },
                enabled = when (step) {
                    1 -> originName != null
                    2 -> selectedIds.isNotEmpty()
                    4 -> routineName.isNotBlank()
                    else -> true
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (step < 4) "Continuar" else "Preparar rutina")
            }
        }
    }
}

private fun createStepTitle(step: Int) = when (step) {
    1 -> "Elegir origen"
    2 -> "Seleccionar actividades"
    3 -> "Organizar pasos"
    else -> "Programar y revisar"
}

@Composable
private fun OriginStep(
    selected: CreateOrigin?,
    onSelected: (CreateOrigin) -> Unit,
) {
    val highContrast = LocalHighContrast.current
    CreateOrigin.entries.forEach { option ->
        val isSelected = selected == option
        Card(
            onClick = { onSelected(option) },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected && highContrast) {
                    MaterialTheme.colorScheme.primary
                } else if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (isSelected && highContrast) {
                    MaterialTheme.colorScheme.onPrimary
                } else if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ),
            border = BorderStroke(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            ),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    if (isSelected) "✓ ${option.label}" else option.label,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    option.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected && highContrast) {
                        MaterialTheme.colorScheme.onPrimary
                    } else if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun ActivitySelectionStep(
    activities: List<ActivityStep>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
) {
    InfoCallout(
        title = "${selectedIds.size} pasos seleccionados",
        body = "Datos demostrativos; el banco de 203 actividades se conectará más adelante.",
    )
    activities.distinctBy { it.id }.forEach { activity ->
        Card(
            onClick = { onToggle(activity.id) },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VisualPlaceholder(activity.visualLabel, size = 56.dp)
                Text(activity.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Checkbox(
                    checked = activity.id in selectedIds,
                    onCheckedChange = { onToggle(activity.id) },
                )
            }
        }
    }
    RutinaGoOutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Text("Añadir paso personalizado")
    }
}

@Composable
private fun OrderingStep(
    orderedSteps: List<ActivityStep>,
    onMove: (index: Int, delta: Int) -> Unit,
) {
    orderedSteps.forEachIndexed { index, activity ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("${index + 1}", style = MaterialTheme.typography.titleMedium)
                VisualPlaceholder(activity.visualLabel, size = 52.dp)
                Text(activity.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Column {
                    TextButton(onClick = { onMove(index, -1) }, enabled = index > 0) { Text("↑") }
                    TextButton(onClick = { onMove(index, 1) }, enabled = index < orderedSteps.lastIndex) { Text("↓") }
                }
            }
        }
    }
    InfoCallout(
        title = "Opciones por paso",
        body = "La arquitectura admite fotografía, ayuda, duración y temporizador opcional en fases posteriores.",
    )
}

@Composable
private fun ScheduleStep(
    routineName: String,
    onRoutineNameChange: (String) -> Unit,
    schedule: String,
    onScheduleChange: (String) -> Unit,
    reminder: Boolean,
    onReminderChange: (Boolean) -> Unit,
) {
    OutlinedTextField(
        value = routineName,
        onValueChange = onRoutineNameChange,
        label = { Text("Nombre de la rutina") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Text("Cuándo aparece", style = MaterialTheme.typography.titleMedium)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(listOf("Según necesidad", "Mañana", "Tarde", "Noche")) { option ->
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
                Text(
                    "Opcional y desactivado por defecto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = reminder, onCheckedChange = onReminderChange)
        }
    }
    InfoCallout(
        title = "Sin hora obligatoria",
        body = "Puedes preparar una rutina sin asignar una hora exacta.",
    )
}
