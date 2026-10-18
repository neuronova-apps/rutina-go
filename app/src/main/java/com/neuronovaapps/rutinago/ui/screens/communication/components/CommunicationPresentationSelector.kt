package com.neuronovaapps.rutinago.ui.screens.communication.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.domain.communication.CommunicationPresentationMode
import com.neuronovaapps.rutinago.domain.communication.CommunicationSuggestionReason
import com.neuronovaapps.rutinago.ui.components.SectionHeading

internal data class CommunicationPresentationLayout(
    val minimumCardWidth: Int,
    val wordHeight: Int,
    val quickAccessHeight: Int,
    val cardPadding: Int,
    val spacing: Int,
)

private val guidedLayout = CommunicationPresentationLayout(184, 96, 112, 20, 16)
private val fullLayout = CommunicationPresentationLayout(132, 64, 64, 12, 10)
internal fun presentationLayout(mode: CommunicationPresentationMode): CommunicationPresentationLayout =
    if (mode == CommunicationPresentationMode.GUIDED) guidedLayout else fullLayout

@Composable
fun CommunicationPresentationSelector(
    selected: CommunicationPresentationMode,
    saving: Boolean,
    error: String?,
    showIntroduction: Boolean,
    suggestionReason: CommunicationSuggestionReason,
    onSelect: (CommunicationPresentationMode) -> Unit,
    onIntroductionShown: () -> Unit,
    onDismissIntroduction: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().testTag("communication-presentation"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeading("Presentación")
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CommunicationPresentationMode.entries.forEach { mode ->
                CommunicationChoiceOption(
                    label = if (mode == CommunicationPresentationMode.GUIDED) "Vista guiada" else "Vista completa",
                    tag = "communication-presentation-${mode.name}",
                    selected = selected == mode,
                    enabled = !saving,
                    onClick = { onSelect(mode) },
                )
            }
        }
        if (saving) Text("Guardando presentación…")
        if (error != null) Text(error, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = MaterialTheme.colorScheme.error)
        if (showIntroduction) {
            LaunchedEffect(Unit) { onIntroductionShown() }
            Text(
                if (suggestionReason == CommunicationSuggestionReason.QUICK_ACCESS_START)
                    "La Vista guiada destaca palabras de acceso rápido. Ambas vistas comparten tu vocabulario. Puedes agregar o recuperar palabras en Mi vocabulario."
                else "Puedes cambiar de vista cuando quieras. Ambas comparten tu vocabulario y todas las categorías. Revisa las palabras en Mi vocabulario.",
                Modifier.testTag("communication-presentation-introduction"),
            )
            TextButton(onClick = onDismissIntroduction, modifier = Modifier.heightIn(min = 48.dp)) { Text("Entendido") }
        }
    }
}
