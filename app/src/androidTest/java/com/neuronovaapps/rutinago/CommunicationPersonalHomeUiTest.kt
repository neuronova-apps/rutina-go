package com.neuronovaapps.rutinago

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.neuronovaapps.rutinago.data.communication.LocalCommunicationCatalogRepository
import com.neuronovaapps.rutinago.data.preferences.UserPreferences
import com.neuronovaapps.rutinago.data.preferences.UserPreferencesRepository
import com.neuronovaapps.rutinago.domain.communication.*
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CommunicationPersonalHomeUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var repository: UserPreferencesRepository
    private lateinit var original: UserPreferences
    private val a = "personal-home-a-${UUID.randomUUID()}"
    private val b = "personal-home-b-${UUID.randomUUID()}"
    private lateinit var allIds: Set<String>
    @Before fun prepare() = runBlocking {
        repository = UserPreferencesRepository(rule.activity)
        original = repository.preferences.first()
        val catalog = LocalCommunicationCatalogRepository {
            rule.activity.assets.open(LocalCommunicationCatalogRepository.ASSET_PATH).bufferedReader().use { it.readText() }
        }
        allIds = catalog.vocabulary.map { it.vocabId }.toSet()
        for (id in listOf(a, b)) {
            repository.createProfile(if (id == a) "Inicio A" else "Inicio B", id)
            repository.saveSetup(CommunicationProfileSetup(id, CommunicationAgeRange.YEARS_9_TO_12, OralCommunication.EMERGING_SPEECH))
            repository.saveVocabulary(id, allIds)
        }
        repository.selectProfile(a)
    }
    @After fun cleanup(): Unit = runBlocking {
        repository.deleteProfile(a); repository.deleteProfile(b)
        original.activeProfile?.id?.let { repository.selectProfile(it) }
    }
    private fun open() {
        rule.onNodeWithText("Comenzar").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("module-selector").fetchSemanticsNodes().isNotEmpty() }
        enter()
    }
    private fun enter() { rule.onNodeWithTag("module-communication").performClick(); rule.awaitCommunicationCatalog() }
    private fun tab(name: String) { rule.onNodeWithTag("communication-destination-$name").performClick().assertIsSelected() }
    private fun systemBack() { rule.waitForIdle(); rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }
    private fun awaitAssigned(id: String, value: Boolean) {
        rule.waitUntil(10_000) { runBlocking {
            repository.homeVocabularyForProfile(a).first()?.let { (id in it) == value } == true
        } }
        rule.waitForIdle()
    }
    private fun organizeFromHome() {
        rule.onNodeWithTag("communication-home").performScrollToKey("organize-home")
        rule.onNodeWithTag("communication-organize-home").performClick()
        rule.onNodeWithTag("home-vocabulary-organization").assertIsDisplayed()
        rule.onNodeWithTag("communication-bottom-bar").assertDoesNotExist()
    }
    private fun assertMessage(vararg words: String) {
        rule.scrollCommunicationTo("message")
        words.forEachIndexed { i, word -> rule.onNodeWithTag("communication-message-item-${i + 1}").assertTextEquals(word) }
        rule.onNodeWithTag("communication-message-item-${words.size + 1}").assertDoesNotExist()
    }
    private fun searchAdmin(id: String) {
        tab("MY_VOCABULARY")
        rule.onNodeWithTag("personal-vocabulary").performScrollToKey("search")
        rule.onNodeWithTag("vocabulary-search").performTextReplacement(id)
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("personal-vocabulary").performScrollToKey(id)
    }
    @Test fun useAddRemoveAndOrganizeAreSeparateAndKeepTheSameMessage() {
        open()
        CommunicationMessageBuilder.CORE_VOCABULARY_IDS.forEach { id ->
            rule.onNodeWithTag("communication-home").performScrollToKey("core-$id")
            rule.onNodeWithTag("communication-core-$id").assertIsDisplayed().assertHasClickAction()
        }
        runBlocking { assertNull(repository.homeVocabularyForProfile(a).first()) }
        rule.selectCommunicationCategory("CAT_003")
        rule.onNodeWithTag("communication-category-grid").performScrollToKey("VOC_015")
        rule.onNodeWithTag("communication-word-VOC_015").performClick()
        rule.onNodeWithTag("communication-compact-message").assertTextEquals("Mensaje: Agua")
        rule.onNodeWithTag("home-assignment-VOC_015").assertDoesNotExist()
        searchAdmin("VOC_015")
        rule.onNodeWithTag("home-assignment-VOC_015").performScrollTo().performClick()
        awaitAssigned("VOC_015", true)
        rule.onNodeWithTag("home-assignment-VOC_015").assertContentDescriptionEquals("Quitar de Inicio: Agua")
        rule.onNodeWithTag("communication-destination-MY_VOCABULARY").assertIsSelected()
        tab("HOME")
        rule.onNodeWithTag("communication-home").performScrollToKey("core-VOC_015")
        rule.onNodeWithTag("communication-core-VOC_015").performClick()
        assertMessage("Agua", "Agua")
        organizeFromHome()
        val list = rule.onNodeWithTag("home-vocabulary-organization")
        list.performScrollToKey("VOC_015")
        rule.onNodeWithTag("home-assignment-VOC_015").performClick()
        awaitAssigned("VOC_015", false)
        systemBack()
        rule.onNodeWithTag("communication-destination-HOME").assertIsSelected()
        rule.onNodeWithTag("communication-core-VOC_015").assertDoesNotExist()
        assertMessage("Agua", "Agua")
        rule.selectCommunicationCategory("CAT_003")
        rule.onNodeWithTag("communication-category-grid").performScrollToKey("VOC_015")
        rule.onNodeWithTag("communication-word-VOC_015").assertIsDisplayed()
        runBlocking { assertTrue("VOC_015" in requireNotNull(repository.vocabularyForProfile(a).first())) }
        tab("MY_VOCABULARY")
        rule.onNodeWithTag("personal-vocabulary").performScrollToKey("home-words")
        rule.onNodeWithTag("vocabulary-organize-home").performScrollTo().performClick()
        rule.onNodeWithTag("home-vocabulary-organization").assertIsDisplayed()
        systemBack()
        rule.onNodeWithTag("communication-destination-MY_VOCABULARY").assertIsSelected()
    }
    @Test fun hideAndRecoverRetainsTheAssignmentAndNeverChangesTheMessage() {
        runBlocking { repository.setHomeWord(a, "VOC_015", true) }
        open()
        rule.onNodeWithTag("communication-home").performScrollToKey("core-VOC_015")
        rule.onNodeWithTag("communication-core-VOC_015").performClick()
        searchAdmin("VOC_015")
        rule.onNodeWithTag("vocabulary-toggle-VOC_015").performScrollTo().assertIsEnabled().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("vocabulary-confirm-hide").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("vocabulary-confirm-hide").performClick()
        rule.waitUntil(10_000) { runBlocking { "VOC_015" !in requireNotNull(repository.vocabularyForProfile(a).first()) } }
        tab("HOME")
        rule.onNodeWithTag("communication-core-VOC_015").assertDoesNotExist()
        assertMessage("Agua")
        rule.selectCommunicationCategory("CAT_003")
        rule.onNodeWithTag("communication-word-VOC_015").assertDoesNotExist()
        runBlocking { assertTrue("VOC_015" in requireNotNull(repository.homeVocabularyForProfile(a).first())) }
        searchAdmin("VOC_015")
        rule.onNodeWithTag("vocabulary-toggle-VOC_015").performScrollTo().assertIsEnabled().performClick()
        rule.waitUntil(10_000) { runBlocking { "VOC_015" in requireNotNull(repository.vocabularyForProfile(a).first()) } }
        tab("HOME")
        rule.onNodeWithTag("communication-home").performScrollToKey("core-VOC_015")
        rule.onNodeWithTag("communication-core-VOC_015").assertIsDisplayed()
        assertMessage("Agua")
    }
    @Test fun emptySelectionAndSecondaryNavigationSurviveRecreationAndAllowAddingWords() {
        open()
        rule.scrollCommunicationTo("core-VOC_006"); rule.onNodeWithTag("communication-core-VOC_006").performClick()
        organizeFromHome()
        CommunicationMessageBuilder.CORE_VOCABULARY_IDS.forEach { id ->
            rule.onNodeWithTag("home-vocabulary-organization").performScrollToKey(id)
            rule.onNodeWithTag("home-assignment-$id").performClick()
            awaitAssigned(id, false)
        }
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("home-vocabulary-organization").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("home-vocabulary-organization").performScrollToKey("back")
        rule.onNodeWithTag("home-organization-back").performClick()
        rule.onNodeWithTag("communication-destination-HOME").assertIsSelected()
        rule.onNodeWithTag("communication-home").performScrollToKey("empty-vocabulary")
        rule.onNodeWithText("Agregar palabras a Inicio").performScrollTo().performClick()
        rule.onNodeWithTag("home-vocabulary-organization").performScrollToKey("filter")
        rule.onNodeWithTag("home-organization-available").assertIsSelected()
        rule.onNodeWithTag("home-vocabulary-organization").performScrollToKey("search")
        rule.onNodeWithTag("home-organization-search").performTextReplacement("VOC_015")
        rule.onNodeWithTag("home-vocabulary-organization").performScrollToKey("VOC_015")
        rule.onNodeWithTag("home-assignment-VOC_015").performClick()
        awaitAssigned("VOC_015", true)
        systemBack()
        assertMessage("Quiero")
        rule.activityRule.scenario.recreate(); rule.awaitCommunicationCatalog()
        rule.onNodeWithTag("communication-home").performScrollToKey("core-VOC_015")
        rule.onNodeWithTag("communication-core-VOC_015").assertIsDisplayed()
        runBlocking { assertEquals(setOf("VOC_015"), repository.homeVocabularyForProfile(a).first()) }
    }
    @Test fun bothViewsAndProfilesKeepTheirOwnHomeSelectionAndOriginalNavigation() {
        runBlocking {
            repository.setHomeWord(a, "VOC_015", true)
            repository.saveVocabulary(b, setOf("VOC_014", "VOC_053"))
        }
        open()
        rule.scrollCommunicationTo("core-VOC_015"); rule.onNodeWithTag("communication-core-VOC_015").performClick()
        CommunicationPresentationMode.entries.forEach { mode ->
            rule.scrollCommunicationTo("presentation")
            rule.onNodeWithTag("communication-presentation-${mode.name}").performScrollTo().performClick()
            rule.waitUntil(10_000) { runBlocking { repository.presentationForProfile(a).first().selectedMode == mode } }
            rule.scrollCommunicationTo("core-VOC_015"); rule.onNodeWithTag("communication-core-VOC_015").assertIsDisplayed()
            assertMessage("Agua")
        }
        systemBack()
        runBlocking { repository.selectProfile(b) }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Perfil activo: Inicio B").fetchSemanticsNodes().isNotEmpty() }
        enter()
        rule.onNodeWithTag("communication-home").performScrollToKey("message")
        rule.onNodeWithText("Tu mensaje aparecerá aquí").assertIsDisplayed()
        rule.onNodeWithTag("communication-core-VOC_015").assertDoesNotExist()
        rule.onNodeWithTag("communication-home").performScrollToKey("core-VOC_014")
        rule.onNodeWithTag("communication-core-VOC_014").assertIsDisplayed()
        systemBack()
        runBlocking { repository.selectProfile(a) }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Perfil activo: Inicio A").fetchSemanticsNodes().isNotEmpty() }
        enter(); assertMessage("Agua")
        CommunicationDestination.entries.forEach {
            tab(it.name); systemBack(); rule.onNodeWithTag("module-selector").assertIsDisplayed(); enter()
        }
        systemBack(); rule.enterRutinaGoModule()
        listOf("HOME", "ROUTINES", "CREATE", "PROGRESS", "SETTINGS").forEach {
            rule.onNodeWithTag("destination-$it").performClick().assertIsSelected()
        }
        rule.onNodeWithTag("communication-bottom-bar").assertDoesNotExist()
    }
}
