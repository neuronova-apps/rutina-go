package com.neuronovaapps.rutinago.ui.screens.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.domain.execution.ExecutionStatus
import com.neuronovaapps.rutinago.domain.execution.ProfileProgress
import com.neuronovaapps.rutinago.domain.execution.RoutineExecution
import com.neuronovaapps.rutinago.ui.components.EmptyState
import com.neuronovaapps.rutinago.ui.components.InfoCallout
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.SectionHeading
import com.neuronovaapps.rutinago.ui.components.StatusPill
import java.text.DateFormat
import java.util.Date

@Composable
fun ProgressScreen(activeProfileName: String, progress: ProfileProgress) {
    ScreenColumn {
        InfoCallout(
            title = "Progreso de $activeProfileName",
            body = "Este resumen se calcula con las ejecuciones guardadas en este perfil.",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProgressStat(progress.completedRoutines.toString(), "Completadas", Modifier.weight(1f))
            ProgressStat(progress.partialRoutines.toString(), "Parciales", Modifier.weight(1f))
            ProgressStat(progress.completedActivities.toString(), "Actividades", Modifier.weight(1f))
        }

        SectionHeading("Historial")
        if (progress.history.isEmpty()) {
            EmptyState(
                title = "Aún no hay rutinas realizadas",
                body = "Cuando inicies una rutina personal, su resultado aparecerá aquí.",
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    progress.history.forEachIndexed { index, execution ->
                        HistoryRow(execution)
                        if (index < progress.history.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(execution: RoutineExecution) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(execution.routineName, style = MaterialTheme.typography.titleSmall)
            Text(
                formatDate(execution.finishedAtEpochMillis ?: execution.startedAtEpochMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${execution.completedSteps} completados · ${execution.skippedSteps} omitidos · " +
                    "${execution.totalSteps} en total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusPill(
            when (execution.status) {
                ExecutionStatus.IN_PROGRESS -> "En curso"
                ExecutionStatus.COMPLETED -> "Completada"
                ExecutionStatus.PARTIAL -> "Parcial"
                ExecutionStatus.ABANDONED -> "Abandonada"
            },
        )
    }
}

@Composable
private fun ProgressStat(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
