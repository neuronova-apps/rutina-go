package com.neuronovaapps.rutinago.ui.screens.communication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.neuronovaapps.rutinago.data.communication.AndroidCommunicationSpeechEngine
import com.neuronovaapps.rutinago.domain.communication.*
import com.neuronovaapps.rutinago.ui.components.SectionHeading
import com.neuronovaapps.rutinago.ui.screens.communication.components.CommunicationChoiceOption

/** Profile/session owner. Only the last confirmed text is transiently saved, never audio/history. */
@Composable
internal fun rememberCommunicationSpeechController(profileId: String,
    engineFactory: (() -> CommunicationSpeechEngine)? = null): CommunicationSpeechController {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var confirmed by rememberSaveable(profileId) { mutableStateOf<String?>(null) }
    val controller = remember(profileId) { CommunicationSpeechController(scope,
        engineFactory ?: { AndroidCommunicationSpeechEngine(context) }, confirmed) { confirmed = it } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(controller, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> when (event) {
            Lifecycle.Event.ON_START -> controller.initialize()
            Lifecycle.Event.ON_STOP -> controller.stop()
            else -> Unit
        } }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) controller.initialize()
        onDispose { lifecycle.removeObserver(observer); controller.close() }
    }
    return controller
}

@Composable
internal fun CommunicationSpeechControls(controller: CommunicationSpeechController,
    items: List<CommunicationMessageItem>, rate: CommunicationSpeechRate) {
    val state by controller.state.collectAsState()
    Column(Modifier.fillMaxWidth().testTag("communication-speech-controls"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { controller.speak(items, rate) }, enabled = state.ready && items.isNotEmpty(),
                modifier = Modifier.heightIn(min = 48.dp).testTag("communication-speak")) {
                Icon(speakerIcon, contentDescription = null, Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Hablar")
            }
            OutlinedButton(onClick = controller::stop, enabled = state.active,
                modifier = Modifier.heightIn(min = 48.dp).testTag("communication-speech-stop")) { Text("Detener") }
            if (state.lastConfirmed != null) OutlinedButton(onClick = { controller.repeat(rate) }, enabled = state.ready,
                modifier = Modifier.heightIn(min = 48.dp).testTag("communication-speech-repeat")) { Text("Repetir") }
        }
        SpeechAvailability(state)
    }
}

@Composable
internal fun CommunicationSpeechSettings(controller: CommunicationSpeechController,
    rate: CommunicationSpeechRate, saving: Boolean, error: String?, onRate: (CommunicationSpeechRate) -> Unit) {
    val state by controller.state.collectAsState()
    SectionHeading("Velocidad de voz")
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CommunicationSpeechRate.entries.forEach {
            CommunicationChoiceOption(it.label, "communication-voice-rate-${it.name}", rate == it, !saving) { onRate(it) }
        }
    }
    if (saving) Text("Guardando velocidad…", Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    if (error != null) Text(error, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    SpeechAvailability(state)
    if (state.status in listOf(SpeechStatus.ERROR, SpeechStatus.UNAVAILABLE)) OutlinedButton(onClick = controller::retry,
        modifier = Modifier.heightIn(min = 48.dp).testTag("communication-speech-retry")) { Text("Reintentar voz") }
    Text("La voz depende del motor instalado en Android. Las voces locales pueden funcionar sin internet; otras necesitan conexión. El motor instalado procesa el texto según su propia configuración. Revisa su idioma, volumen y salida de audio en la Configuración de Android.")
}

@Composable
private fun SpeechAvailability(state: CommunicationSpeechState) {
    val label = state.problem?.explanation ?: when {
        state.status == SpeechStatus.INITIALIZING -> "Preparando voz…"
        state.active && state.status != SpeechStatus.SPEAKING -> "Solicitud de voz enviada…"
        state.status == SpeechStatus.SPEAKING -> "Reproduciendo…"
        state.status == SpeechStatus.STOPPED -> "Voz detenida"
        state.ready -> "Voz disponible"
        else -> "Voz no disponible"
    }
    Text(label, modifier = Modifier.testTag("communication-speech-status").semantics { liveRegion = LiveRegionMode.Polite })
    if (state.voice?.networkRequired == true) Text("La voz española disponible requiere conexión a internet.")
}

private val speakerIcon = ImageVector.Builder("Altavoz", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 9f); lineTo(7f, 9f); lineTo(12f, 4f); lineTo(12f, 20f)
        lineTo(7f, 15f); lineTo(3f, 15f); close()
        moveTo(15f, 7f); lineTo(17f, 7f); lineTo(20f, 12f); lineTo(17f, 17f)
        lineTo(15f, 17f); lineTo(18f, 12f); close()
    }
}.build()
