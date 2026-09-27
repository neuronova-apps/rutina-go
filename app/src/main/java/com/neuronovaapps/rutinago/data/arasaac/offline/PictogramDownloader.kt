package com.neuronovaapps.rutinago.data.arasaac.offline

import com.neuronovaapps.rutinago.domain.arasaac.ArasaacImageResolution
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacImageUrls
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

enum class PictogramDownloadResult { READY, RETRY, PERMANENT_FAILURE }

/** Downloads only a selected, persisted ID supplied by the worker. */
class PictogramDownloader(
    private val store: PictogramFileStore,
    private val client: OkHttpClient = defaultClient,
    private val imageUrl: (Long) -> String = { id ->
        ArasaacImageUrls.forPictogram(id, OFFLINE_RESOLUTION)
    },
) {
    suspend fun download(id: Long): PictogramDownloadResult = withContext(Dispatchers.IO) {
        require(id > 0) { "El ID de ARASAAC debe ser positivo." }
        if (store.exists(id)) return@withContext PictogramDownloadResult.READY
        val request = Request.Builder().url(imageUrl(id)).header("Accept", "image/png").build()
        try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> Unit
                    404 -> return@withContext PictogramDownloadResult.PERMANENT_FAILURE
                    429 -> return@withContext PictogramDownloadResult.RETRY
                    in 500..599 -> return@withContext PictogramDownloadResult.RETRY
                    else -> return@withContext PictogramDownloadResult.PERMANENT_FAILURE
                }
                val body = response.body ?: return@withContext PictogramDownloadResult.RETRY
                val contentType = response.header("Content-Type")
                if (contentType != null && !contentType.substringBefore(';').trim()
                        .equals("image/png", ignoreCase = true)) {
                    return@withContext PictogramDownloadResult.PERMANENT_FAILURE
                }
                if (body.contentLength() == 0L || body.contentLength() > PictogramFileStore.MAX_BYTES) {
                    return@withContext PictogramDownloadResult.PERMANENT_FAILURE
                }
                try {
                    store.save(id, body.byteStream(), body.contentLength())
                    PictogramDownloadResult.READY
                } catch (_: IncompletePictogramException) {
                    PictogramDownloadResult.RETRY
                } catch (_: InvalidPictogramException) {
                    PictogramDownloadResult.PERMANENT_FAILURE
                } catch (_: PictogramStorageException) {
                    PictogramDownloadResult.PERMANENT_FAILURE
                }
            }
        } catch (_: IOException) {
            PictogramDownloadResult.RETRY
        }
    }

    companion object {
        val OFFLINE_RESOLUTION = ArasaacImageResolution.PREVIEW // 500 px
        private val defaultClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
