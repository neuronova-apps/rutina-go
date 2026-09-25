package com.neuronovaapps.rutinago.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.domain.execution.ProfileProgress
import com.neuronovaapps.rutinago.domain.execution.RoutineExecution
import com.neuronovaapps.rutinago.ui.components.EmptyState
import com.neuronovaapps.rutinago.ui.components.InfoCallout
import com.neuronovaapps.rutinago.ui.components.RutinaGoOutlinedButton
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.SectionHeading

@Composable
fun HomeScreen(
    activeProfileName: String,
    activeExecution: RoutineExecution?,
    progress: ProfileProgress,
    onResumeExecution: () -> Unit,
    onOpenRoutines: () -> Unit,
    onCreateRoutine: () -> Unit,
) {
    ScreenColumn {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Perfil activo", style = MaterialTheme.typography.labelMedium)
                    Text(activeProfileName, style = MaterialTheme.typography.titleMedium)
                }
                Text("Hoy", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (activeExecution != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("RUTINA EN CURSO", style = MaterialTheme.typography.labelMedium)
                    Text(
                        activeExecution.routineName,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        "Paso ${activeExecution.currentStepPosition ?: 1} de ${activeExecution.totalSteps} · " +
                            "avance guardado",
                    )
                    Button(onClick = onResumeExecution, modifier = Modifier.fillMaxWidth()) {
                        Text("Reanudar rutina")
                    }
                }
            }
        } else {
            EmptyState(
                title = "No hay rutinas programadas para hoy",
                body = "RutinaGo todavía no programa horarios. Puedes iniciar una rutina personal desde Mis rutinas.",
                actionLabel = "Ver Mis rutinas",
                onAction = onOpenRoutines,
            )
        }

        SectionHeading("Actividad real")
        if (progress.history.isEmpty()) {
            InfoCallout(
                title = "Aún no hay rutinas realizadas",
                body = "Aquí aparecerá un resumen cuando completes o finalices una rutina.",
            )
        } else {
            InfoCallout(
                title = "Progreso guardado",
                body = "${progress.completedRoutines} rutinas completadas, " +
                    "${progress.partialRoutines} parciales y ${progress.completedActivities} actividades realizadas.",
            )
        }

        RutinaGoOutlinedButton(onClick = onCreateRoutine, modifier = Modifier.fillMaxWidth()) {
            Text("Crear una rutina")
        }
    }
}
