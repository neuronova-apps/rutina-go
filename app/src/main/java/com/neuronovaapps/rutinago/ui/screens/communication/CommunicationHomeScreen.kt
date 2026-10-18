package com.neuronovaapps.rutinago.ui.screens.communication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.communication.CommunicationStateCodec
import com.neuronovaapps.rutinago.data.communication.LocalCommunicationCatalogRepository
import com.neuronovaapps.rutinago.domain.communication.*
import com.neuronovaapps.rutinago.ui.components.EmptyState
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.SectionHeading
import com.neuronovaapps.rutinago.ui.screens.communication.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One catalog and transient builder shared by three tabs and their secondary screens. */
@Composable
fun CommunicationHomeScreen(
    activeProfileName: String,
    onPersonalize: (() -> Unit)? = null,
    presentationMode: CommunicationPresentationMode = CommunicationPresentationMode.GUIDED,
    presentationControls: (@Composable () -> Unit)? = null,
    visibleVocabularyIds: Set<String>? = null,
    homeVocabularyIds: Set<String>? = null,
    homeSaving: Boolean = false,
    homeError: String? = null,
    onSetHomeWord: ((String, Boolean) -> Unit)? = null,
    onVocabulary: (() -> Unit)? = null,
    profileSetup: CommunicationProfileSetup? = null,
    navigation: CommunicationNavigationState? = null,
    onNavigate: ((CommunicationNavigationState) -> Unit)? = null,
    vocabularyContent: (@Composable (CommunicationCatalogRepository) -> Unit)? = null,
    speechContent: (@Composable (List<CommunicationMessageItem>) -> Unit)? = null,
    speechSettings: (@Composable () -> Unit)? = null,
    onBackToModules: () -> Unit,
) {
    val assets = LocalContext.current.applicationContext.assets
    val catalog by produceState<Result<CommunicationCatalogRepository>?>(null, assets) {
        value = withContext(Dispatchers.IO) { runCatching {
            LocalCommunicationCatalogRepository {
                assets.open(LocalCommunicationCatalogRepository.ASSET_PATH)
                    .bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } }
    }
    val repository = catalog?.getOrNull()
    if (repository == null) {
        BackHandler(onBack = onBackToModules)
        ScreenColumn(Modifier.safeDrawingPadding().testTag("communication-loading")) {
            OutlinedButton(onClick = onBackToModules) { Text("Volver al selector") }
            SectionHeading("ComunicaGo")
            Text("Perfil activo: $activeProfileName")
            Text(if (catalog == null) "Cargando catálogo…" else "No se pudo cargar el catálogo local. Vuelve al selector e inténtalo de nuevo.")
        }
        return
    }
    val builder = remember(repository) { CommunicationMessageBuilder(repository) }
    val saver = remember(builder) {
        Saver<CommunicationUiState, String>(
            save = { CommunicationStateCodec.encode(it) },
            restore = { runCatching { CommunicationStateCodec.decode(it, builder) }.getOrNull() },
        )
    }
    var state by rememberSaveable(stateSaver = saver) { mutableStateOf(builder.emptyState()) }
    // Standalone previews/tests own navigation here; the profile entry owns it in the app.
    var localNavigation by rememberSaveable(stateSaver = CommunicationNavigationSaver) { mutableStateOf(CommunicationNavigationState()) }
    val currentNavigation = navigation ?: localNavigation
    val destination = currentNavigation.destination
    val focus = LocalFocusManager.current
    fun navigate(next: CommunicationNavigationState) {
        if (onNavigate != null) onNavigate(next) else localNavigation = next
    }
    fun select(target: CommunicationDestination) {
        focus.clearFocus()
        navigate(currentNavigation.select(target))
    }
    val tabs = rememberSaveableStateHolder()
    fun exit() {
        // A new module visit opens Home; its message and saved tab contents remain in the session.
        select(CommunicationDestination.HOME)
        onBackToModules()
    }
    fun back() { currentNavigation.back()?.let(::navigate) ?: exit() }
    fun organizeHome() { focus.clearFocus(); navigate(currentNavigation.organizeHome()) }
    fun explore() { focus.clearFocus(); navigate(currentNavigation.explore()) }
    BackHandler { back() }
    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentNavigation.homeOrganizationOrigin == null) CommunicationBottomBar(destination, ::select)
        },
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            color = MaterialTheme.colorScheme.background) {
            tabs.SaveableStateProvider(when {
                currentNavigation.homeOrganizationOrigin != null -> "home-organization"
                currentNavigation.exploring -> "explorer"
                else -> destination.name
            }) {
                if (currentNavigation.homeOrganizationOrigin != null && onSetHomeWord != null) {
                    CommunicationHomeVocabularyScreen(activeProfileName, repository, visibleVocabularyIds, homeVocabularyIds,
                        homeSaving, homeError, onSetHomeWord,
                        returnToHome = currentNavigation.homeOrganizationOrigin == CommunicationDestination.HOME, onBack = ::back,
                        onVocabulary = { select(CommunicationDestination.MY_VOCABULARY) })
                } else {
                    when (destination) {
                        CommunicationDestination.HOME -> {
                            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                                if (currentNavigation.exploring) {
                                    CompactCommunicationMessage(state.messageItems) {
                                        select(CommunicationDestination.HOME)
                                    }
                                }
                                CommunicationHomeSaveStatus(homeSaving, homeError)
                                CommunicationWordGrid(activeProfileName, repository, builder, state,
                                    onState = { state = it }, presentationMode, visibleVocabularyIds,
                                    homeVocabularyIds,
                                    categories = currentNavigation.exploring,
                                    onVocabulary = { select(CommunicationDestination.MY_VOCABULARY) },
                                    onOrganizeHome = if (onSetHomeWord != null) ::organizeHome else null,
                                    onExplore = ::explore, speechContent = speechContent)
                            }
                        }
                        CommunicationDestination.MY_VOCABULARY -> {
                            if (vocabularyContent != null) vocabularyContent(repository)
                            else ScreenColumn(Modifier.safeDrawingPadding()) {
                                SectionHeading("Mi vocabulario")
                                Text("Perfil activo: $activeProfileName")
                                Text("Administración de las palabras visibles del perfil.")
                                if (onVocabulary != null) Button(onClick = onVocabulary) { Text("Administrar vocabulario") }
                            }
                        }
                        CommunicationDestination.SETTINGS -> CommunicationSettingsScreen(
                            activeProfileName, profileSetup, onPersonalize, presentationControls, ::exit, speechSettings)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunicationWordGrid(
    activeProfileName: String,
    repository: CommunicationCatalogRepository,
    builder: CommunicationMessageBuilder,
    state: CommunicationUiState,
    onState: (CommunicationUiState) -> Unit,
    presentationMode: CommunicationPresentationMode,
    visibleIds: Set<String>?,
    homeIds: Set<String>?,
    categories: Boolean,
    onVocabulary: () -> Unit,
    onOrganizeHome: (() -> Unit)?,
    onExplore: () -> Unit,
    speechContent: (@Composable (List<CommunicationMessageItem>) -> Unit)?,
) {
    val layout = presentationLayout(presentationMode)
    val textFactor = MaterialTheme.typography.bodyLarge.fontSize.value / 17f * LocalDensity.current.fontScale
    var query by rememberSaveable { mutableStateOf("") }
    val words = if (categories) CommunicationPersonalVocabulary.search(repository,
        visibleIds ?: repository.vocabulary.map { it.vocabId }.toSet(), VocabularyFilter.VISIBLE, state.selectedCategoryId, query)
        else CommunicationHomeVocabulary.words(repository, homeIds, visibleIds)
    Box(contentAlignment = Alignment.TopCenter) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive((layout.minimumCardWidth * textFactor.coerceAtLeast(1f)).dp),
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()
                .testTag(if (categories) "communication-category-grid" else "communication-home"),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(layout.spacing.dp),
            verticalArrangement = Arrangement.spacedBy(layout.spacing.dp),
        ) {
            item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeading(if (categories) "Buscar más palabras" else "ComunicaGo")
            }
            if (!categories) {
                // Both actions precede long profile names and the message, including large text.
                item(key = "explore", span = { GridItemSpan(maxLineSpan) }) {
                    Button(onClick = onExplore, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("communication-explore")) { Text("Buscar más palabras", Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                }
                if (onOrganizeHome != null) item(key = "organize-home", span = { GridItemSpan(maxLineSpan) }) {
                    OutlinedButton(onClick = onOrganizeHome, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("communication-organize-home")) { Text("Organizar palabras de Inicio", Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                }
                item(key = "profile", span = { GridItemSpan(maxLineSpan) }) { Text("Perfil activo: $activeProfileName") }
                item(key = "message", span = { GridItemSpan(maxLineSpan) }) {
                    CommunicationMessageBar(state.messageItems, state.clearedItems.isNotEmpty(),
                        { onState(builder.removeLast(state)) }, { onState(builder.clear(state)) },
                        { onState(builder.undoClear(state)) }, speechControls = speechContent?.let { content -> { content(state.messageItems) } })
                }
                item(key = "core-title", span = { GridItemSpan(maxLineSpan) }) { SectionHeading("Palabras de Inicio") }
            } else {
                item(key = "use-explanation", span = { GridItemSpan(maxLineSpan) }) {
                    Text("Toca palabras para agregarlas al mensaje. Puedes seguir eligiendo y volver a Inicio cuando quieras.")
                }
                item(key = "categories", span = { GridItemSpan(maxLineSpan) }) {
                    CommunicationCategorySelector(repository.categories.filter { it.active }, state.selectedCategoryId) {
                        onState(builder.selectCategory(state, it))
                    }
                }
                item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
                    OutlinedTextField(query, { query = it }, label = { Text("Buscar palabra o categoría") },
                        modifier = Modifier.fillMaxWidth().testTag("communication-explorer-search"))
                }
                item(key = "vocabulary-title", span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeading("Vocabulario", Modifier.testTag("communication-vocabulary-area"))
                }
                item(key = "count", span = { GridItemSpan(maxLineSpan) }) {
                    Text("${words.size} palabras", Modifier.testTag("communication-vocabulary-count"))
                }
            }
            if (words.isEmpty()) item(key = "empty-vocabulary", span = { GridItemSpan(maxLineSpan) }) {
                if (categories) EmptyState("No hay palabras visibles en esta categoría",
                    "Abre Mi vocabulario para agregar o recuperar palabras.", "Mi vocabulario", onVocabulary)
                else EmptyState("No hay palabras visibles en Inicio",
                    "Puedes elegir palabras disponibles o recuperarlas en Mi vocabulario.",
                    "Agregar palabras a Inicio", onOrganizeHome ?: onVocabulary)
            }
            items(words, key = { if (categories) it.vocabId else "core-${it.vocabId}" }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CommunicationVocabularyCard(it, { id -> onState(builder.add(state, id)) },
                        tag = if (categories) "communication-word-${it.vocabId}" else "communication-core-${it.vocabId}",
                        minimumHeight = (if (categories) layout.wordHeight else layout.quickAccessHeight).dp,
                        contentPadding = layout.cardPadding.dp,
                        emphasized = !categories && presentationMode == CommunicationPresentationMode.GUIDED)
                }
            }
        }
    }
}

@Composable
private fun CompactCommunicationMessage(items: List<CommunicationMessageItem>, onHome: () -> Unit) {
    val message = items.joinToString(" · ") { it.text }.ifEmpty { "Tu mensaje aparecerá aquí" }
    Surface(color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Mensaje: $message", style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("communication-compact-message").semantics {
                    liveRegion = LiveRegionMode.Polite
                })
            TextButton(onClick = onHome, modifier = Modifier.heightIn(min = 48.dp)
                .testTag("communication-view-full-message")) { Text("Volver a Inicio") }
        }
    }
}
