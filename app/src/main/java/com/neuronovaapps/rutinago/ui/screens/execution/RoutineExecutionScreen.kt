package com.neuronovaapps.rutinago.ui.screens.execution

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.domain.model.Routine
import com.neuronovaapps.rutinago.domain.model.RoutineOutcome
import com.neuronovaapps.rutinago.ui.components.InfoCallout
import com.neuronovaapps.rutinago.ui.components.RutinaGoOutlinedButton
import com.neuronovaapps.rutinago.ui.components.StatusPill
import com.neuronovaapps.rutinago.ui.components.VisualPlaceholder

@Composable
fun RoutineExecutionScreen(
    routine: Routine,
    onClose: () -> Unit,
    onViewProgress: () -> Unit,
) {
    var stepIndex by rememberSaveable(routine.id) { mutableIntStateOf(0) }
    var paused by rememberSaveable(routine.id) { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var supportMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var outcomeName by rememberSaveable(routine.id) { mutableStateOf<String?>(null) }
    val outcome = outcomeName?.let(RoutineOutcome::valueOf)

    fun finish(result: RoutineOutcome) {
        outcomeName = result.name
        paused = false
    }

    BackHandler {
        if (outcome == null) {
            finish(if (stepIndex == 0) RoutineOutcome.CANCELLED else RoutineOutcome.PARTIAL)
        } else {
            onClose()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    routine.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        finish(if (stepIndex == 0) RoutineOutcome.CANCELLED else RoutineOutcome.PARTIAL)
                    },
                ) { Text("Salir") }
            }

            if (outcome == null) {
                val step = routine.steps[stepIndex]
                LinearProgressIndicator(
                    progress = { (stepIndex + 1f) / routine.steps.size },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (paused) {
                        Text(
                            "Rutina en pausa",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics {
                                heading()
                                liveRegion = LiveRegionMode.Polite
                            },
                        )
                        Text(
                            "Puedes continuar cuando estés listo.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        VisualPlaceholder(step.visualLabel, size = 240.dp)
                        Text(
                            "Paso ${stepIndex + 1} de ${routine.steps.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        Text(
                            step.title,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .semantics { heading() },
                        )
                        Text(
                            step.instruction,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    supportMessage?.let {
                        InfoCallout(
                            title = "Apoyo solicitado",
                            body = it,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (paused) {
                        Button(
                            onClick = { paused = false },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Continuar") }
                    } else {
                        RutinaGoOutlinedButton(
                            onClick = { showHelp = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Necesito ayuda") }
                        Button(
                            onClick = {
                                supportMessage = null
                                if (stepIndex < routine.steps.lastIndex) {
                                    stepIndex++
                                } else {
                                    finish(RoutineOutcome.COMPLETED)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Completar paso") }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            TextButton(onClick = { paused = true }) { Text("Pausar") }
                            TextButton(
                                onClick = {
                                    supportMessage = null
                                    if (stepIndex < routine.steps.lastIndex) {
                                        stepIndex++
                                    } else {
                                        finish(RoutineOutcome.PARTIAL)
                                    }
                                },
                            ) { Text("Omitir") }
                        }
                    }
                }
            } else {
                CompletionPanel(
                    outcome = outcome,
                    completedSteps = when (outcome) {
                        RoutineOutcome.COMPLETED -> routine.steps.size
                        RoutineOutcome.PARTIAL -> stepIndex.coerceAtLeast(1)
                        RoutineOutcome.CANCELLED -> 0
                    },
                    totalSteps = routine.steps.size,
                    onClose = onClose,
                    onViewProgress = onViewProgress,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("¿Qué necesitas?") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Pedir ayuda no cambia tu progreso ni añade penalizaciones.")
                    RutinaGoOutlinedButton(
                        onClick = {
                            supportMessage = "Una persona de apoyo puede acompañarte en este paso."
                            showHelp = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Pedir ayuda") }
                    RutinaGoOutlinedButton(
                        onClick = {
                            supportMessage = "Puedes leer de nuevo la instrucción y avanzar a tu ritmo."
                            showHelp = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Repetir instrucción") }
                    RutinaGoOutlinedButton(
                        onClick = {
                            paused = true
                            showHelp = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Hacer una pausa") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("Volver") }
            },
        )
    }
}

@Composable
private fun CompletionPanel(
    outcome: RoutineOutcome,
    completedSteps: Int,
    totalSteps: Int,
    onClose: () -> Unit,
    onViewProgress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StatusPill(outcome.label)
        Text(
            "Rutina finalizada",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 16.dp)
                .semantics { heading() },
        )
        Text(
            when (outcome) {
                RoutineOutcome.COMPLETED -> "$totalSteps de $totalSteps pasos realizados."
                RoutineOutcome.PARTIAL -> "$completedSteps de $totalSteps pasos realizados. Parcial no es un error."
                RoutineOutcome.CANCELLED -> "La rutina terminó sin penalización."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar y volver")
        }
        RutinaGoOutlinedButton(
            onClick = onViewProgress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) { Text("Ver progreso") }
    }
}
