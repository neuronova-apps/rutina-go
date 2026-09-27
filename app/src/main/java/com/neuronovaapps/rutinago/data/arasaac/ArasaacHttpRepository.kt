package com.neuronovaapps.rutinago.data.arasaac

import android.util.Log
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacApiException
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacFailureKind
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacPictogramCandidate
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacRepository
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl

class ArasaacHttpRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val baseUrl: HttpUrl = "https://api.arasaac.org/".toHttpUrl(),
    private val onFailure: (ArasaacApiException) -> Unit = {},
) : ArasaacRepository {
    override suspend fun searchPictograms(query: String): List<ArasaacPictogramCandidate> {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) return emptyList()
        val url = baseUrl.newBuilder()
            .addPathSegments("v1/pictograms/es/search")
            .addPathSegment(cleaned)
            .build()
        return try {
            val result = execute(url)
            if (result.statusCode == 404) return emptyList()
            checkStatus(result)
            parse { ArasaacDtoParser.search(result.body).map(ArasaacPictogramDto::toCandidate) }
        } catch (error: ArasaacApiException) {
            onFailure(error)
            throw error
        }
    }

    override suspend fun getPictogram(id: Long): ArasaacPictogramCandidate {
        require(id > 0) { "El ID debe provenir de ARASAAC." }
        val url = baseUrl.newBuilder()
            .addPathSegments("v1/pictograms/es")
            .addPathSegment(id.toString())
            .build()
        return try {
            val result = execute(url)
            if (result.statusCode == 404) {
                throw ArasaacApiException(ArasaacFailureKind.NOT_FOUND, 404)
            }
            checkStatus(result)
            parse {
                ArasaacDtoParser.detail(result.body).toCandidate().also { candidate ->
                    require(candidate.id == id) { "El detalle no corresponde al ID solicitado." }
                }
            }
        } catch (error: ArasaacApiException) {
            onFailure(error)
            throw error
        }
    }

    private inline fun <T> parse(block: () -> T): T = try {
        block()
    } catch (error: Exception) {
        throw ArasaacApiException(ArasaacFailureKind.INVALID_RESPONSE, cause = error)
    }

    private fun checkStatus(result: HttpResult) {
        when (result.statusCode) {
            in 200..299 -> Unit
            429 -> throw ArasaacApiException(
                ArasaacFailureKind.RATE_LIMITED,
                statusCode = 429,
                retryAfterMillis = result.retryAfterSeconds
                    ?.coerceIn(2, 30)?.times(1_000) ?: 3_000,
            )
            in 500..599 -> throw ArasaacApiException(
                ArasaacFailureKind.TEMPORARY,
                statusCode = result.statusCode,
            )
            else -> throw ArasaacApiException(
                ArasaacFailureKind.HTTP,
                statusCode = result.statusCode,
            )
        }
    }

    private suspend fun execute(url: HttpUrl): HttpResult {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()
        return try {
            suspendCancellableCoroutine { continuation ->
                val call = client.newCall(request)
                continuation.invokeOnCancellation { call.cancel() }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            response.use {
                                val result = HttpResult(
                                    statusCode = it.code,
                                    body = it.body.string(),
                                    retryAfterSeconds = it.header("Retry-After")?.toLongOrNull(),
                                )
                                if (continuation.isActive) continuation.resume(result)
                            }
                        } catch (error: Exception) {
                            if (continuation.isActive) continuation.resumeWithException(error)
                        }
                    }
                })
            }
        } catch (error: IOException) {
            val kind = when (error) {
                is InterruptedIOException -> ArasaacFailureKind.TIMEOUT
                is UnknownHostException, is ConnectException -> ArasaacFailureKind.NO_CONNECTION
                else -> ArasaacFailureKind.NO_CONNECTION
            }
            throw ArasaacApiException(kind, cause = error)
        }
    }

    private data class HttpResult(
        val statusCode: Int,
        val body: String,
        val retryAfterSeconds: Long?,
    )
}

object DefaultArasaacRepository : ArasaacRepository by ArasaacHttpRepository(
    onFailure = { error ->
        // Never log the search text, profile, routine, response body or image URL.
        Log.w("RutinaGoARASAAC", "request=${error.kind} status=${error.statusCode ?: "none"}")
    },
)
