package com.neuronovaapps.rutinago.domain.user

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** Small, versioned reference stored on a personal step and copied into execution history. */
data class RoutineStepVisualReference(
    val source: String,
    val pictogramId: Long,
    val label: String,
) {
    init {
        require(source == SOURCE_ARASAAC) { "Fuente visual desconocida." }
        require(pictogramId > 0) { "El ID de ARASAAC debe ser positivo." }
        require(label.isNotBlank()) { "El pictograma necesita una etiqueta." }
    }

    companion object { const val SOURCE_ARASAAC = "ARASAAC" }
}

object RoutineStepVisualCodec {
    private const val SCHEMA = "rutinago.visual.v1"

    fun encode(reference: RoutineStepVisualReference): String = JsonObject().apply {
        addProperty("schema", SCHEMA)
        addProperty("source", reference.source)
        addProperty("id", reference.pictogramId)
        addProperty("label", reference.label.trim())
    }.toString()

    fun decode(raw: String?): RoutineStepVisualReference? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val value = JsonParser.parseString(raw).asJsonObject
            if (value.get("schema")?.asString != SCHEMA) return null
            val idText = value.get("id").asString
            if (!idText.matches(Regex("[1-9][0-9]*"))) return null
            RoutineStepVisualReference(
                source = value.get("source").asString,
                pictogramId = idText.toLong(),
                label = value.get("label").asString,
            )
        }.getOrNull()
    }
}
