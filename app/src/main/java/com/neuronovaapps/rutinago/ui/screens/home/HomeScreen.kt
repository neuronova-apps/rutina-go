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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.demo.DemoContent
import com.neuronovaapps.rutinago.data.preferences.AppMode
import com.neuronovaapps.rutinago.domain.model.Routine
import com.neuronovaapps.rutinago.ui.components.EmptyState
import com.neuronovaapps.rutinago.ui.components.RoutineCard
import com.neuronovaapps.rutinago.ui.components.RutinaGoOutlinedButton
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.SectionHeading
import com.neuronovaapps.rutinago.ui.components.StatusPill
import com.neuronovaapps.rutinago.ui.components.VisualPlaceholder

@Composable
fun HomeScreen(
    activeProfileName: String,
    appMode: AppMode,
    onViewNowNext: () -> Unit,
    onOpenRoutine: (Routine) -> Unit,
    onStartRoutine: (Routine) -> Unit,
    onCreateRoutine: () -> Unit,
) {
    val nextRoutine = DemoContent.routines.firstOrNull()
    ScreenColumn {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Hoy · Lunes",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Tu próxima acción y las rutinas del día.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill("Demo")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
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
                Text("General", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (nextRoutine == null) {
            EmptyState(
                title = "No hay rutinas programadas para hoy",
                body = "Puedes usar una rutina según necesidad o crear una nueva.",
                actionLabel = "Crear rutina",
                onAction = onCreateRoutine,
            )
        } else {
            NextRoutineCard(
                routine = nextRoutine,
                onViewNowNext = onViewNowNext,
                onStart = { onStartRoutine(nextRoutine) },
            )
        }

        if (appMode == AppMode.ADVANCED) {
            SectionHeading("Más tarde")
            DemoContent.routines.drop(1).forEach { routine ->
                RoutineCard(
                    routine = routine,
                    onOpen = { onOpenRoutine(routine) },
                )
            }
        } else {
            Button(onClick = onCreateRoutine, modifier = Modifier.fillMaxWidth()) {
                Text("Crear una rutina")
            }
        }
    }
}

@Composable
private fun NextRoutineCard(
    routine: Routine,
    onViewNowNext: () -> Unit,
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Siguiente · 7:15", style = MaterialTheme.typography.labelMedium)
            Text(
                routine.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                routine.steps.take(2).forEachIndexed { index, step ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            if (index == 0) "AHORA" else "DESPUÉS",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        VisualPlaceholder(step.visualLabel)
                        Text(
                            step.title,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text("Iniciar rutina")
            }
            TextButton(onClick = onViewNowNext, modifier = Modifier.fillMaxWidth()) {
                Text("Ver Ahora / Después")
            }
        }
    }
}

@Composable
fun NowNextScreen(
    onStartRoutine: () -> Unit,
) {
    val steps = DemoContent.morningSteps
    ScreenColumn {
        Text(
            "Anticipación visual",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NowNextPanel(label = "AHORA", title = steps[0].title, visual = steps[0].visualLabel, emphasized = true)
        NowNextPanel(label = "DESPUÉS", title = steps[1].title, visual = steps[1].visualLabel, emphasized = false)
        Button(onClick = onStartRoutine, modifier = Modifier.fillMaxWidth()) {
            Text("Iniciar rutina")
        }
        RutinaGoOutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text("Escuchar: ${steps[0].title}. Después, ${steps[1].title}.")
        }
    }
}

@Composable
private fun NowNextPanel(
    label: String,
    title: String,
    visual: String,
    emphasized: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (emphasized) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = BorderStroke(
            if (emphasized) 2.dp else 1.dp,
            if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            VisualPlaceholder(visual, size = if (emphasized) 150.dp else 126.dp)
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
    }
}
