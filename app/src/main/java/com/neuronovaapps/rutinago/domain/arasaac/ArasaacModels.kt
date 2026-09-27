package com.neuronovaapps.rutinago.domain.arasaac

enum class ArasaacImageResolution(val pixels: Int) {
    THUMBNAIL(300), PREVIEW(500),
}

object ArasaacImageUrls {
    fun forPictogram(id: Long, resolution: ArasaacImageResolution): String {
        require(id > 0) { "El ID de ARASAAC debe ser positivo." }
        return "https://static.arasaac.org/pictograms/$id/${id}_${resolution.pixels}.png"
    }
}

data class ArasaacPictogramCandidate(
    val id: Long,
    val keywords: List<String>,
    val description: String?,
    val categories: List<String>,
    val previewUrl: String,
    val source: String = "ARASAAC",
) {
    val label: String get() = keywords.firstOrNull().orEmpty().ifBlank { "Sin palabra asociada" }
}

interface ArasaacRepository {
    suspend fun searchPictograms(query: String): List<ArasaacPictogramCandidate>
    suspend fun getPictogram(id: Long): ArasaacPictogramCandidate
}

enum class ArasaacFailureKind {
    NO_CONNECTION, TIMEOUT, RATE_LIMITED, TEMPORARY, NOT_FOUND, HTTP, INVALID_RESPONSE,
}

class ArasaacApiException(
    val kind: ArasaacFailureKind,
    val statusCode: Int? = null,
    val retryAfterMillis: Long? = null,
    cause: Throwable? = null,
) : Exception("ARASAAC request failed: $kind", cause)
