package com.neuronovaapps.rutinago.ui.screens.execution

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.domain.execution.ExecutionStatus
import com.neuronovaapps.rutinago.domain.execution.RoutineExecution
import com.neuronovaapps.rutinago.ui.components.InfoCallout
import com.neuronovaapps.rutinago.ui.components.RutinaGoOutlinedButton
import com.neuronovaapps.rutinago.ui.components.StatusPill
import com.neuronovaapps.rutinago.ui.components.VisualPlaceholder
import kotlinx.coroutines.launch

@Composable
fun RoutineExecutionScreen(
    execution: RoutineExecution,
    onCompleteStep: suspend (Int) -> RoutineExecution,
    onSkipStep: suspend (Int) -> RoutineExecution,
    onFinishPartial: suspend () -> RoutineExecution,
    onAbandon: suspend () -> Unit,
    onExecutionChanged: (RoutineExecution) -> Unit,
    onExit: () -> Unit,
    onCloseResult: () -> Unit,
    onViewProgress: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var paused by rememberSaveable(execution.id) { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var supportMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showExitConfirmation by rememberSaveable(execution.id) { mutableStateOf(false) }
    var showFinishConfirmation by rememberSaveable(execution.id) { mutableStateOf(false) }
    var actionInProgress by rememberSaveable(execution.id) { mutableStateOf(false) }
    var actionError by rememberSaveable(execution.id) { mutableStateOf<String?>(null) }
    val isRunning = execution.status == ExecutionStatus.IN_PROGRESS

    fun runAction(action: suspend () -> RoutineExecution) {
        if (actionInProgress) return
        actionInProgress = true
        actionError = null
        scope.launch {
            runCatching { action() }
                .onSuccess(onExecutionChanged)
                .onFailure { actionError = it.message ?: "No fue posible guardar el avance." }
            actionInProgress = false
        }
    }

    BackHandler {
        when {
            showExitConfirmation -> showExitConfirmation = false
            showFinishConfirmation -> showFinishConfirmation = false
            isRunning -> showExitConfirmation = true
            else -> onCloseResult()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                    execution.routineName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (isRunning) {
                    TextButton(onClick = { showExitConfirmation = true }, enabled = !actionInProgress) {
                        Text("Salir")
                    }
                }
            }

            if (isRunning) {
                val step = execution.currentStep
                if (step == null) {
                    InfoCallout(
                        title = "No se pudo recuperar el paso actual",
                        body = "Sal de la rutina y vuelve a abrirla desde Mis rutinas.",
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { step.position.toFloat() / execution.totalSteps.coerceAtLeast(1) },
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
                                "Puedes continuar cuando estés listo. Tu avance está guardado.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            VisualPlaceholder(
                                if (step.visualReference == null) "Visual pendiente" else "Recurso visual",
                                size = 240.dp,
                            )
                            Text(
                                "Paso ${step.position} de ${execution.totalSteps}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                            Text(
                                step.activityName,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp).semantics { heading() },
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
                            InfoCallout("Apoyo solicitado", it, Modifier.padding(top = 16.dp))
                        }
                        actionError?.let {
                            InfoCallout("No se guardó el cambio", it, Modifier.padding(top = 16.dp))
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (paused) {
                            Button(onClick = { paused = false }, modifier = Modifier.fillMaxWidth()) {
                                Text("Continuar")
                            }
                        } else {
                            RutinaGoOutlinedButton(
                                onClick = { showHelp = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !actionInProgress,
                            ) { Text("Necesito ayuda") }
                            Button(
                                onClick = {
                                    supportMessage = null
                                    runAction { onCompleteStep(step.position) }
                                },
                                enabled = !actionInProgress,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(if (actionInProgress) "Guardando…" else "Completar paso") }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                TextButton(onClick = { paused = true }, enabled = !actionInProgress) {
                                    Text("Pausar")
                                }
                                TextButton(
                                    onClick = {
                                        supportMessage = null
                                        runAction { onSkipStep(step.position) }
                                    },
                                    enabled = !actionInProgress,
                                ) { Text("Omitir") }
                            }
                            if (execution.completedSteps + execution.skippedSteps > 0) {
                                TextButton(
                                    onClick = { showFinishConfirmation = true },
                                    enabled = !actionInProgress,
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("Finalizar por ahora") }
                            }
                        }
                    }
                }
            } else {
                CompletionPanel(
                    execution = execution,
                    onClose = onCloseResult,
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
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Volver") } },
        )
    }

    if (showFinishConfirmation) {
        AlertDialog(
            onDismissRequest = { showFinishConfirmation = false },
            title = { Text("¿Finalizar por ahora?") },
            text = { Text("Se guardará como una rutina realizada parcialmente.") },
            confirmButton = {
                TextButton(onClick = {
                    showFinishConfirmation = false
                    runAction(onFinishPartial)
                }) { Text("Finalizar parcialmente") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirmation = false }) { Text("Continuar rutina") }
            },
        )
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("¿Deseas salir de la rutina?") },
            text = { Text("El intento se guardará como abandonado. Tu avance real no se perderá.") },
            confirmButton = {
                TextButton(
                    enabled = !actionInProgress,
                    onClick = {
                        if (actionInProgress) return@TextButton
                        actionInProgress = true
                        scope.launch {
                            runCatching { onAbandon() }
                                .onSuccess {
                                    showExitConfirmation = false
                                    onExit()
                                }
                                .onFailure {
                                    actionError = it.message ?: "No fue posible guardar la salida."
                                    showExitConfirmation = false
                                }
                            actionInProgress = false
                        }
                    },
                ) { Text("Salir de la rutina") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) { Text("Continuar rutina") }
            },
        )
    }
}

@Composable
private fun CompletionPanel(
    execution: RoutineExecution,
    onClose: () -> Unit,
    onViewProgress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StatusPill(execution.status.label)
        Text(
            if (execution.status == ExecutionStatus.COMPLETED) {
                "Rutina completada"
            } else {
                "Rutina realizada parcialmente"
            },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp).semantics { heading() },
        )
        Text(
            "${execution.completedSteps} completados · ${execution.skippedSteps} omitidos · " +
                "${execution.totalSteps} pasos en total",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Cerrar y volver") }
        RutinaGoOutlinedButton(
            onClick = onViewProgress,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Ver progreso") }
    }
}
