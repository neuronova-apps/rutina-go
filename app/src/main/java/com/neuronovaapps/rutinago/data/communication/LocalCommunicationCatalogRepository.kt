package com.neuronovaapps.rutinago.data.communication

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.neuronovaapps.rutinago.domain.communication.CommunicationCatalogRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationCategory
import com.neuronovaapps.rutinago.domain.communication.CommunicationVocabularyItem
import java.util.Collections

/** Reads one packaged asset atomically. No database, profile state, or network dependencies. */
class LocalCommunicationCatalogRepository(readCatalog: () -> String) : CommunicationCatalogRepository {
    override val categories: List<CommunicationCategory>
    override val vocabulary: List<CommunicationVocabularyItem>

    init {
        val root = JsonParser.parseString(readCatalog()).asJsonObject
        require(root.int("version") == 1) { "Unsupported communication catalog version" }
        require(root.string("contractVersion") == "1.0") { "Unsupported source contract" }
        require(root.string("sourceUrl").isNotBlank() && root.string("capturedOn").isNotBlank())
        val counts = root.getAsJsonObject("counts")
        mapOf("categories" to 18, "vocabulary" to 132, "validated" to 111,
            "pending" to 21, "textOnly" to 10, "pictogramPending" to 11).forEach { (key, expected) ->
            require(counts.int(key) == expected) { "Invalid declared count: $key" }
        }
        val parsedCategories = root.getAsJsonArray("categories").map { element ->
            val row = element.asJsonObject
            require(row.keySet() == CATEGORY_FIELDS) { "Unexpected category fields" }
            CommunicationCategory(
                row.string("categoria_id"), row.string("categoria"), row.string("descripcion"),
                row.int("orden"), row.boolean("modo_simple"), row.boolean("modo_avanzado"),
                row.boolean("activo"),
            )
        }
        val parsedVocabulary = root.getAsJsonArray("vocabulary").map { element ->
            val row = element.asJsonObject
            require(row.keySet() == VOCABULARY_FIELDS) { "Unexpected vocabulary fields" }
            CommunicationVocabularyItem(
                row.string("vocab_id"), row.string("texto"), row.string("categoria_id"),
                row.string("tipo_gramatical"), row.string("termino_arasaac"),
                row.boolean("modo_simple"), row.boolean("modo_avanzado"), row.boolean("frase_rapida"),
                row.int("orden"), row.string("estado"), row.string("observaciones"),
                row.string("termino_arasaac_alt"), row.string("estado_termino_arasaac"),
                row.id("arasaac_id"), row.string("estado_curacion"), row.string("fuente_curacion"),
                row.string("criterio_curacion"), row.string("estrategia_visual"),
                row.string("tratamiento_modo_simple"), row.string("criterio_estrategia_visual"),
            )
        }
        require(parsedCategories.size == 18 && parsedVocabulary.size == 132) { "Catalog count mismatch" }
        require(parsedCategories.map { it.id }.toSet().size == 18) { "Duplicate category ID" }
        require(parsedVocabulary.map { it.vocabId }.toSet().size == 132) { "Duplicate vocabulary ID" }
        val categoryIds = parsedCategories.map { it.id }.toSet()
        parsedCategories.forEach {
            require(it.id.matches(Regex("CAT_\\d{3}")) && it.name.isNotBlank() && it.order > 0)
        }
        parsedVocabulary.forEach {
            require(it.vocabId.matches(Regex("VOC_\\d{3}")) && it.text.isNotBlank() && it.order > 0)
            require(it.categoryId in categoryIds) { "Broken category reference: ${it.vocabId}" }
            require(it.status in setOf("VALIDADO", "PENDIENTE_ARASAAC"))
            require(it.visualStrategy in setOf("", "PICTOGRAMA_VALIDADO", "SOLO_TEXTO", "PICTOGRAMA_PENDIENTE"))
            require(it.simpleModeTreatment in setOf("", "VISIBLE_SIMPLE", "OCULTO_SIMPLE"))
            if (it.status == "VALIDADO" || it.visualStrategy == "PICTOGRAMA_VALIDADO") {
                require(it.arasaacId.isNotEmpty()) { "Validated vocabulary requires an ARASAAC ID" }
            }
        }
        require(parsedVocabulary.count { it.status == "VALIDADO" } == 111)
        require(parsedVocabulary.count { it.status == "PENDIENTE_ARASAAC" } == 21)
        require(parsedVocabulary.count { it.visualStrategy == "SOLO_TEXTO" } == 10)
        require(parsedVocabulary.count { it.visualStrategy == "PICTOGRAMA_PENDIENTE" } == 11)
        // The contract explicitly defines source ID as the tie-breaker; never sort by label.
        categories = immutable(parsedCategories.sortedWith(compareBy({ it.order }, { it.id })))
        vocabulary = immutable(parsedVocabulary.sortedWith(compareBy({ it.order }, { it.vocabId })))
    }

    override fun vocabularyForCategory(categoryId: String?): List<CommunicationVocabularyItem> {
        if (categoryId == null) return vocabulary
        require(categories.any { it.id == categoryId }) { "Unknown category: $categoryId" }
        return immutable(vocabulary.filter { it.categoryId == categoryId })
    }

    companion object {
        const val ASSET_PATH = "communication/communication_catalog_v1.json"
        private val CATEGORY_FIELDS = setOf("categoria_id", "categoria", "descripcion", "orden",
            "modo_simple", "modo_avanzado", "activo")
        private val VOCABULARY_FIELDS = setOf("vocab_id", "texto", "categoria_id", "tipo_gramatical",
            "termino_arasaac", "modo_simple", "modo_avanzado", "frase_rapida", "orden", "estado",
            "observaciones", "termino_arasaac_alt", "estado_termino_arasaac", "arasaac_id",
            "estado_curacion", "fuente_curacion", "criterio_curacion", "estrategia_visual",
            "tratamiento_modo_simple", "criterio_estrategia_visual")

        private fun <T> immutable(values: List<T>): List<T> = Collections.unmodifiableList(ArrayList(values))
        private fun JsonObject.value(key: String): JsonElement = requireNotNull(get(key)) { "Missing $key" }
        private fun JsonObject.string(key: String): String = value(key).let {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isString) { "Invalid string: $key" }
            it.asString
        }
        private fun JsonObject.int(key: String): Int = value(key).let {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isNumber && it.toString().matches(Regex("[0-9]+"))) {
                "Invalid integer: $key"
            }
            it.asString.toInt()
        }
        private fun JsonObject.boolean(key: String): Boolean = value(key).let {
            require(it.isJsonPrimitive && it.asJsonPrimitive.isBoolean) { "Invalid boolean: $key" }
            it.asBoolean
        }
        private fun JsonObject.id(key: String): String = value(key).let {
            require(it.isJsonPrimitive && (it.asJsonPrimitive.isString || it.asJsonPrimitive.isNumber))
            it.asString.also { id -> require(id.isEmpty() || id.matches(Regex("[1-9][0-9]*"))) }
        }
    }
}
