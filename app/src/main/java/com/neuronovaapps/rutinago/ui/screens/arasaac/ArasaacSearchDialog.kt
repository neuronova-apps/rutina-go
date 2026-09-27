package com.neuronovaapps.rutinago.ui.screens.arasaac

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacApiException
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacFailureKind
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacImageResolution
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacImageUrls
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacPictogramCandidate
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacRepository
import com.neuronovaapps.rutinago.domain.catalog.CatalogActivity
import com.neuronovaapps.rutinago.ui.theme.LocalHighContrast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val ARASAAC_ATTRIBUTION =
    "Pictogramas: Sergio Palao para ARASAAC. Propiedad: Gobierno de Aragón. " +
        "Licencia: Creative Commons BY-NC-SA."

@Composable
fun ArasaacSearchDialog(
    activity: CatalogActivity,
    repository: ArasaacRepository,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val controller = remember(activity.id, repository) {
        ArasaacSearchController(activity.arasaacSearchTerm, repository, scope)
    }
    DisposableEffect(controller) { onDispose { controller.close() } }
    val state by controller.state.collectAsState()
    var query by remember(activity.id) { mutableStateOf(controller.query) }
    var inspected by remember(activity.id) { mutableStateOf<ArasaacPictogramCandidate?>(null) }
    var detail by remember(activity.id) { mutableStateOf<ArasaacPictogramCandidate?>(null) }
    var detailError by remember(activity.id) { mutableStateOf<ArasaacFailureKind?>(null) }
    var detailLoading by remember(activity.id) { mutableStateOf(false) }
    var provisional by remember(activity.id) { mutableStateOf<ArasaacPictogramCandidate?>(null) }
    var detailJob by remember(activity.id) { mutableStateOf<Job?>(null) }
    var detailRetryDelay by remember(activity.id) { mutableStateOf(0L) }

    fun open(candidate: ArasaacPictogramCandidate, retry: Boolean = false) {
        detailJob?.cancel()
        inspected = candidate
        detail = null
        detailError = null
        detailLoading = true
        detailJob = scope.launch {
            try {
                if (retry) delay(detailRetryDelay)
                detail = repository.getPictogram(candidate.id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: ArasaacApiException) {
                detailError = error.kind
                detailRetryDelay = when (error.kind) {
                    ArasaacFailureKind.RATE_LIMITED -> error.retryAfterMillis ?: 3_000
                    ArasaacFailureKind.TEMPORARY -> 1_500
                    else -> 0
                }
            } catch (error: Exception) {
                detailError = ArasaacFailureKind.TEMPORARY
            } finally {
                detailLoading = false
            }
        }
    }

    fun back() {
        if (inspected != null) {
            detailJob?.cancel()
            inspected = null
            detail = null
            detailError = null
        } else onDismiss()
    }

    Dialog(
        onDismissRequest = ::back,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BackHandler(onBack = ::back)
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = ::back) {
                    Text(if (inspected == null) "‹ Volver a Crear" else "‹ Volver a resultados")
                }
                Text(
                    if (inspected == null) "Buscar pictograma" else "Previsualizar pictograma",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text("Actividad: ${activity.name}", style = MaterialTheme.typography.bodyMedium)
                if (inspected == null) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; controller.setQuery(it) },
                        label = { Text("Buscar en ARASAAC") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("arasaac-query"),
                    )
                    provisional?.let {
                        Text(
                            "Selección provisional: ${it.label} (ID ${it.id}). No guardada.",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                    when (val current = state) {
                        ArasaacSearchState.Idle -> StatusText("No has realizado una búsqueda.")
                        ArasaacSearchState.Loading -> Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            StatusText("Buscando pictogramas…")
                        }
                        ArasaacSearchState.Empty -> StatusText(
                            "No encontramos pictogramas para esta búsqueda. Prueba con otras palabras.",
                        )
                        is ArasaacSearchState.Error -> {
                            StatusText(errorMessage(current.failure))
                            Button(onClick = controller::retry) { Text("Reintentar") }
                        }
                        is ArasaacSearchState.Success -> LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(current.results, key = ArasaacPictogramCandidate::id) { candidate ->
                                CandidateCard(candidate) { open(candidate) }
                            }
                        }
                    }
                } else {
                    val candidate = detail ?: inspected!!
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PictogramImage(
                            url = ArasaacImageUrls.forPictogram(
                                candidate.id, ArasaacImageResolution.PREVIEW,
                            ),
                            label = candidate.label,
                            size = 180,
                        )
                        Text(candidate.keywords.joinToString().ifBlank { candidate.label },
                            style = MaterialTheme.typography.titleLarge)
                        candidate.description?.let { Text(it) }
                        if (candidate.categories.isNotEmpty()) {
                            Text("Categorías: ${candidate.categories.joinToString()}")
                        }
                        Text("ID ARASAAC: ${candidate.id} · Fuente: ${candidate.source}")
                        if (detailLoading) StatusText("Cargando detalles…")
                        detailError?.let {
                            StatusText("No se pudo cargar el detalle. ${errorMessage(it)}")
                            TextButton(onClick = { open(candidate, retry = true) }) {
                                Text("Reintentar detalle")
                            }
                        }
                        provisional?.takeIf { it.id == candidate.id }?.let {
                            StatusText("Selección provisional. No guardada en el catálogo ni en la rutina.")
                        }
                        Text(ARASAAC_ATTRIBUTION, style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = { provisional = candidate },
                            enabled = detail != null,
                            modifier = Modifier.fillMaxWidth().testTag("arasaac-provisional-select"),
                        ) { Text("Seleccionar provisionalmente") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateCard(candidate: ArasaacPictogramCandidate, onClick: () -> Unit) {
    val borderWidth = if (LocalHighContrast.current) 2.dp else 1.dp
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("arasaac-result-${candidate.id}").semantics {
            contentDescription = "Pictograma candidato: ${candidate.label}, ID ARASAAC ${candidate.id}"
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(borderWidth, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PictogramImage(candidate.previewUrl, candidate.label, 72)
            Column {
                Text(candidate.label, style = MaterialTheme.typography.titleMedium)
                candidate.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text("ID ${candidate.id} · ARASAAC", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PictogramImage(url: String, label: String, size: Int) {
    var failed by remember(url) { mutableStateOf(false) }
    val context = LocalContext.current
    val request = remember(url, context) {
        ImageRequest.Builder(context).data(url).diskCachePolicy(CachePolicy.DISABLED).build()
    }
    Surface(
        modifier = Modifier.size(size.dp),
        color = Color.White,
        border = BorderStroke(
            if (LocalHighContrast.current) 2.dp else 1.dp,
            MaterialTheme.colorScheme.outline,
        ),
    ) {
        if (failed) {
            Text("Imagen no disponible", modifier = Modifier.padding(8.dp),
                color = Color.Black, style = MaterialTheme.typography.bodySmall)
        } else {
            AsyncImage(
                model = request,
                contentDescription = "Pictograma de $label",
                onError = { failed = true },
                modifier = Modifier.fillMaxSize().padding(4.dp),
            )
        }
    }
}

@Composable
private fun StatusText(message: String) {
    Text(message, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

private fun errorMessage(kind: ArasaacFailureKind): String = when (kind) {
    ArasaacFailureKind.NO_CONNECTION ->
        "No hay conexión a Internet. Comprueba tu conexión e inténtalo nuevamente."
    ArasaacFailureKind.TIMEOUT -> "La consulta tardó demasiado. Inténtalo nuevamente."
    ArasaacFailureKind.RATE_LIMITED ->
        "ARASAAC recibió demasiadas consultas. Espera un momento antes de reintentar."
    ArasaacFailureKind.NOT_FOUND -> "Este pictograma ya no está disponible en ARASAAC."
    ArasaacFailureKind.INVALID_RESPONSE -> "ARASAAC devolvió una respuesta inesperada."
    ArasaacFailureKind.TEMPORARY, ArasaacFailureKind.HTTP ->
        "No se pudo consultar ARASAAC en este momento."
}
