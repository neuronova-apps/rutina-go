package com.neuronovaapps.rutinago.ui.screens.communication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
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
    Surface(color = MaterialTheme.colorScheme.background) {
        ScreenColumn(Modifier.safeDrawingPadding().testTag("communication-placeholder")) {
            RutinaGoOutlinedButton(
                onClick = onBackToModules,
                modifier = Modifier.fillMaxWidth().testTag("communication-back-to-modules"),
            ) {
                Text("Volver al selector", textAlign = TextAlign.Center)
            }
            SectionHeading("ComunicaGo")
            Text("Perfil activo: $activeProfileName")
            EmptyState(
                title = "Próximamente",
                body = "Este espacio de comunicación está en preparación.",
            )
        }
    }
}
