package com.neuronovaapps.rutinago

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.neuronovaapps.rutinago.data.communication.*
import com.neuronovaapps.rutinago.domain.communication.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CommunicationPersonalVocabularyTest {
    @get:Rule val temporary = TemporaryFolder()
    private val root = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
        .first { File(it, "docs/communication/source_v1.json").isFile }
    private val catalog = LocalCommunicationCatalogRepository {
        File(root, "app/src/main/assets/${LocalCommunicationCatalogRepository.ASSET_PATH}").readText()
    }
    private val allIds = catalog.vocabulary.map { it.vocabId }.toSet()
    private val names = mutableMapOf("a" to "Mismo nombre", "b" to "Mismo nombre")
    private lateinit var scope: CoroutineScope
    private lateinit var store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var manager: CommunicationProfileSetupManager
    private fun open() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(temporary.root, "vocabulary.preferences_pb") })
        manager = CommunicationProfileSetupManager(CommunicationSetupPreferences(store), object : CommunicationProfileRegistry {
            override suspend fun exists(profileId: String) = profileId in names
            override suspend fun delete(profileId: String) { names.remove(profileId) }
        }, { catalog })
    }
    @Before fun prepare() = open()
    @After fun close() = runBlocking { scope.coroutineContext[Job]!!.cancelAndJoin() }

    @Test fun unconfiguredEmptyAndCompleteAreDifferentAndSurviveReopening() = runBlocking {
        assertNull(manager.vocabularyForProfile("a").first())
        manager.saveVocabulary("a", emptySet())
        manager.saveVocabulary("b", allIds)
        scope.coroutineContext[Job]!!.cancelAndJoin()
        open()
        assertEquals(emptySet<String>(), manager.vocabularyForProfile("a").first())
        assertEquals(allIds, manager.vocabularyForProfile("b").first())
    }

    @Test fun sameNamesRenameAndEditsNeverShareSelections() = runBlocking {
        manager.saveVocabulary("a", setOf("VOC_001"))
        manager.saveVocabulary("b", allIds)
        names["a"] = "Renombrado"
        manager.saveVocabulary("a", setOf("VOC_014", "VOC_053"))
        assertEquals(setOf("VOC_014", "VOC_053"), manager.vocabularyForProfile("a").first())
        assertEquals(allIds, manager.vocabularyForProfile("b").first())
    }

    @Test fun legacySetupAndPresentationArePreservedByVocabularyEdits() = runBlocking {
        val setup = CommunicationProfileSetup("a", CommunicationAgeRange.YEARS_9_TO_12, OralCommunication.EMERGING_SPEECH)
        manager.saveSetup(setup)
        manager.setPresentationMode("a", CommunicationPresentationMode.FULL)
        val before = store.data.first().asMap()
        assertNull(manager.vocabularyForProfile("a").first())
        manager.saveVocabulary("a", emptySet())
        before.forEach { (key, value) -> assertEquals(value, store.data.first().asMap()[key]) }
        manager.saveSetup(setup.copy(ageRange = CommunicationAgeRange.YEARS_3_TO_5))
        manager.setPresentationMode("a", CommunicationPresentationMode.GUIDED)
        assertEquals(emptySet<String>(), manager.vocabularyForProfile("a").first())
    }

    @Test fun unknownIdsAndDeletedProfilesCannotBeSaved() = runBlocking {
        manager.saveVocabulary("a", allIds)
        assertTrue(runCatching { manager.saveVocabulary("a", setOf("VOC_999")) }.isFailure)
        assertEquals(allIds, manager.vocabularyForProfile("a").first())
        manager.deleteProfile("a")
        assertTrue(runCatching { manager.saveVocabulary("a", allIds) }.isFailure)
        assertNull(manager.vocabularyForProfile("a").first())
    }

    @Test fun deletionCleansOnlyRequestedProfilesEntireCommunicationContext() = runBlocking {
        for (id in names.keys) {
            manager.saveSetup(CommunicationProfileSetup(id, CommunicationAgeRange.NOT_SPECIFIED, OralCommunication.NOT_SPECIFIED))
            manager.setPresentationMode(id, CommunicationPresentationMode.FULL)
            manager.saveVocabulary(id, allIds)
        }
        manager.deleteProfile("a")
        assertNull(manager.setupForProfile("a").first())
        assertNull(manager.presentationForProfile("a").first().selectedMode)
        assertNull(manager.vocabularyForProfile("a").first())
        assertEquals(allIds, manager.vocabularyForProfile("b").first())
        assertEquals(3, store.data.first().asMap().size)
    }

    @Test fun concurrentSaveAndDeletionCannotLeaveOrphanedVocabulary() = runBlocking {
        coroutineScope {
            launch { runCatching { manager.saveVocabulary("a", allIds) } }
            launch { manager.deleteProfile("a") }
        }
        assertNull(manager.vocabularyForProfile("a").first())
    }

    @Test fun storedUnknownReferencesDoNotExposeInventedWordsOrOverwriteOnRead() = runBlocking {
        val key = stringSetPreferencesKey("vocabulary_visible_v1:a")
        store.edit { it[key] = setOf("VOC_001", "VOC_999") }
        val ids = manager.vocabularyForProfile("a").first()
        assertEquals(listOf("VOC_001"), CommunicationPersonalVocabulary.visibleWords(catalog.vocabulary, ids).map { it.vocabId })
        assertEquals(setOf("VOC_001", "VOC_999"), store.data.first()[key])
    }

    @Test fun hidingAndRestoringPreservesSourceObjectsOrderAndAllCategories() {
        val ids = allIds - "VOC_014"
        val visible = CommunicationPersonalVocabulary.visibleWords(catalog.vocabulary, ids)
        assertEquals(131, visible.size)
        assertFalse(visible.any { it.vocabId == "VOC_014" })
        assertEquals(catalog.vocabulary, CommunicationPersonalVocabulary.visibleWords(catalog.vocabulary, ids + "VOC_014"))
        assertTrue(visible.all { word -> catalog.vocabulary.any { it === word } })
        assertEquals(18, catalog.categories.size)
        assertTrue(CommunicationPersonalVocabulary.visibleWords(catalog.vocabularyForCategory("CAT_004"), emptySet()).isEmpty())
    }

    @Test fun searchFindsHiddenWordsByAccentInsensitiveTextCategoryAndStableId() {
        val selected = allIds - "VOC_053"
        assertEquals(listOf("VOC_053"), CommunicationPersonalVocabulary.search(catalog, selected, VocabularyFilter.HIDDEN, null, "No").map { it.vocabId })
        assertEquals(listOf("VOC_063"), CommunicationPersonalVocabulary.search(catalog, allIds, VocabularyFilter.VISIBLE, null, "si").filter { it.text == "Sí" }.map { it.vocabId })
        assertEquals("VOC_132", CommunicationPersonalVocabulary.search(catalog, allIds, VocabularyFilter.ALL, null, "VOC_132").single().vocabId)
        val category = catalog.categories.first()
        assertEquals(catalog.vocabularyForCategory(category.id), CommunicationPersonalVocabulary.search(catalog, allIds, VocabularyFilter.ALL, category.id, category.name))
    }

    @Test fun suggestedAndImportantWordsAreVerifiedRealCatalogReferences() {
        val suggested = CommunicationPersonalVocabulary.suggestedIds(catalog)
        assertTrue(suggested.containsAll(CommunicationMessageBuilder.CORE_VOCABULARY_IDS))
        assertTrue(allIds.containsAll(suggested))
        listOf("VOC_063", "VOC_053", "VOC_014", "VOC_049", "VOC_073", "VOC_091", "VOC_092", "VOC_070").forEach {
            assertTrue(CommunicationPersonalVocabulary.needsHideWarning(it))
        }
        assertFalse(CommunicationPersonalVocabulary.needsHideWarning("VOC_001"))
    }

    @Test fun hiddenQuickAccessWordsAndBothViewsShareSameSelectionWithoutRewritingMessageOrUndo() {
        val builder = CommunicationMessageBuilder(catalog)
        val message = listOf("VOC_001", "VOC_014", "VOC_014").fold(builder.emptyState(), builder::add)
        val cleared = builder.clear(message)
        val snapshot = CommunicationStateCodec.encode(message)
        val ids = allIds - "VOC_014"
        CommunicationPresentationMode.entries.forEach {
            assertFalse(CommunicationPersonalVocabulary.visibleWords(builder.coreVocabulary, ids).any { it.vocabId == "VOC_014" })
            assertEquals(131, CommunicationPersonalVocabulary.visibleWords(builder.visibleVocabulary(message), ids).size)
            assertEquals(snapshot, CommunicationStateCodec.encode(message))
            assertEquals(message.messageItems, builder.undoClear(cleared).messageItems)
            assertEquals(message.messageItems, CommunicationStateCodec.decode(snapshot, builder).messageItems)
        }
    }
}
