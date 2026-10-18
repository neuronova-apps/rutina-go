package com.neuronovaapps.rutinago.domain.communication

import java.text.Normalizer
import kotlinx.coroutines.flow.Flow

interface CommunicationPersonalVocabularyRepository {
    /** null = never configured; an empty set = explicitly no visible words. */
    fun vocabularyForProfile(profileId: String): Flow<Set<String>?>
    suspend fun saveVocabulary(profileId: String, visibleIds: Set<String>)
}

enum class VocabularyFilter(val label: String) {
    ALL("Todas las palabras"), VISIBLE("Palabras visibles"), HIDDEN("Palabras ocultas"),
}

object CommunicationPersonalVocabulary {
    /** Editable functional starting point, identical for all ages and speech contexts. */
    fun suggestedIds(catalog: CommunicationCatalogRepository): Set<String> =
        (CommunicationMessageBuilder.CORE_VOCABULARY_IDS + listOf(
            "VOC_003", "VOC_004", "VOC_008", "VOC_009", "VOC_011", "VOC_013",
            "VOC_015", "VOC_021", "VOC_022", "VOC_026", "VOC_027", "VOC_030",
            "VOC_049", "VOC_054", "VOC_069", "VOC_070", "VOC_091", "VOC_092",
        )).toSet().intersect(catalog.vocabulary.map { it.vocabId }.toSet())

    // Verified references for agreement, refusal, needs, discomfort and distress.
    private val importantIds = setOf(
        "VOC_007", "VOC_008", "VOC_009", "VOC_012", "VOC_013", "VOC_014", "VOC_015",
        "VOC_021", "VOC_027", "VOC_028", "VOC_029", "VOC_030", "VOC_041", "VOC_042",
        "VOC_049", "VOC_053", "VOC_054", "VOC_055", "VOC_063", "VOC_070", "VOC_073",
        "VOC_091", "VOC_092", "VOC_094", "VOC_096", "VOC_097", "VOC_098",
    )
    fun needsHideWarning(id: String) = id in importantIds

    fun visibleWords(words: List<CommunicationVocabularyItem>, visibleIds: Set<String>?): List<CommunicationVocabularyItem> =
        if (visibleIds == null) words else words.filter { it.vocabId in visibleIds }

    fun search(catalog: CommunicationCatalogRepository, visibleIds: Set<String>, filter: VocabularyFilter,
        categoryId: String?, query: String): List<CommunicationVocabularyItem> {
        val needle = normalize(query.trim())
        val categories = catalog.categories.associate { it.id to it.name }
        return catalog.vocabularyForCategory(categoryId).filter { word ->
            val visible = word.vocabId in visibleIds
            (filter == VocabularyFilter.ALL || (filter == VocabularyFilter.VISIBLE) == visible) &&
                (needle.isEmpty() || normalize("${word.text} ${categories[word.categoryId]} ${word.vocabId}").contains(needle))
        }
    }

    private fun normalize(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(java.util.Locale.ROOT)
}
