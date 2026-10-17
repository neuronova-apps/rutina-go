package com.neuronovaapps.rutinago.ui.screens.communication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.SectionHeading
import com.neuronovaapps.rutinago.ui.screens.communication.components.CommunicationMessageBar
import com.neuronovaapps.rutinago.ui.screens.communication.components.CommunicationVocabularyArea
import com.neuronovaapps.rutinago.ui.theme.LocalHighContrast

@Composable
fun CommunicationHomeScreen(
    activeProfileName: String,
    onBackToModules: () -> Unit,
) {
    val highContrast = LocalHighContrast.current
    BackHandler(onBack = onBackToModules)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.TopCenter) {
            ScreenColumn(
                Modifier.widthIn(max = 720.dp).safeDrawingPadding().testTag("communication-home"),
            ) {
                OutlinedButton(
                    onClick = onBackToModules,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("communication-back-to-modules"),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (highContrast) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    ),
                    border = BorderStroke(
                        if (highContrast) 2.dp else 1.dp,
                        MaterialTheme.colorScheme.outline,
                    ),
                ) {
                    Text("Volver al selector", textAlign = TextAlign.Center)
                }
                SectionHeading("ComunicaGo")
                Text("Perfil activo: $activeProfileName")
                CommunicationMessageBar()
                CommunicationVocabularyArea()
            }
        }
    }
}
