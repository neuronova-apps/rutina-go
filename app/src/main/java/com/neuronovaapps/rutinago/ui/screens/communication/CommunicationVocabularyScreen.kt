package com.neuronovaapps.rutinago.ui.screens.communication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.communication.LocalCommunicationCatalogRepository
import com.neuronovaapps.rutinago.domain.communication.*
import com.neuronovaapps.rutinago.ui.components.RutinaGoOutlinedButton
import com.neuronovaapps.rutinago.ui.components.SectionHeading
import com.neuronovaapps.rutinago.ui.screens.communication.components.CommunicationCategorySelector
import com.neuronovaapps.rutinago.ui.screens.communication.components.CommunicationChoiceOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CommunicationVocabularyScreen(
    activeProfileName: String,
    visibleIds: Set<String>?,
    saving: Boolean,
    error: String?,
    onSave: (Set<String>) -> Unit,
    onBack: () -> Unit,
) {
    val assets = LocalContext.current.applicationContext.assets
    var retry by remember { mutableIntStateOf(0) }
    val loaded by produceState<Result<CommunicationCatalogRepository>?>(null, assets, retry) {
        value = withContext(Dispatchers.IO) { runCatching {
            LocalCommunicationCatalogRepository {
                assets.open(LocalCommunicationCatalogRepository.ASSET_PATH).bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } }
    }
    BackHandler { if (!saving) onBack() }
    val catalog = loaded?.getOrNull()
    if (catalog != null) {
        CommunicationVocabularyContent(activeProfileName, catalog, visibleIds, saving, error, onSave, onBack)
    } else {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RutinaGoOutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Volver") }
                Text(if (loaded == null) "Cargando vocabulario…" else "No se pudo cargar el vocabulario local.")
                if (loaded?.isFailure == true) Button(onClick = { retry++ }) { Text("Reintentar") }
            }
        }
    }
}

@Composable
internal fun CommunicationVocabularyContent(
    activeProfileName: String,
    catalog: CommunicationCatalogRepository,
    visibleIds: Set<String>?,
    saving: Boolean,
    error: String?,
    onSave: (Set<String>) -> Unit,
    onBack: () -> Unit,
) {
    val firstSetup = visibleIds == null
    var draft by rememberSaveable { mutableStateOf<List<String>?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var filterName by rememberSaveable { mutableStateOf(VocabularyFilter.ALL.name) }
    var hideId by rememberSaveable { mutableStateOf<String?>(null) }
    var resetting by rememberSaveable { mutableStateOf(false) }
    var resetSuggested by rememberSaveable { mutableStateOf(false) }
    val allIds = remember(catalog) { catalog.vocabulary.map { it.vocabId }.toSet() }
    val selected = (visibleIds ?: draft?.toSet() ?: emptySet()).intersect(allIds)
    val canEdit = !saving && (!firstSetup || draft != null)
    val words = CommunicationPersonalVocabulary.search(catalog, selected, VocabularyFilter.valueOf(filterName), category, query)
    val categories = remember(catalog) { catalog.categories.associateBy { it.id } }
    fun change(ids: Set<String>) {
        if (firstSetup) draft = ids.toList() else onSave(ids)
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize().safeDrawingPadding().imePadding().testTag("personal-vocabulary"),
                contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item(key = "back") {
                    RutinaGoOutlinedButton(onClick = onBack, enabled = !saving,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("vocabulary-back")) {
                        Text(if (firstSetup) "Volver al selector" else "Volver a ComunicaGo")
                    }
                }
                item(key = "title") { SectionHeading("Mi vocabulario") }
                item(key = "profile") { Text("Administración para familiares y apoyos · Perfil: $activeProfileName") }
                item(key = "explanation") {
                    Text("Ocultar solo retira palabras de la vista habitual. Puedes recuperarlas aquí; no se borran del banco ni de los mensajes.")
                }
                if (firstSetup) {
                    item(key = "initial") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionHeading("Primera configuración")
                            Text("Ambas opciones son válidas y pueden modificarse después. La selección sugerida es un punto de partida funcional: no es una evaluación ni cubre necesariamente todas las necesidades. Revisa y amplía las palabras antes de confirmar.")
                            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CommunicationChoiceOption("Utilizar las ${allIds.size} palabras", "vocabulary-start-all",
                                    draft?.toSet() == allIds, !saving) { draft = allIds.toList() }
                                CommunicationChoiceOption("Empezar con una selección sugerida", "vocabulary-start-suggested",
                                    draft?.toSet() == CommunicationPersonalVocabulary.suggestedIds(catalog), !saving) {
                                    draft = CommunicationPersonalVocabulary.suggestedIds(catalog).toList()
                                }
                            }
                            if (draft != null) Text("Selección en revisión. Los cambios se guardarán al confirmar.")
                            Button(onClick = { onSave(selected) }, enabled = canEdit,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("vocabulary-confirm-initial")) {
                                Text("Confirmar vocabulario")
                            }
                        }
                    }
                }
                item(key = "counts") {
                    Text(if (firstSetup && draft == null) "Banco completo: ${allIds.size} palabras. Elige una opción para revisar."
                        else "${selected.size} palabras visibles · ${allIds.size - selected.size} palabras ocultas",
                        Modifier.testTag("vocabulary-counts").semantics { liveRegion = LiveRegionMode.Polite })
                }
                item(key = "status") {
                    Column {
                        Text(if (saving) "Guardando vocabulario…" else if (!firstSetup) "Los cambios se guardan automáticamente." else "Puedes agregar u ocultar palabras en esta revisión.",
                            Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        if (error != null) Text(error, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.error)
                    }
                }
                if (!firstSetup) item(key = "reset") {
                    RutinaGoOutlinedButton(onClick = { resetting = true }, enabled = !saving,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("vocabulary-reset")) {
                        Text("Restablecer vocabulario")
                    }
                }
                item(key = "search") {
                    OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Buscar palabra o categoría") },
                        modifier = Modifier.fillMaxWidth().testTag("vocabulary-search"))
                }
                item(key = "filters") {
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VocabularyFilter.entries.forEach { filter ->
                            CommunicationChoiceOption(filter.label, "vocabulary-filter-${filter.name}", filterName == filter.name, true) {
                                filterName = filter.name
                            }
                        }
                    }
                }
                item(key = "categories") {
                    CommunicationCategorySelector(catalog.categories, category) { category = it }
                }
                if (words.isEmpty()) item(key = "empty") { Text("No hay palabras con estos filtros. Prueba con Todas las palabras, Todos o una búsqueda diferente.") }
                items(words, key = { it.vocabId }) { word ->
                    val visible = word.vocabId in selected
                    Surface(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(12.dp).testTag("vocabulary-row-${word.vocabId}"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(word.text, style = MaterialTheme.typography.titleMedium)
                            Text(categories[word.categoryId]?.name.orEmpty())
                            Text(if (visible) "Visible" else "Oculta", Modifier.semantics { stateDescription = if (visible) "Visible" else "Oculta" })
                            RutinaGoOutlinedButton(onClick = {
                                if (visible && CommunicationPersonalVocabulary.needsHideWarning(word.vocabId)) hideId = word.vocabId
                                else change(if (visible) selected - word.vocabId else selected + word.vocabId)
                            }, enabled = canEdit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("vocabulary-toggle-${word.vocabId}")) {
                                Text(if (visible) "Ocultar ${word.text}" else "${if (VocabularyFilter.valueOf(filterName) == VocabularyFilter.HIDDEN) "Recuperar" else "Agregar"} ${word.text}")
                            }
                        }
                    }
                }
            }
        }
    }
    val pendingWord = catalog.vocabulary.firstOrNull { it.vocabId == hideId }
    if (pendingWord != null) AlertDialog(
        onDismissRequest = { hideId = null }, title = { Text("¿Ocultar ${pendingWord.text}?") },
        text = { Text("Esta palabra puede ser importante para expresar necesidades, molestias o desacuerdo. Puedes recuperarla desde Palabras ocultas.",
            Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { change(selected - pendingWord.vocabId); hideId = null }, enabled = canEdit,
            modifier = Modifier.heightIn(min = 48.dp).testTag("vocabulary-confirm-hide")) { Text("Ocultar") } },
        dismissButton = { TextButton(onClick = { hideId = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancelar") } },
    )
    if (resetting) AlertDialog(
        onDismissRequest = { resetting = false }, title = { Text("Restablecer vocabulario") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Se reemplazará la selección de este perfil. No cambia perfiles, rutinas, mensajes ni historial.")
                Text("Algunas palabras pueden ser importantes para expresar necesidades, molestias o desacuerdo. Revisa la selección y recupera las que necesites.")
                CommunicationChoiceOption("Mostrar las ${allIds.size} palabras", "vocabulary-reset-all", !resetSuggested, !saving) { resetSuggested = false }
                CommunicationChoiceOption("Volver a la selección inicial sugerida", "vocabulary-reset-suggested", resetSuggested, !saving) { resetSuggested = true }
                Text("Quedarán visibles ${if (resetSuggested) CommunicationPersonalVocabulary.suggestedIds(catalog).size else allIds.size} palabras. Las demás se podrán recuperar en Palabras ocultas.")
            }
        },
        confirmButton = { TextButton(onClick = {
            onSave(if (resetSuggested) CommunicationPersonalVocabulary.suggestedIds(catalog) else allIds)
            resetting = false
        }, enabled = !saving, modifier = Modifier.heightIn(min = 48.dp).testTag("vocabulary-confirm-reset")) { Text("Restablecer") } },
        dismissButton = { TextButton(onClick = { resetting = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancelar") } },
    )
}
