package com.neuronovaapps.rutinago.ui.screens.modules

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.neuronovaapps.rutinago.ui.components.RutinaGoOutlinedButton
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.SectionHeading

@Composable
fun ModuleSelectorScreen(
    activeProfileName: String,
    onOpenRutinaGo: () -> Unit,
    onOpenCommunication: () -> Unit,
) {
    ScreenColumn(Modifier.safeDrawingPadding().testTag("module-selector")) {
        SectionHeading("Elige un módulo")
        Text("Perfil activo: $activeProfileName")
        Text("Selecciona el apoyo que deseas utilizar.")
        Button(
            onClick = onOpenRutinaGo,
            modifier = Modifier.fillMaxWidth().testTag("module-rutinago"),
        ) {
            Text("RutinaGo")
        }
        Text("Organiza tus rutinas y consulta tu progreso.")
        RutinaGoOutlinedButton(
            onClick = onOpenCommunication,
            modifier = Modifier.fillMaxWidth().testTag("module-communication"),
        ) {
            Text("Comunicación")
        }
        Text("Un espacio de comunicación en preparación.")
    }
}
