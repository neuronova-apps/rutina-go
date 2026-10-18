package com.neuronovaapps.rutinago.ui.screens.communication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.domain.communication.CommunicationAgeRange
import com.neuronovaapps.rutinago.domain.communication.CommunicationProfileSetup
import com.neuronovaapps.rutinago.domain.communication.OralCommunication
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.SectionHeading
import com.neuronovaapps.rutinago.ui.theme.LocalHighContrast

internal val CommunicationAgeRange.setupLabel: String get() = when (this) {
    CommunicationAgeRange.YEARS_3_TO_5 -> "3 a 5 años"
    CommunicationAgeRange.YEARS_6_TO_8 -> "6 a 8 años"
    CommunicationAgeRange.YEARS_9_TO_12 -> "9 a 12 años"
    CommunicationAgeRange.YEARS_13_TO_17 -> "13 a 17 años"
    CommunicationAgeRange.ADULT -> "18 años o más"
    CommunicationAgeRange.NOT_SPECIFIED -> "Prefiero no especificar"
}

internal val OralCommunication.setupLabel: String get() = when (this) {
    OralCommunication.WORDS_AND_PHRASES -> "Habla utilizando palabras y frases."
    OralCommunication.EMERGING_SPEECH -> "Utiliza algunas palabras o vocalizaciones."
    OralCommunication.NO_CURRENT_SPEECH -> "No utiliza habla oral actualmente."
    OralCommunication.VARIABLE_SPEECH -> "Su comunicación oral es variable."
    OralCommunication.NOT_SPECIFIED -> "Prefiero no especificar."
}

@Composable
fun CommunicationProfileSetupScreen(
    profileId: String,
    activeProfileName: String,
    initialSetup: CommunicationProfileSetup?,
    editing: Boolean,
    saving: Boolean,
    error: String?,
    onSave: (CommunicationAgeRange, OralCommunication) -> Unit,
    onBack: () -> Unit,
) {
    var ageName by rememberSaveable(profileId) { mutableStateOf(initialSetup?.ageRange?.name) }
    var oralName by rememberSaveable(profileId) { mutableStateOf(initialSetup?.oralCommunication?.name) }
    BackHandler { if (!saving) onBack() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.TopCenter) {
            ScreenColumn(Modifier.widthIn(max = 720.dp).safeDrawingPadding().testTag("communication-profile-setup")) {
                OutlinedButton(
                    onClick = onBack, enabled = !saving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("communication-setup-back"),
                    shape = MaterialTheme.shapes.medium,
                ) { Text(if (editing) "Volver a ComunicaGo" else "Volver al selector", textAlign = TextAlign.Center) }
                SectionHeading("Personalizar ComunicaGo")
                Text("Perfil activo: $activeProfileName")
                Text("Estas opciones nos ayudarán a adaptar los apoyos de comunicación. Podrás modificarlas cuando lo necesites.")
                Text("Estas respuestas no limitarán las palabras ni las funciones disponibles.")
                SectionHeading("¿Cuál es su rango de edad?")
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CommunicationAgeRange.entries.forEach { age ->
                        SetupOption(age.setupLabel, "communication-age-${age.name}", ageName == age.name, !saving) {
                            ageName = age.name
                        }
                    }
                }
                SectionHeading("¿Cómo se comunica oralmente actualmente?")
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OralCommunication.entries.forEach { oral ->
                        SetupOption(oral.setupLabel, "communication-oral-${oral.name}", oralName == oral.name, !saving) {
                            oralName = oral.name
                        }
                    }
                }
                if (error != null) {
                    Text(error, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = { onSave(CommunicationAgeRange.valueOf(requireNotNull(ageName)),
                        OralCommunication.valueOf(requireNotNull(oralName))) },
                    enabled = ageName != null && oralName != null && !saving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("communication-setup-save"),
                    shape = MaterialTheme.shapes.medium,
                ) { Text(if (saving) "Guardando…" else if (editing) "Guardar cambios" else "Guardar y continuar",
                    textAlign = TextAlign.Center) }
            }
        }
    }
}

@Composable
private fun SetupOption(label: String, tag: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag)
            .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { stateDescription = if (selected) "Seleccionada" else "No seleccionada" },
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected || LocalHighContrast.current) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RadioButton(selected, onClick = null, enabled = enabled, modifier = Modifier.clearAndSetSemantics { })
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
