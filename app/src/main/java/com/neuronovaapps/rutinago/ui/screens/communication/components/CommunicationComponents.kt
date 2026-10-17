package com.neuronovaapps.rutinago.ui.screens.communication.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.ui.components.SectionHeading
import com.neuronovaapps.rutinago.ui.theme.LocalHighContrast

/** Presentation surface only; message composition will be introduced with its own module state. */
@Composable
fun CommunicationMessageBar(modifier: Modifier = Modifier) {
    val highContrast = LocalHighContrast.current
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 144.dp)
            .testTag("communication-message-area")
            .semantics { isTraversalGroup = true },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        border = BorderStroke(if (highContrast) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeading("Mensaje")
            Text("Tu mensaje aparecerá aquí", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Empty vocabulary surface: no demo words, categories, or routine data. */
@Composable
fun CommunicationVocabularyArea(modifier: Modifier = Modifier) {
    val highContrast = LocalHighContrast.current
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 208.dp)
            .testTag("communication-vocabulary-area")
            .semantics { isTraversalGroup = true },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (highContrast) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeading("Palabras y apoyos visuales")
            Text(
                "Las palabras y apoyos visuales aparecerán aquí.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "En la siguiente etapa podrás seleccionar palabras para construir tu mensaje.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
