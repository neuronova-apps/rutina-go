package com.neuronovaapps.rutinago.ui.screens.communication.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.neuronovaapps.rutinago.ui.components.SectionHeading
import com.neuronovaapps.rutinago.ui.theme.LocalHighContrast
import com.neuronovaapps.rutinago.domain.communication.CommunicationCategory
import com.neuronovaapps.rutinago.domain.communication.CommunicationVocabularyItem
import com.neuronovaapps.rutinago.domain.communication.CommunicationMessageItem

@Composable
fun CommunicationMessageBar(
    items: List<CommunicationMessageItem>,
    canUndoClear: Boolean,
    onRemoveLast: () -> Unit,
    onClear: () -> Unit,
    onUndoClear: () -> Unit,
    modifier: Modifier = Modifier,
    speechControls: (@Composable () -> Unit)? = null,
) {
    val highContrast = LocalHighContrast.current
    val messageScroll = rememberScrollState()
    LaunchedEffect(items.lastOrNull()?.selectionId, messageScroll.maxValue) {
        messageScroll.scrollTo(messageScroll.maxValue)
    }
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeading("Mi mensaje")
            if (items.isEmpty()) {
                Text("Tu mensaje aparecerá aquí", style = MaterialTheme.typography.bodyLarge)
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)
                        .verticalScroll(messageScroll).testTag("communication-message-items")
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items.forEachIndexed { index, item ->
                        key(item.selectionId) {
                            Surface(
                                modifier = Modifier.testTag("communication-message-item-${item.selectionId}")
                                    .semantics(mergeDescendants = true) { stateDescription = "Palabra ${index + 1} de ${items.size}" },
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(if (highContrast) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Text(item.text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
            speechControls?.invoke()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MessageAction("Borrar último", "communication-remove-last", items.isNotEmpty(), onRemoveLast)
                MessageAction("Limpiar", "communication-clear", items.isNotEmpty(), onClear)
                if (canUndoClear) {
                    MessageAction("Deshacer limpieza", "communication-undo-clear", true, onUndoClear)
                }
            }
        }
    }
}

@Composable
private fun MessageAction(label: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.heightIn(min = 48.dp).testTag(tag),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) { Text(label, textAlign = TextAlign.Center) }
}

/** All source categories remain available through vertical scrolling, without a horizontal strip. */
@Composable
fun CommunicationCategorySelector(
    categories: List<CommunicationCategory>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().testTag("communication-categories"),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Categoría activa: ${categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Todos"}",
            Modifier.testTag("communication-active-category"))
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("communication-category-toggle")
                .semantics { stateDescription = if (expanded) "Categorías visibles" else "Categorías contraídas" },
            shape = MaterialTheme.shapes.medium,
        ) { Text(if (expanded) "Cerrar categorías" else "Elegir categoría", textAlign = TextAlign.Center) }
        if (expanded) {
            FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CommunicationChoiceOption("Todos", "communication-category-all", selectedCategoryId == null) {
                    onSelect(null)
                    expanded = false
                }
                categories.forEach { category ->
                    key(category.id) {
                        CommunicationChoiceOption(category.name, "communication-category-${category.id}",
                            selectedCategoryId == category.id) {
                            onSelect(category.id)
                            expanded = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CommunicationChoiceOption(
    label: String, tag: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit,
) {
    val highContrast = LocalHighContrast.current
    Surface(
        modifier = Modifier.widthIn(max = 260.dp).heightIn(min = 48.dp)
            .testTag(tag)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
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

/** Both quick access and category results select the same catalog record. */
@Composable
fun CommunicationVocabularyCard(
    item: CommunicationVocabularyItem,
    onSelect: (String) -> Unit,
    tag: String = "communication-word-${item.vocabId}",
    minimumHeight: Dp = 72.dp,
    contentPadding: Dp = 16.dp,
    emphasized: Boolean = false,
) {
    Surface(
        onClick = { onSelect(item.vocabId) },
        modifier = Modifier.fillMaxWidth().heightIn(min = minimumHeight)
            .testTag(tag).semantics { role = Role.Button; contentDescription = "Usar palabra: ${item.text}" },
        shape = MaterialTheme.shapes.medium,
        color = if (emphasized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (LocalHighContrast.current) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(item.text, Modifier.padding(contentPadding), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}
