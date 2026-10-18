package com.neuronovaapps.rutinago.ui.screens.communication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.domain.communication.*
import com.neuronovaapps.rutinago.ui.components.RutinaGoOutlinedButton
import com.neuronovaapps.rutinago.ui.components.SectionHeading
import com.neuronovaapps.rutinago.ui.screens.communication.components.CommunicationChoiceOption

/** Administration has no message callback. Its controls can only change Home membership. */
@Composable
internal fun CommunicationHomeVocabularyScreen(
    profileName: String,
    catalog: CommunicationCatalogRepository,
    availableIds: Set<String>?,
    homeIds: Set<String>?,
    saving: Boolean,
    error: String?,
    onSetHomeWord: (String, Boolean) -> Unit,
    returnToHome: Boolean,
    onBack: () -> Unit,
    onVocabulary: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    // New visits expose available words immediately, including profiles with six legacy defaults.
    var assignedOnly by rememberSaveable { mutableStateOf(false) }
    val assigned = CommunicationHomeVocabulary.assignedIds(homeIds, availableIds)
        .intersect(catalog.vocabulary.map { it.vocabId }.toSet())
    LaunchedEffect(assigned.isEmpty()) { if (assigned.isEmpty()) assignedOnly = false }
    val homeWords = CommunicationHomeVocabulary.words(catalog, homeIds, availableIds)
    val available = availableIds ?: catalog.vocabulary.map { it.vocabId }.toSet()
    val results = CommunicationPersonalVocabulary.search(catalog, available, VocabularyFilter.ALL, null, query)
        .filter { if (assignedOnly) it.vocabId in assigned else it.vocabId in available }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        CommunicationHomeSaveStatus(saving, error)
        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize().safeDrawingPadding().imePadding()
                .testTag("home-vocabulary-organization"), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item(key = "back") {
                    RutinaGoOutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("home-organization-back")) { Text(if (returnToHome) "Volver a Inicio" else "Volver a Mi vocabulario") }
                }
                item(key = "title") { SectionHeading("Palabras de Inicio") }
                item(key = "profile") { Text("Perfil activo: $profileName") }
                item(key = "search") {
                    OutlinedTextField(query, { query = it }, label = { Text("Buscar palabra o categoría") },
                        modifier = Modifier.fillMaxWidth().testTag("home-organization-search"))
                }
                item(key = "filter") {
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CommunicationChoiceOption("Asignadas a Inicio", "home-organization-assigned", assignedOnly) { assignedOnly = true }
                        CommunicationChoiceOption("Todas las disponibles", "home-organization-available", !assignedOnly) { assignedOnly = false }
                    }
                }
                item(key = "counts") {
                    Text("${homeWords.size} palabras en Inicio · ${assigned.size - homeWords.size} asignadas temporalmente ocultas",
                        Modifier.testTag("home-organization-count").semantics { liveRegion = LiveRegionMode.Polite })
                }
                item(key = "explanation") {
                    Text("Elige las palabras que quieres tener a mano. Quitar de Inicio no oculta la palabra en Buscar más palabras ni cambia el mensaje. Puedes decidir con el apoyo de una persona de confianza.")
                }
                if (results.isEmpty()) item(key = "empty") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (assignedOnly && query.isBlank()) "Todavía no hay palabras asignadas a Inicio." else "No hay palabras con esta búsqueda.")
                        if (available.isEmpty()) {
                            RutinaGoOutlinedButton(onClick = onVocabulary, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text("Recuperar palabras en Mi vocabulario")
                            }
                        } else {
                            RutinaGoOutlinedButton(onClick = { assignedOnly = false; query = "" },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home-organization-add-words")) {
                                Text("Agregar palabras a Inicio")
                            }
                        }
                    }
                }
                items(results, key = { it.vocabId }) { word ->
                    val inHome = word.vocabId in assigned
                    val visible = word.vocabId in available
                    Surface(shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(word.text, style = MaterialTheme.typography.titleMedium)
                            if (!visible) Text("Asignada a Inicio, temporalmente oculta. Reaparecerá al recuperarla en Mi vocabulario.")
                            CommunicationHomeAssignmentControl(word, inHome, !saving, onSetHomeWord, allowRemoval = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CommunicationHomeAssignmentControl(
    word: CommunicationVocabularyItem,
    assigned: Boolean,
    enabled: Boolean,
    onSetHomeWord: (String, Boolean) -> Unit,
    allowRemoval: Boolean = false,
) {
    val label = if (assigned && allowRemoval) "Quitar de Inicio" else if (assigned) "En Inicio" else "Añadir a Inicio"
    if (assigned && !allowRemoval) {
        Text("En Inicio", Modifier.testTag("home-assigned-${word.vocabId}")
            .semantics { contentDescription = "${word.text}: En Inicio" })
    } else {
        RutinaGoOutlinedButton(onClick = { onSetHomeWord(word.vocabId, !assigned) }, enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home-assignment-${word.vocabId}")
                .semantics { contentDescription = "$label: ${word.text}" }) {
            Text(label, Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun CommunicationHomeSaveStatus(saving: Boolean, error: String?) {
    if (saving) Text("Guardando palabras de Inicio…", Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    if (error != null) Text(error, Modifier.testTag("home-save-error").semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.error)
}
