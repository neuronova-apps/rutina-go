package com.neuronovaapps.rutinago.ui.screens.communication.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.ui.components.SectionHeading
import com.neuronovaapps.rutinago.ui.theme.LocalHighContrast
import com.neuronovaapps.rutinago.domain.communication.CommunicationCategory
import com.neuronovaapps.rutinago.domain.communication.CommunicationVocabularyItem

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

/** Horizontal scrolling also supports large text in a small viewport. */
@Composable
fun CommunicationCategorySelector(
    categories: List<CommunicationCategory>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().selectableGroup().testTag("communication-categories"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "all") { CategoryOption(null, "Todos", selectedCategoryId == null, onSelect) }
        items(categories, key = { it.id }) { CategoryOption(it.id, it.name, selectedCategoryId == it.id, onSelect) }
    }
}

@Composable
private fun CategoryOption(id: String?, label: String, selected: Boolean, onSelect: (String?) -> Unit) {
    val highContrast = LocalHighContrast.current
    Surface(
        modifier = Modifier.widthIn(max = 260.dp).heightIn(min = 48.dp)
            .testTag("communication-category-${id ?: "all"}")
            .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(id) })
            .semantics { stateDescription = if (selected) "Seleccionada" else "No seleccionada" },
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(if (selected) 3.dp else if (highContrast) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (selected) Text("✓", Modifier.clearAndSetSemantics { })
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

/** Read-only text card; message and speech interaction belong to a later phase. */
@Composable
fun CommunicationVocabularyCard(item: CommunicationVocabularyItem) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .testTag("communication-word-${item.vocabId}").semantics(mergeDescendants = true) { },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (LocalHighContrast.current) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(item.text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}
