package com.neuronovaapps.rutinago.ui.screens.communication

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.domain.communication.CommunicationProfileSetup
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.SectionHeading

@Composable
internal fun CommunicationSettingsScreen(
    profileName: String,
    setup: CommunicationProfileSetup?,
    onPersonalize: (() -> Unit)?,
    presentationControls: (@Composable () -> Unit)?,
    onBackToModules: () -> Unit,
) {
    ScreenColumn(Modifier.safeDrawingPadding().testTag("communication-settings")) {
        SectionHeading("Configuración de ComunicaGo")
        Text("Perfil activo: $profileName")
        SectionHeading("Personalización del perfil")
        if (setup != null) {
            Text("Rango de edad: ${setup.ageRange.setupLabel}")
            Text("Comunicación oral: ${setup.oralCommunication.setupLabel}")
        }
        if (onPersonalize != null) OutlinedButton(onClick = onPersonalize,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("communication-personalize")) {
            Text("Editar personalización")
        }
        presentationControls?.invoke()
        SectionHeading("Accesibilidad compartida")
        Text("El tema Día, Noche o Sistema, el alto contraste y el tamaño del texto se comparten con RutinaGo. Puedes cambiarlos en la Configuración de RutinaGo.")
        OutlinedButton(onClick = onBackToModules,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("communication-back-to-modules")) {
            Text("Volver al selector de módulos")
        }
    }
}
