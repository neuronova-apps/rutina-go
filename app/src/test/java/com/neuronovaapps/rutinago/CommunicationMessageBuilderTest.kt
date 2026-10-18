package com.neuronovaapps.rutinago

import com.neuronovaapps.rutinago.data.communication.CommunicationStateCodec
import com.neuronovaapps.rutinago.data.communication.LocalCommunicationCatalogRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationMessageBuilder
import com.neuronovaapps.rutinago.domain.communication.CommunicationMessageItem
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class CommunicationMessageBuilderTest {
    private val root = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
        .first { File(it, "app/src/main/assets/communication/communication_catalog_v1.json").isFile }
    private val asset = File(root, "app/src/main/assets/${LocalCommunicationCatalogRepository.ASSET_PATH}")
    private var catalogReads = 0
    private val catalog = LocalCommunicationCatalogRepository { catalogReads++; asset.readText() }
    private val builder = CommunicationMessageBuilder(catalog)

    private fun message(vararg ids: String) = ids.fold(builder.emptyState(), builder::add)

    @Test fun emptyMessageAndDisabledOperationsAreSafe() {
        val empty = builder.emptyState()
        assertTrue(empty.messageItems.isEmpty())
        assertSame(empty, builder.removeLast(empty))
        assertSame(empty, builder.clear(empty))
        assertSame(empty, builder.undoClear(empty))
    }

    @Test fun singleWordKeepsItsRealIdAndExactEditorialText() {
        val state = message("VOC_015")
        assertEquals(listOf(CommunicationMessageItem(1, "VOC_015", "Agua")), state.messageItems)
    }

    @Test fun multipleWordsKeepSelectionOrderAcrossCategoriesWithoutGrammarChanges() {
        val state = message("VOC_001", "VOC_006", "VOC_015", "VOC_126", "VOC_073")
        assertEquals(listOf("Yo", "Quiero", "Agua", "Me", "Duele"), state.messageItems.map { it.text })
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), state.messageItems.map { it.selectionId })
    }

    @Test fun incompleteAndUnusualSequencesAreAcceptedWithoutReordering() {
        listOf(arrayOf("VOC_015"), arrayOf("VOC_053"), arrayOf("VOC_053", "VOC_006"),
            arrayOf("VOC_064", "VOC_006"), arrayOf("VOC_015", "VOC_001", "VOC_015")).forEach { ids ->
            assertEquals(ids.toList(), message(*ids).messageItems.map { it.vocabularyId })
        }
    }

    @Test fun repeatedWordsHaveDistinctOccurrenceIdsAndAreNeverDeduplicated() {
        val state = message("VOC_064", "VOC_064", "VOC_015")
        assertEquals(listOf("Más", "Más", "Agua"), state.messageItems.map { it.text })
        assertEquals(3, state.messageItems.map { it.selectionId }.toSet().size)
        assertEquals(listOf("Más", "Más"), builder.removeLast(state).messageItems.map { it.text })
    }

    @Test fun deleteRemovesOnlyTheLastOccurrenceAndDoesNotReuseItsIdentity() {
        val state = message("VOC_001", "VOC_006", "VOC_015")
        val shorter = builder.removeLast(state)
        assertEquals(state.messageItems.take(2), shorter.messageItems)
        val appended = builder.add(shorter, "VOC_015")
        assertEquals(4L, appended.messageItems.last().selectionId)
        assertEquals(3, state.messageItems.size)
    }

    @Test fun repeatedDeletionRestoresTheOriginalEmptyState() {
        var state = message("VOC_064", "VOC_064")
        repeat(3) { state = builder.removeLast(state) }
        assertTrue(state.messageItems.isEmpty())
        assertTrue(state.clearedItems.isEmpty())
    }

    @Test fun clearIsImmediatelyReversibleEvenForLongMessages() {
        val state = (1..250).fold(builder.emptyState()) { current, _ -> builder.add(current, "VOC_006") }
        val cleared = builder.clear(state)
        assertTrue(cleared.messageItems.isEmpty())
        assertEquals(state.messageItems, cleared.clearedItems)
        assertEquals(state.messageItems, builder.undoClear(cleared).messageItems)
        assertTrue(builder.undoClear(cleared).clearedItems.isEmpty())
    }

    @Test fun newSelectionAfterClearStartsANewMessageAndInvalidatesUndo() {
        val cleared = builder.clear(message("VOC_001", "VOC_006"))
        val state = builder.add(cleared, "VOC_015")
        assertEquals(listOf("Agua"), state.messageItems.map { it.text })
        assertTrue(state.clearedItems.isEmpty())
        assertSame(state, builder.undoClear(state))
    }

    @Test fun categoryChangesKeepMessageUndoAndQuickAccessAvailable() {
        var state = message("VOC_006")
        catalog.categories.forEach { category ->
            state = builder.selectCategory(state, category.id)
            assertEquals(listOf("Quiero"), state.messageItems.map { it.text })
            assertEquals(catalog.vocabularyForCategory(category.id), builder.visibleVocabulary(state))
            assertEquals(6, builder.coreVocabulary.size)
        }
        val cleared = builder.selectCategory(builder.clear(state), "CAT_003")
        assertEquals(listOf("Quiero"), builder.undoClear(cleared).messageItems.map { it.text })
        val all = builder.selectCategory(cleared, null)
        assertEquals(132, builder.visibleVocabulary(all).size)
    }

    @Test fun coreReferencesAreExactUniqueCatalogObjectsAndDoNotCreateRecords() {
        assertEquals(listOf("VOC_001", "VOC_006", "VOC_007", "VOC_063", "VOC_053", "VOC_014"),
            builder.coreVocabulary.map { it.vocabId })
        assertEquals(listOf("Yo", "Quiero", "Necesito", "Sí", "No", "Ayuda"), builder.coreVocabulary.map { it.text })
        builder.coreVocabulary.forEach { core -> assertSame(catalog.vocabulary.single { it.vocabId == core.vocabId }, core) }
        assertEquals(6, builder.coreVocabulary.map { it.vocabId }.toSet().size)
        assertEquals(132, catalog.vocabulary.size)
        assertEquals(18, catalog.categories.size)
    }

    @Test fun communicationSessionsHaveNoRutinaGoModeOrMutableSharedMessage() {
        val other = CommunicationMessageBuilder(catalog)
        val state = message("VOC_015")
        assertTrue(other.emptyState().messageItems.isEmpty())
        assertEquals("Agua", state.messageItems.single().text)
        assertEquals(132, other.visibleVocabulary(other.emptyState()).size)
        assertThrows(UnsupportedOperationException::class.java) { (state.messageItems as MutableList).clear() }
        assertThrows(UnsupportedOperationException::class.java) { (builder.coreVocabulary as MutableList).clear() }
    }

    @Test fun snapshotRestoresCategoryRepeatedWordsIdsAndNextOccurrence() {
        val original = builder.selectCategory(message("VOC_006", "VOC_015", "VOC_015"), "CAT_003")
        val restored = CommunicationStateCodec.decode(CommunicationStateCodec.encode(original), builder)
        assertEquals(original.selectedCategoryId, restored.selectedCategoryId)
        assertEquals(original.messageItems, restored.messageItems)
        assertEquals(4L, builder.add(restored, "VOC_001").messageItems.last().selectionId)
    }

    @Test fun snapshotAlsoRestoresUndoWithoutPermanentStorage() {
        val beforeClear = message("VOC_064", "VOC_015")
        val restored = CommunicationStateCodec.decode(CommunicationStateCodec.encode(builder.clear(beforeClear)), builder)
        assertTrue(restored.messageItems.isEmpty())
        assertEquals(beforeClear.messageItems, builder.undoClear(restored).messageItems)
        val empty = CommunicationStateCodec.decode(CommunicationStateCodec.encode(builder.emptyState()), builder)
        assertTrue(empty.messageItems.isEmpty())
    }

    @Test fun unknownIdsCorruptSnapshotsAndEditedTextsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { message("VOC_999") }
        assertThrows(IllegalArgumentException::class.java) { builder.selectCategory(builder.emptyState(), "CAT_999") }
        val snapshot = CommunicationStateCodec.encode(message("VOC_006"))
        listOf(snapshot.replace("Quiero", "quiero"), snapshot.replace("VOC_006", "VOC_999"),
            snapshot.replace("\"version\":1", "\"version\":2")).forEach { invalid ->
            assertThrows(RuntimeException::class.java) { CommunicationStateCodec.decode(invalid, builder) }
        }
        val item = CommunicationMessageItem(1, "VOC_006", "Quiero")
        assertThrows(IllegalArgumentException::class.java) { builder.restore(null, listOf(item, item), emptyList(), 2) }
        assertThrows(IllegalArgumentException::class.java) { builder.restore(null, listOf(item), emptyList(), 1) }
    }

    @Test fun everyOperationUsesOnlyTheLoadedCatalogWithoutReloadingOrChangingTheBank() {
        val originalBytes = asset.readBytes()
        val originalRecords = catalog.vocabulary.toList()
        var state = builder.emptyState()
        repeat(20) {
            state = builder.add(state, builder.coreVocabulary[it % 6].vocabId)
            state = builder.selectCategory(state, catalog.categories[it % 18].id)
            builder.visibleVocabulary(state)
        }
        state = builder.undoClear(builder.clear(builder.removeLast(state)))
        CommunicationStateCodec.decode(CommunicationStateCodec.encode(state), builder)
        assertEquals(1, catalogReads)
        assertEquals(originalRecords, catalog.vocabulary)
        assertArrayEquals(originalBytes, asset.readBytes())
    }

    @Test fun communicationHasNoNetworkOrDatabaseAndSpeechIsConfinedToItsAndroidAdapter() {
        val base = File(root, "app/src/main/java/com/neuronovaapps/rutinago")
        val sources = listOf("domain/communication", "data/communication", "ui/screens/communication")
            .flatMap { File(base, it).walkTopDown().filter { file -> file.extension == "kt" }.toList() }
        val forbidden = Regex("\\b(OkHttpClient|ArasaacClient|ArasaacHttpRepository|Room|Supabase)\\b|import (java\\.net|okhttp3|androidx\\.room)")
        sources.forEach { source ->
            assertFalse("Communication boundary violation: ${source.name}", forbidden.containsMatchIn(source.readText()))
            if (source.name != "AndroidCommunicationSpeechEngine.kt") assertFalse("Speech outside adapter: ${source.name}",
                Regex("\\bTextToSpeech\\b|import android\\.speech").containsMatchIn(source.readText()))
        }
    }
}
