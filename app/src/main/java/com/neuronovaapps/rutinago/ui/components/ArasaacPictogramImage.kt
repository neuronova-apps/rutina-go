package com.neuronovaapps.rutinago.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.neuronovaapps.rutinago.data.arasaac.offline.PictogramDownloadWorker
import com.neuronovaapps.rutinago.data.arasaac.offline.PictogramFileStore
import com.neuronovaapps.rutinago.data.arasaac.offline.PictogramImageResolver
import com.neuronovaapps.rutinago.ui.theme.LocalHighContrast
import java.io.File

/** Local private file first; remote URL next; accessible text if both fail. */
@Composable
fun ArasaacPictogramImage(
    url: String,
    label: String,
    size: Int,
    offlineId: Long? = null,
) {
    val context = LocalContext.current
    val store = remember(context.applicationContext) {
        PictogramFileStore.fromFilesDir(context.filesDir)
    }
    val resolver = remember(store) { PictogramImageResolver(store) }
    val workInfos by if (offlineId != null) {
        val flow = remember(context.applicationContext, offlineId) {
            WorkManager.getInstance(context.applicationContext)
                .getWorkInfosForUniqueWorkFlow(PictogramDownloadWorker.uniqueName(offlineId))
        }
        flow.collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<WorkInfo>()) }
    }
    val preferredModel = remember(offlineId, workInfos, url) {
        resolver.modelFor(offlineId, url)
    }
    var localFailed by remember(preferredModel) { mutableStateOf(false) }
    var remoteFailed by remember(url, preferredModel) { mutableStateOf(false) }
    val model = if (preferredModel is File && !localFailed) preferredModel else url
    val request = remember(model, context) {
        ImageRequest.Builder(context).data(model).diskCachePolicy(CachePolicy.DISABLED).build()
    }
    Surface(
        modifier = Modifier.size(size.dp),
        color = Color.White,
        border = BorderStroke(
            if (LocalHighContrast.current) 2.dp else 1.dp,
            MaterialTheme.colorScheme.outline,
        ),
    ) {
        if (remoteFailed) {
            Text("Pictograma no disponible", modifier = Modifier.padding(8.dp),
                color = Color.Black, style = MaterialTheme.typography.bodySmall)
        } else {
            AsyncImage(
                model = request,
                contentDescription = "Pictograma de $label",
                onError = {
                    if (model is File) localFailed = true else remoteFailed = true
                },
                modifier = Modifier.fillMaxSize().padding(4.dp),
            )
        }
    }
}
