package com.neuronovaapps.rutinago.ui.screens.routines

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.demo.DemoContent
import com.neuronovaapps.rutinago.data.preferences.AppMode
import com.neuronovaapps.rutinago.domain.model.Routine
import com.neuronovaapps.rutinago.domain.model.RoutineCategory
import com.neuronovaapps.rutinago.domain.model.filterFor
import com.neuronovaapps.rutinago.ui.components.EmptyState
import com.neuronovaapps.rutinago.ui.components.InfoCallout
import com.neuronovaapps.rutinago.ui.components.RoutineCard
import com.neuronovaapps.rutinago.ui.components.RutinaGoOutlinedButton
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.SectionHeading
import com.neuronovaapps.rutinago.ui.components.StatusPill
import com.neuronovaapps.rutinago.ui.components.VisualPlaceholder

@Composable
fun RoutinesScreen(
    appMode: AppMode,
    onOpenRoutine: (Routine) -> Unit,
    onStartRoutine: (Routine) -> Unit,
    onUseTemplate: (Routine) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf(RoutineCategory.ALL.name) }
    val category = RoutineCategory.valueOf(categoryName)
    val matchingRoutines = DemoContent.routines.filterFor(query, category)
    val matchingTemplates = DemoContent.templates.filterFor(query, category)

    ScreenColumn {
        if (appMode == AppMode.ADVANCED) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Buscar rutinas o actividades") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(RoutineCategory.entries, key = { it.name }) { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = { categoryName = option.name },
                        label = { Text(option.label) },
                    )
                }
            }
        }

        SectionHeading("Mis rutinas")
        if (matchingRoutines.isEmpty()) {
            EmptyState(
                title = "No encontramos rutinas",
                body = "Prueba otra palabra o elige una categoría diferente.",
            )
        } else {
            matchingRoutines.forEach { routine ->
                RoutineCard(
                    routine = routine,
                    onOpen = { onOpenRoutine(routine) },
                    onStart = { onStartRoutine(routine) },
                )
            }
        }

        if (appMode == AppMode.ADVANCED) {
            SectionHeading("Catálogo de plantillas")
            InfoCallout(
                title = "Muestra de la biblioteca",
                body = "Esta fase usa plantillas demostrativas. El banco maestro se integrará después.",
            )
            matchingTemplates.forEach { routine ->
                RoutineCard(
                    routine = routine,
                    onOpen = { onOpenRoutine(routine) },
                    onStart = { onUseTemplate(routine) },
                )
            }
        } else {
            InfoCallout(
                title = "Modo simple",
                body = "Aquí se muestran solo tus rutinas. Usa Crear para empezar con una plantilla.",
            )
        }
    }
}

@Composable
fun RoutineDetailScreen(
    routine: Routine,
    onStart: () -> Unit,
    onUseTemplate: () -> Unit,
) {
    ScreenColumn {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(if (routine.isTemplate) "Plantilla" else "Disponible")
            Text(
                routine.category.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            routine.title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "${routine.steps.size} pasos · ${routine.scheduleLabel}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    VisualPlaceholder(step.visualLabel, size = 56.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(step.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            step.instruction,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text("Iniciar rutina")
        }
        RutinaGoOutlinedButton(onClick = onUseTemplate, modifier = Modifier.fillMaxWidth()) {
            Text(if (routine.isTemplate) "Usar esta plantilla" else "Crear una copia personal")
        }
    }
}
