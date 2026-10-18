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

class CommunicationHomeVocabularyTest {
    @get:Rule val temporary = TemporaryFolder()
    private val root = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
        .first { File(it, "docs/communication/source_v1.json").isFile }
    private val asset = File(root, "app/src/main/assets/${LocalCommunicationCatalogRepository.ASSET_PATH}")
    private val catalog = LocalCommunicationCatalogRepository { asset.readText() }
    private val allIds = catalog.vocabulary.map { it.vocabId }.toSet()
    private val core = CommunicationMessageBuilder.CORE_VOCABULARY_IDS
    private val profiles = mutableSetOf("a", "b")
    private lateinit var scope: CoroutineScope
    private lateinit var store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var manager: CommunicationProfileSetupManager
    private fun open() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(temporary.root, "home.preferences_pb") })
        manager = CommunicationProfileSetupManager(CommunicationSetupPreferences(store), object : CommunicationProfileRegistry {
            override suspend fun exists(profileId: String) = profileId in profiles
            override suspend fun delete(profileId: String) { profiles.remove(profileId) }
        }, { catalog })
    }
    @Before fun prepare() = runBlocking {
        open()
        manager.saveVocabulary("a", allIds)
        manager.saveVocabulary("b", allIds)
    }
    @After fun close() = runBlocking { scope.coroutineContext[Job]!!.cancelAndJoin() }
    private suspend fun home(profile: String = "a") = manager.homeVocabularyForProfile(profile).first()
    private suspend fun words(profile: String = "a") = CommunicationHomeVocabulary.words(catalog, home(profile), manager.vocabularyForProfile(profile).first())

    @Test fun neverConfiguredShowsTheSixEssentialsInTheirExistingOrderWithoutWriting() = runBlocking {
        val before = store.data.first().asMap()
        assertNull(home())
        assertEquals(core, words().map { it.vocabId })
        assertEquals(before, store.data.first().asMap())
    }
    @Test fun initialProposalNeverRestoresWordsThatWereAlreadyHidden() = runBlocking {
        manager.saveVocabulary("a", allIds - "VOC_014" - "VOC_053")
        assertEquals(core.filterNot { it in setOf("VOC_014", "VOC_053") }, words().map { it.vocabId })
        manager.setHomeWord("a", "VOC_015", true)
        assertFalse("VOC_014" in requireNotNull(home()))
        assertFalse("VOC_053" in requireNotNull(home()))
    }
    @Test fun addingCreatesOneAssignmentWithRealCatalogObjects() = runBlocking {
        manager.setHomeWord("a", "VOC_015", true)
        assertEquals(core.toSet() + "VOC_015", home())
        assertSame(catalog.vocabulary.single { it.vocabId == "VOC_015" }, words().single { it.vocabId == "VOC_015" })
    }
    @Test fun removingAHomeWordKeepsItAvailableInCategories() = runBlocking {
        manager.setHomeWord("a", "VOC_001", false)
        assertFalse(words().any { it.vocabId == "VOC_001" })
        assertTrue("VOC_001" in requireNotNull(manager.vocabularyForProfile("a").first()))
        assertTrue(CommunicationPersonalVocabulary.visibleWords(catalog.vocabularyForCategory("CAT_001"),
            manager.vocabularyForProfile("a").first()).any { it.vocabId == "VOC_001" })
    }
    @Test fun repeatedAddsAndRemovesAreIdempotent() = runBlocking {
        repeat(3) { manager.setHomeWord("a", "VOC_015", true) }
        assertEquals(1, words().count { it.vocabId == "VOC_015" })
        repeat(3) { manager.setHomeWord("a", "VOC_015", false) }
        assertEquals(core.toSet(), home())
    }
    @Test fun customAndEmptySelectionsSurviveDataStoreReopening() = runBlocking {
        manager.setHomeWord("a", "VOC_015", true)
        core.forEach { manager.setHomeWord("b", it, false) }
        scope.coroutineContext[Job]!!.cancelAndJoin()
        open()
        assertEquals(core.toSet() + "VOC_015", home("a"))
        assertEquals(emptySet<String>(), home("b"))
        assertTrue(words("b").isEmpty())
    }
    @Test fun explicitlyEmptyNeverFallsBackToTheEssentials() = runBlocking {
        core.forEach { manager.setHomeWord("a", it, false) }
        assertNotNull(home())
        assertEquals(emptySet<String>(), home())
        assertTrue(words().isEmpty())
        manager.saveVocabulary("a", allIds - "VOC_014")
        manager.saveVocabulary("a", allIds)
        assertTrue(words().isEmpty())
    }
    @Test fun twoProfilesKeepDifferentSelectionsAndDoNotUseTheirNames() = runBlocking {
        manager.setHomeWord("a", "VOC_015", true)
        manager.setHomeWord("b", "VOC_001", false)
        assertTrue("VOC_015" in requireNotNull(home("a")))
        assertFalse("VOC_015" in requireNotNull(home("b")))
        assertTrue("VOC_001" in requireNotNull(home("a")))
        assertFalse("VOC_001" in requireNotNull(home("b")))
    }
    @Test fun hidingRetainsAssignmentButRemovesTheWordFromBothViews() = runBlocking {
        manager.setHomeWord("a", "VOC_015", true)
        manager.saveVocabulary("a", allIds - "VOC_015")
        assertTrue("VOC_015" in requireNotNull(home()))
        assertFalse(words().any { it.vocabId == "VOC_015" })
        assertFalse(CommunicationPersonalVocabulary.visibleWords(catalog.vocabulary, manager.vocabularyForProfile("a").first()).any { it.vocabId == "VOC_015" })
    }
    @Test fun recoveringAnAssignedHiddenWordRestoresItToHome() = runBlocking {
        manager.setHomeWord("a", "VOC_015", true)
        val assignment = home()
        manager.saveVocabulary("a", allIds - "VOC_015")
        manager.saveVocabulary("a", allIds)
        assertEquals(assignment, home())
        assertTrue(words().any { it.vocabId == "VOC_015" })
    }
    @Test fun removingAnAssignmentWhileHiddenPreventsItReappearingOnRecovery() = runBlocking {
        manager.setHomeWord("a", "VOC_015", true)
        manager.saveVocabulary("a", allIds - "VOC_015")
        manager.setHomeWord("a", "VOC_015", false)
        manager.saveVocabulary("a", allIds)
        assertFalse(words().any { it.vocabId == "VOC_015" })
    }
    @Test fun addingUnavailableOrUnknownWordsFailsWithoutChangingTheSelection() = runBlocking {
        manager.saveVocabulary("a", allIds - "VOC_015")
        assertTrue(runCatching { manager.setHomeWord("a", "VOC_015", true) }.isFailure)
        assertTrue(runCatching { manager.setHomeWord("a", "VOC_999", true) }.isFailure)
        assertTrue(runCatching { manager.setHomeWord("a", "VOC_999", false) }.isFailure)
        assertNull(home())
    }
    @Test fun vocabularyMustBeConfiguredBeforeAnyAssignment() = runBlocking {
        val key = stringSetPreferencesKey("vocabulary_visible_v1:a")
        store.edit { it.remove(key) }
        assertTrue(runCatching { manager.setHomeWord("a", "VOC_015", true) }.isFailure)
        assertNull(home())
    }
    @Test fun homeEditsPreserveAllLegacyProfileAndPresentationRecords() = runBlocking {
        val setup = CommunicationProfileSetup("a", CommunicationAgeRange.YEARS_9_TO_12, OralCommunication.EMERGING_SPEECH)
        manager.saveSetup(setup)
        manager.setPresentationMode("a", CommunicationPresentationMode.FULL)
        val before = store.data.first().asMap()
        manager.setHomeWord("a", "VOC_015", true)
        before.forEach { (key, value) -> assertEquals(value, store.data.first().asMap()[key]) }
        assertEquals(setup, manager.setupForProfile("a").first())
    }
    @Test fun editingAgeSpeechAndPresentationNeverReplacesHomeAssignments() = runBlocking {
        manager.setHomeWord("a", "VOC_015", true)
        val selection = home()
        CommunicationAgeRange.entries.forEach { age ->
            manager.saveSetup(CommunicationProfileSetup("a", age, OralCommunication.NO_CURRENT_SPEECH))
        }
        CommunicationPresentationMode.entries.forEach { mode ->
            manager.setPresentationMode("a", mode)
            assertEquals(selection, home())
            assertTrue(words().any { it.vocabId == "VOC_015" })
        }
    }
    @Test fun homeAdministrationDoesNotMutateMessageOrderRepetitionCategoryOrUndo() = runBlocking {
        val builder = CommunicationMessageBuilder(catalog)
        val message = builder.selectCategory(listOf("VOC_001", "VOC_015", "VOC_015").fold(builder.emptyState(), builder::add), "CAT_003")
        val before = CommunicationStateCodec.encode(message)
        val cleared = builder.clear(message)
        manager.setHomeWord("a", "VOC_015", true)
        manager.setHomeWord("a", "VOC_001", false)
        assertEquals(before, CommunicationStateCodec.encode(message))
        assertEquals(message.messageItems, builder.undoClear(cleared).messageItems)
        assertEquals(listOf("Yo", "Agua", "Agua"), message.messageItems.map { it.text })
        assertEquals(message.messageItems + CommunicationMessageItem(4, "VOC_015", "Agua"), builder.add(message, "VOC_015").messageItems)
    }
    @Test fun deletionCleansOnlyTheDeletedProfilesHomeAndBlocksFurtherSaves() = runBlocking {
        manager.setHomeWord("a", "VOC_015", true)
        manager.setHomeWord("b", "VOC_083", true)
        val other = home("b")
        manager.deleteProfile("a")
        assertNull(home("a"))
        assertEquals(other, home("b"))
        assertTrue(runCatching { manager.setHomeWord("a", "VOC_015", true) }.isFailure)
    }
    @Test fun concurrentAddsNeverLoseOneAnothersAssignments() = runBlocking {
        coroutineScope { (15..30).map { n -> launch { manager.setHomeWord("a", "VOC_${n.toString().padStart(3, '0')}", true) } }.joinAll() }
        assertTrue(requireNotNull(home()).containsAll((15..30).map { "VOC_${it.toString().padStart(3, '0')}" }))
    }
    @Test fun concurrentDeletionAndAssignmentLeaveNoOrphan() = runBlocking {
        coroutineScope {
            launch { runCatching { manager.setHomeWord("a", "VOC_015", true) } }
            launch { manager.deleteProfile("a") }
        }
        assertNull(home())
    }
    @Test fun unknownStoredReferencesAreNeverDisplayedAndOnlyValidIdsAreSaved() = runBlocking {
        val key = stringSetPreferencesKey("home_vocabulary_v1:a")
        store.edit { it[key] = setOf("VOC_015", "VOC_999") }
        assertEquals(listOf("VOC_015"), words().map { it.vocabId })
        assertEquals(setOf("VOC_015", "VOC_999"), store.data.first()[key])
        manager.setHomeWord("a", "VOC_083", true)
        assertEquals(setOf("VOC_015", "VOC_083"), home())
    }
    @Test fun all132WordsMayBeAssignedWithoutAnAgeDependentLimitAndBankRemainsIntact() = runBlocking {
        val bytes = asset.readBytes()
        allIds.forEach { manager.setHomeWord("a", it, true) }
        assertEquals(132, words().size)
        assertEquals(catalog.vocabulary, words())
        assertEquals(18, catalog.categories.size)
        assertArrayEquals(bytes, asset.readBytes())
    }
    @Test fun organizationBackReturnsToItsOriginWithoutAddingADestination() {
        listOf(CommunicationDestination.HOME, CommunicationDestination.MY_VOCABULARY).forEach {
            val original = CommunicationNavigationState(it)
            assertEquals(original, original.organizeHome().back())
            assertEquals(CommunicationDestination.MY_VOCABULARY, original.organizeHome().destination)
            assertNull(original.organizeHome().back()?.back())
        }
        assertEquals(3, CommunicationDestination.entries.size)
    }
}
