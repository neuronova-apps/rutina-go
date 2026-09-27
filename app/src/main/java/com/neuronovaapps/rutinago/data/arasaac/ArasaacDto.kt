package com.neuronovaapps.rutinago.data.arasaac

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacImageResolution
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacImageUrls
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacPictogramCandidate

internal data class ArasaacKeywordDto(val word: String, val meaning: String?)

internal data class ArasaacPictogramDto(
    val id: Long,
    val keywords: List<ArasaacKeywordDto>,
    val description: String?,
    val categories: List<String>,
) {
    fun toCandidate() = ArasaacPictogramCandidate(
        id = id,
        keywords = keywords.map(ArasaacKeywordDto::word),
        description = description?.takeIf(String::isNotBlank)
            ?: keywords.firstNotNullOfOrNull { it.meaning?.takeIf(String::isNotBlank) },
        categories = categories,
        previewUrl = ArasaacImageUrls.forPictogram(id, ArasaacImageResolution.THUMBNAIL),
    )
}

internal object ArasaacDtoParser {
    fun search(json: String): List<ArasaacPictogramDto> {
        val root = JsonParser.parseString(json)
        require(root.isJsonArray) { "La búsqueda no devolvió una lista." }
        val array = root.asJsonArray
        val results = array.mapNotNull(::parsePictogram)
        require(array.size() == 0 || results.isNotEmpty()) { "No hay IDs válidos en la respuesta." }
        return results
    }

    fun detail(json: String): ArasaacPictogramDto =
        requireNotNull(parsePictogram(JsonParser.parseString(json))) {
            "El detalle no contiene un ID de ARASAAC válido."
        }

    private fun parsePictogram(element: JsonElement): ArasaacPictogramDto? {
        if (!element.isJsonObject) return null
        val item = element.asJsonObject
        val rawId = item.get("_id") ?: return null
        if (!rawId.isJsonPrimitive || !rawId.asJsonPrimitive.isNumber) return null
        val id = rawId.asString.toLongOrNull()?.takeIf { it > 0 } ?: return null
        val words = item.get("keywords")
            ?.takeIf(JsonElement::isJsonArray)
            ?.asJsonArray
            ?.mapNotNull { keyword ->
                if (!keyword.isJsonObject) return@mapNotNull null
                val word = keyword.asJsonObject.get("keyword")?.stringOrNull()
                    ?.trim()?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
                ArasaacKeywordDto(word, keyword.asJsonObject.get("meaning")?.stringOrNull()?.trim())
            }
            .orEmpty()
        val categories = item.get("categories")
            ?.takeIf(JsonElement::isJsonArray)
            ?.asJsonArray
            ?.mapNotNull { it.stringOrNull()?.trim()?.takeIf(String::isNotEmpty) }
            .orEmpty()
        return ArasaacPictogramDto(
            id = id,
            keywords = words,
            description = item.get("desc")?.stringOrNull()?.trim(),
            categories = categories,
        )
    }

    private fun JsonElement.stringOrNull(): String? =
        if (isJsonPrimitive && asJsonPrimitive.isString) asString else null
}
