package com.neuronovaapps.rutinago

import com.neuronovaapps.rutinago.data.communication.CommunicationStateCodec
import com.neuronovaapps.rutinago.data.communication.LocalCommunicationCatalogRepository
import com.neuronovaapps.rutinago.domain.communication.*
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class CommunicationBottomNavigationTest {
    private val root = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
        .first { File(it, "app/src/main/assets/communication/communication_catalog_v1.json").isFile }
    private val catalog = LocalCommunicationCatalogRepository {
        File(root, "app/src/main/assets/${LocalCommunicationCatalogRepository.ASSET_PATH}").readText()
    }
    private val builder = CommunicationMessageBuilder(catalog)

    @Test fun threeIndependentDestinationsHaveTheRequiredVisibleLabels() {
        assertEquals(listOf("HOME", "MY_VOCABULARY", "SETTINGS"), CommunicationDestination.entries.map { it.name })
        assertEquals(listOf("Inicio", "Mi vocabulario", "Configuración"), CommunicationDestination.entries.map { it.label })
    }
    @Test fun explorerIsSecondaryAndBackReturnsHomeBeforeTheSelector() {
        val explorer = CommunicationNavigationState().explore()
        assertEquals(CommunicationDestination.HOME, explorer.destination)
        assertTrue(explorer.exploring)
        assertEquals(CommunicationNavigationState(), explorer.back())
        assertNull(explorer.back()?.back())
        CommunicationDestination.entries.forEach { assertFalse(explorer.select(it).exploring) }
    }
    @Test fun organizerReturnsToItsOriginAndDoesNotReopenExplorer() {
        for (tab in listOf(CommunicationDestination.HOME, CommunicationDestination.MY_VOCABULARY)) {
            val origin = CommunicationNavigationState(tab)
            assertEquals(origin, origin.organizeHome().back())
        }
        assertFalse(CommunicationNavigationState().explore().organizeHome().exploring)
    }
    @Test fun oldCategorySnapshotRestoresAsSecondaryExplorerWithoutChangingStoredPreferences() {
        val restored = com.neuronovaapps.rutinago.ui.screens.communication.CommunicationNavigationSaver
            .restore(listOf("CATEGORIES", "false", ""))
        assertEquals(CommunicationNavigationState(exploring = true), restored)
        val current = com.neuronovaapps.rutinago.ui.screens.communication.CommunicationNavigationSaver
            .restore(listOf("HOME", "false", "", "true"))
        assertEquals(restored, current)
    }
    @Test fun aNewSessionStartsAtHomeWithoutAnEditor() {
        assertEquals(CommunicationDestination.HOME, CommunicationNavigationState().destination)
        assertFalse(CommunicationNavigationState().personalizing)
    }
    @Test fun everyTabCanBeSelectedFromEveryOtherTab() {
        CommunicationDestination.entries.forEach { origin ->
            CommunicationDestination.entries.forEach { target ->
                assertEquals(CommunicationNavigationState(target), CommunicationNavigationState(origin).select(target))
            }
        }
    }
    @Test fun systemBackFromEveryMainTabExitsToTheModuleSelector() {
        CommunicationDestination.entries.forEach { assertNull(CommunicationNavigationState(it).back()) }
    }
    @Test fun secondaryBackReturnsToItsOriginalTab() {
        CommunicationDestination.entries.forEach {
            val origin = CommunicationNavigationState(it)
            assertEquals(origin, origin.personalize().back())
            assertNull(origin.personalize().back()?.back())
        }
    }
    @Test fun switchingDestinationsCannotRewriteRepeatedMessageOrCategorySnapshot() {
        val message = builder.selectCategory(listOf("VOC_001", "VOC_006", "VOC_015", "VOC_015")
            .fold(builder.emptyState(), builder::add), "CAT_003")
        val encoded = CommunicationStateCodec.encode(message)
        var nav = CommunicationNavigationState()
        CommunicationDestination.entries.forEach {
            nav = nav.select(it)
            val restored = CommunicationStateCodec.decode(encoded, builder)
            assertEquals(message.messageItems, restored.messageItems)
            assertEquals("CAT_003", restored.selectedCategoryId)
            assertEquals(listOf("Yo", "Quiero", "Agua", "Agua"), restored.messageItems.map { word -> word.text })
        }
    }
    @Test fun categoryAndPersonalVocabularyFiltersIntersectWithoutRestrictingTheBank() {
        val state = builder.selectCategory(builder.emptyState(), "CAT_003")
        val visible = catalog.vocabulary.map { it.vocabId }.toSet() - "VOC_015"
        assertEquals(catalog.vocabularyForCategory("CAT_003").filter { it.vocabId != "VOC_015" },
            CommunicationPersonalVocabulary.visibleWords(builder.visibleVocabulary(state), visible))
        assertEquals(132, catalog.vocabulary.size)
        assertEquals(18, catalog.categories.size)
    }
    @Test fun hiddenEssentialWordsStayHiddenInBothPresentations() {
        val ids = CommunicationMessageBuilder.CORE_VOCABULARY_IDS.toSet() - "VOC_014" - "VOC_053"
        assertEquals(listOf("Yo", "Quiero", "Necesito", "Sí"),
            CommunicationPersonalVocabulary.visibleWords(builder.coreVocabulary, ids).map { it.text })
        assertTrue(CommunicationPersonalVocabulary.visibleWords(builder.coreVocabulary, emptySet()).isEmpty())
    }
    @Test fun hidingWordsNeverChangesAnExistingMessageOrItsUndo() {
        val message = builder.add(builder.add(builder.emptyState(), "VOC_014"), "VOC_014")
        val ids = catalog.vocabulary.map { it.vocabId }.toSet() - "VOC_014"
        CommunicationPersonalVocabulary.visibleWords(builder.coreVocabulary, ids)
        assertEquals(listOf("Ayuda", "Ayuda"), message.messageItems.map { it.text })
        assertEquals(message.messageItems, builder.undoClear(builder.clear(message)).messageItems)
    }
    @Test fun profileSnapshotsRemainIndependentIncludingRepetitionsAndCategories() {
        val a = builder.selectCategory(builder.add(builder.add(builder.emptyState(), "VOC_015"), "VOC_015"), "CAT_003")
        val b = builder.selectCategory(builder.add(builder.emptyState(), "VOC_014"), "CAT_004")
        val snapshots = mapOf("profile-a" to CommunicationStateCodec.encode(a), "profile-b" to CommunicationStateCodec.encode(b))
        assertEquals(a.messageItems, CommunicationStateCodec.decode(snapshots.getValue("profile-a"), builder).messageItems)
        assertEquals("CAT_004", CommunicationStateCodec.decode(snapshots.getValue("profile-b"), builder).selectedCategoryId)
        assertEquals(listOf("Ayuda"), CommunicationStateCodec.decode(snapshots.getValue("profile-b"), builder).messageItems.map { it.text })
    }
}
