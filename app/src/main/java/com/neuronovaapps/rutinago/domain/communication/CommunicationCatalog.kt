package com.neuronovaapps.rutinago.domain.communication

data class CommunicationCategory(
    val id: String,
    val name: String,
    val description: String,
    val order: Int,
    val simpleMode: Boolean,
    val advancedMode: Boolean,
    val active: Boolean,
)

/** All editorial fields are preserved, including intentionally empty metadata. */
data class CommunicationVocabularyItem(
    val vocabId: String,
    val text: String,
    val categoryId: String,
    val grammaticalType: String,
    val arasaacTerm: String,
    val simpleMode: Boolean,
    val advancedMode: Boolean,
    val quickPhrase: Boolean,
    val order: Int,
    val status: String,
    val observations: String,
    val alternateArasaacTerm: String,
    val arasaacTermStatus: String,
    val arasaacId: String,
    val curationStatus: String,
    val curationSource: String,
    val curationCriterion: String,
    val visualStrategy: String,
    val simpleModeTreatment: String,
    val visualStrategyCriterion: String,
)

interface CommunicationCatalogRepository {
    val categories: List<CommunicationCategory>
    val vocabulary: List<CommunicationVocabularyItem>

    /** null is the UI's “Todos” filter; it never becomes a source category. */
    fun vocabularyForCategory(categoryId: String?): List<CommunicationVocabularyItem>
}
