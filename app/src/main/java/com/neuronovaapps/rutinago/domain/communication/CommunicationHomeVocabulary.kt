package com.neuronovaapps.rutinago.domain.communication

import kotlinx.coroutines.flow.Flow

interface CommunicationHomeVocabularyRepository {
    /** null = no customization; empty = the user's explicit choice. */
    fun homeVocabularyForProfile(profileId: String): Flow<Set<String>?>
    /** Atomic membership change, independent of message construction and available vocabulary. */
    suspend fun setHomeWord(profileId: String, vocabularyId: String, inHome: Boolean)
}

object CommunicationHomeVocabulary {
    fun assignedIds(homeIds: Set<String>?, availableIds: Set<String>?): Set<String> = homeIds
        ?: CommunicationMessageBuilder.CORE_VOCABULARY_IDS.filter { availableIds == null || it in availableIds }.toSet()

    /** Hidden assignments survive internally. Only available, real catalog records are displayed. */
    fun words(catalog: CommunicationCatalogRepository, homeIds: Set<String>?, availableIds: Set<String>?): List<CommunicationVocabularyItem> {
        val ids = assignedIds(homeIds, availableIds)
        val words = CommunicationPersonalVocabulary.visibleWords(catalog.vocabulary, availableIds).filter { it.vocabId in ids }
        return if (homeIds == null) words.sortedBy { CommunicationMessageBuilder.CORE_VOCABULARY_IDS.indexOf(it.vocabId) } else words
    }
}
