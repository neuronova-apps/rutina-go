package com.neuronovaapps.rutinago.ui.screens.communication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.neuronovaapps.rutinago.ui.components.EmptyState
import com.neuronovaapps.rutinago.ui.components.RutinaGoOutlinedButton
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.SectionHeading

@Composable
fun CommunicationPlaceholderScreen(
    activeProfileName: String,
    onBackToModules: () -> Unit,
) {
    BackHandler(onBack = onBackToModules)
    ScreenColumn(Modifier.safeDrawingPadding().testTag("communication-placeholder")) {
        SectionHeading("Comunicación")
        Text("Perfil activo: $activeProfileName")
        EmptyState(
            title = "Próximamente",
            body = "Este espacio de comunicación está en preparación.",
        )
        RutinaGoOutlinedButton(
            onClick = onBackToModules,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Volver al selector")
        }
    }
}
