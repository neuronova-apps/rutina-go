package com.neuronovaapps.rutinago.ui.screens.communication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neuronovaapps.rutinago.data.communication.LocalCommunicationCatalogRepository
import com.neuronovaapps.rutinago.data.communication.CommunicationStateCodec
import com.neuronovaapps.rutinago.domain.communication.CommunicationCatalogRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationMessageBuilder
import com.neuronovaapps.rutinago.domain.communication.CommunicationUiState
import com.neuronovaapps.rutinago.ui.components.ScreenColumn
import com.neuronovaapps.rutinago.ui.components.SectionHeading
import com.neuronovaapps.rutinago.ui.screens.communication.components.CommunicationCategorySelector
import com.neuronovaapps.rutinago.ui.screens.communication.components.CommunicationMessageBar
import com.neuronovaapps.rutinago.ui.screens.communication.components.CommunicationVocabularyCard
import com.neuronovaapps.rutinago.ui.theme.LocalHighContrast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CommunicationHomeScreen(
    activeProfileName: String,
    onPersonalize: (() -> Unit)? = null,
    onBackToModules: () -> Unit,
) {
    val assets = LocalContext.current.applicationContext.assets
    val catalog by produceState<Result<CommunicationCatalogRepository>?>(null, assets) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                LocalCommunicationCatalogRepository {
                    assets.open(LocalCommunicationCatalogRepository.ASSET_PATH)
                        .bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
            }
        }
    }
    val repository = catalog?.getOrNull()
    BackHandler(onBack = onBackToModules)
    if (repository == null) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            ScreenColumn(Modifier.safeDrawingPadding().testTag("communication-loading")) {
                OutlinedButton(onClick = onBackToModules, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Volver al selector")
                }
                SectionHeading("ComunicaGo")
                Text("Perfil activo: $activeProfileName")
                Text(if (catalog == null) "Cargando catálogo…"
                    else "No se pudo cargar el catálogo local. Vuelve al selector e inténtalo de nuevo.")
            }
        }
    } else {
        CommunicationBuilderScreen(activeProfileName, repository, onPersonalize, onBackToModules)
    }
}

@Composable
private fun CommunicationBuilderScreen(
    activeProfileName: String,
    repository: CommunicationCatalogRepository,
    onPersonalize: (() -> Unit)?,
    onBackToModules: () -> Unit,
) {
    val builder = remember(repository) { CommunicationMessageBuilder(repository) }
    val saver = remember(builder) {
        Saver<CommunicationUiState, String>(
            save = { CommunicationStateCodec.encode(it) },
            restore = { runCatching { CommunicationStateCodec.decode(it, builder) }.getOrNull() },
        )
    }
    var state by rememberSaveable(stateSaver = saver) { mutableStateOf(builder.emptyState()) }
    val words = builder.visibleVocabulary(state)
    val highContrast = LocalHighContrast.current
    val gridState = rememberLazyGridState()
    val textFactor = MaterialTheme.typography.bodyLarge.fontSize.value / 17f * LocalDensity.current.fontScale
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.TopCenter) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive((144f * textFactor.coerceAtLeast(1f)).dp),
                state = gridState,
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().safeDrawingPadding()
                    .testTag("communication-home"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "back", span = { GridItemSpan(maxLineSpan) }) {
                    OutlinedButton(
                        onClick = onBackToModules,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("communication-back-to-modules"),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (highContrast) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.primary,
                        ),
                        border = BorderStroke(if (highContrast) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
                    ) { Text("Volver al selector", textAlign = TextAlign.Center) }
                }
                item(key = "title", span = { GridItemSpan(maxLineSpan) }) { SectionHeading("ComunicaGo") }
                item(key = "profile", span = { GridItemSpan(maxLineSpan) }) { Text("Perfil activo: $activeProfileName") }
                if (onPersonalize != null) {
                    item(key = "personalize", span = { GridItemSpan(maxLineSpan) }) {
                        OutlinedButton(onClick = onPersonalize,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("communication-personalize"),
                            shape = MaterialTheme.shapes.medium) { Text("Personalizar ComunicaGo", textAlign = TextAlign.Center) }
                    }
                }
                item(key = "message", span = { GridItemSpan(maxLineSpan) }) {
                    CommunicationMessageBar(
                        items = state.messageItems,
                        canUndoClear = state.clearedItems.isNotEmpty(),
                        onRemoveLast = { state = builder.removeLast(state) },
                        onClear = { state = builder.clear(state) },
                        onUndoClear = { state = builder.undoClear(state) },
                    )
                }
                item(key = "core-title", span = { GridItemSpan(maxLineSpan) }) { SectionHeading("Acceso rápido") }
                items(builder.coreVocabulary, key = { "core-${it.vocabId}" }) {
                    CommunicationVocabularyCard(it, { id -> state = builder.add(state, id) },
                        tag = "communication-core-${it.vocabId}")
                }
                item(key = "categories-title", span = { GridItemSpan(maxLineSpan) }) { SectionHeading("Categorías") }
                item(key = "categories", span = { GridItemSpan(maxLineSpan) }) {
                    CommunicationCategorySelector(repository.categories.filter { it.active }, state.selectedCategoryId) {
                        state = builder.selectCategory(state, it)
                    }
                }
                item(key = "vocabulary-title", span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeading("Vocabulario", Modifier.testTag("communication-vocabulary-area"))
                }
                item(key = "count", span = { GridItemSpan(maxLineSpan) }) {
                    Text("${words.size} palabras", Modifier.testTag("communication-vocabulary-count"))
                }
                items(words, key = { it.vocabId }) {
                    CommunicationVocabularyCard(it, { id -> state = builder.add(state, id) })
                }
            }
        }
    }
}
