package com.neuronovaapps.rutinago.domain.communication

import java.util.Collections

/** selectionId identifies an occurrence, so repeated vocabulary IDs remain distinct. */
data class CommunicationMessageItem(
    val selectionId: Long,
    val vocabularyId: String,
    val text: String,
)

/** Session state only. Lists are defensively copied and cannot be mutated through a cast. */
class CommunicationUiState internal constructor(
    val selectedCategoryId: String? = null,
    messageItems: List<CommunicationMessageItem> = emptyList(),
    clearedItems: List<CommunicationMessageItem> = emptyList(),
    val nextSelectionId: Long = 1,
) {
    val messageItems: List<CommunicationMessageItem> = immutable(messageItems)
    val clearedItems: List<CommunicationMessageItem> = immutable(clearedItems)

    private fun <T> immutable(items: List<T>): List<T> =
        Collections.unmodifiableList(ArrayList(items))
}

/** Pure message operations depend only on the read-only communication catalog. */
class CommunicationMessageBuilder(private val catalog: CommunicationCatalogRepository) {
    private val vocabularyById = catalog.vocabulary.associateBy { it.vocabId }
    val coreVocabulary: List<CommunicationVocabularyItem> = Collections.unmodifiableList(
        CORE_VOCABULARY_IDS.map { id -> requireNotNull(vocabularyById[id]) { "Missing core vocabulary: $id" } },
    )

    fun emptyState() = CommunicationUiState()

    fun add(state: CommunicationUiState, vocabularyId: String): CommunicationUiState {
        val word = requireNotNull(vocabularyById[vocabularyId]) { "Unknown vocabulary ID: $vocabularyId" }
        return CommunicationUiState(
            selectedCategoryId = state.selectedCategoryId,
            messageItems = state.messageItems + CommunicationMessageItem(state.nextSelectionId, word.vocabId, word.text),
            nextSelectionId = Math.addExact(state.nextSelectionId, 1),
        )
    }

    fun removeLast(state: CommunicationUiState): CommunicationUiState =
        if (state.messageItems.isEmpty()) state else CommunicationUiState(
            selectedCategoryId = state.selectedCategoryId,
            messageItems = state.messageItems.dropLast(1),
            nextSelectionId = state.nextSelectionId,
        )

    fun clear(state: CommunicationUiState): CommunicationUiState =
        if (state.messageItems.isEmpty()) state else CommunicationUiState(
            selectedCategoryId = state.selectedCategoryId,
            clearedItems = state.messageItems,
            nextSelectionId = state.nextSelectionId,
        )

    fun undoClear(state: CommunicationUiState): CommunicationUiState =
        if (state.clearedItems.isEmpty()) state else CommunicationUiState(
            selectedCategoryId = state.selectedCategoryId,
            messageItems = state.clearedItems,
            nextSelectionId = state.nextSelectionId,
        )

    fun selectCategory(state: CommunicationUiState, categoryId: String?): CommunicationUiState {
        require(categoryId == null || catalog.categories.any { it.id == categoryId && it.active })
        return CommunicationUiState(categoryId, state.messageItems, state.clearedItems, state.nextSelectionId)
    }

    fun visibleVocabulary(state: CommunicationUiState): List<CommunicationVocabularyItem> =
        catalog.vocabularyForCategory(state.selectedCategoryId)

    /** Restore a transient snapshot without guessing missing IDs or changing stored text. */
    fun restore(
        categoryId: String?,
        messageItems: List<CommunicationMessageItem>,
        clearedItems: List<CommunicationMessageItem>,
        nextSelectionId: Long,
    ): CommunicationUiState {
        require(categoryId == null || catalog.categories.any { it.id == categoryId && it.active })
        require(messageItems.isEmpty() || clearedItems.isEmpty()) { "Undo cannot overwrite a new message" }
        val items = messageItems + clearedItems
        require(items.all { it.selectionId > 0 && vocabularyById[it.vocabularyId]?.text == it.text })
        require(items.map { it.selectionId }.toSet().size == items.size)
        require(items.zipWithNext().all { (a, b) -> a.selectionId < b.selectionId })
        require(nextSelectionId > (items.maxOfOrNull { it.selectionId } ?: 0))
        return CommunicationUiState(categoryId, messageItems, clearedItems, nextSelectionId)
    }

    companion object {
        // Verified against communication_catalog_v1.json; these are presentation references.
        val CORE_VOCABULARY_IDS: List<String> = Collections.unmodifiableList(
            listOf("VOC_001", "VOC_006", "VOC_007", "VOC_063", "VOC_053", "VOC_014"),
        )
    }
}
