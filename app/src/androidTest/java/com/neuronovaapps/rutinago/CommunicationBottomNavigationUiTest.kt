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

class CommunicationBottomNavigationUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var repository: UserPreferencesRepository
    private lateinit var original: UserPreferences
    private val a = "bottom-a-${UUID.randomUUID()}"
    private val b = "bottom-b-${UUID.randomUUID()}"
    @Before fun prepare() = runBlocking {
        repository = UserPreferencesRepository(rule.activity)
        original = repository.preferences.first()
        val catalog = LocalCommunicationCatalogRepository {
            rule.activity.assets.open(LocalCommunicationCatalogRepository.ASSET_PATH).bufferedReader().use { it.readText() }
        }
        for (id in listOf(a, b)) {
            repository.createProfile(if (id == a) "Comunicación A" else "Comunicación B", id)
            repository.saveSetup(CommunicationProfileSetup(id, CommunicationAgeRange.YEARS_9_TO_12, OralCommunication.EMERGING_SPEECH))
            repository.saveVocabulary(id, catalog.vocabulary.map { it.vocabId }.toSet())
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
    private fun tab(name: String) {
        rule.onNodeWithTag("communication-destination-$name").performClick().assertIsSelected()
        rule.onNodeWithTag("communication-bottom-bar").assertIsDisplayed()
    }
    private fun back() { rule.waitForIdle(); rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }
    private fun assertMessage(vararg words: String) {
        rule.scrollCommunicationTo("message")
        words.forEachIndexed { index, word -> rule.onNodeWithTag("communication-message-item-${index + 1}").assertTextEquals(word) }
        rule.onAllNodes(hasTestTag("communication-message-item-${words.size + 1}")).assertCountEquals(0)
    }
    @Test fun threeTabsBackPolicyAndRutinaGoRemainIndependent() {
        open()
        rule.onNodeWithTag("communication-destination-HOME").assertIsSelected()
        rule.onNodeWithTag("communication-vocabulary-area").assertDoesNotExist()
        rule.onNodeWithTag("communication-categories").assertDoesNotExist()
        CommunicationDestination.entries.forEach { destination ->
            tab(destination.name)
            listOf("HOME", "ROUTINES", "CREATE", "PROGRESS", "SETTINGS").forEach {
                rule.onNodeWithTag("destination-$it").assertDoesNotExist()
            }
            back()
            rule.onNodeWithTag("module-selector").assertIsDisplayed()
            enter()
            rule.onNodeWithTag("communication-destination-HOME").assertIsSelected()
        }
        rule.scrollCommunicationTo("back")
        rule.onNodeWithTag("communication-back-to-modules").performClick()
        rule.enterRutinaGoModule()
        listOf("HOME", "ROUTINES", "CREATE", "PROGRESS", "SETTINGS").forEach {
            rule.onNodeWithTag("destination-$it").performClick().assertIsSelected()
        }
        rule.onNodeWithTag("communication-bottom-bar").assertDoesNotExist()
    }
    @Test fun sharedMessageRepetitionsCategoryAndAdministrationSurviveTabsAndRecreation() {
        open()
        rule.scrollCommunicationTo("core-VOC_001")
        rule.onNodeWithTag("communication-core-VOC_001").performClick()
        rule.selectCommunicationCategory("CAT_003")
        rule.scrollCommunicationTo("VOC_015")
        repeat(2) { rule.onNodeWithTag("communication-word-VOC_015").performClick() }
        rule.onNodeWithTag("communication-compact-message").assertTextEquals("Mensaje: Yo · Agua · Agua")
        rule.activityRule.scenario.recreate()
        rule.awaitCommunicationCatalog()
        rule.onNodeWithTag("communication-destination-CATEGORIES").assertDoesNotExist()
        rule.onNodeWithTag("communication-category-grid").assertIsDisplayed()
        rule.onNodeWithTag("communication-compact-message").assertTextEquals("Mensaje: Yo · Agua · Agua")
        rule.onNodeWithTag("communication-view-full-message").performClick()
        assertMessage("Yo", "Agua", "Agua")
        tab("MY_VOCABULARY")
        rule.onNodeWithTag("personal-vocabulary").performScrollToKey("search")
        rule.onNodeWithTag("vocabulary-search").performTextReplacement("VOC_001")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("personal-vocabulary").performScrollToKey("VOC_001")
        rule.onNodeWithTag("vocabulary-toggle-VOC_001").performScrollTo().performClick()
        rule.waitUntil(10_000) { runBlocking { "VOC_001" !in requireNotNull(repository.vocabularyForProfile(a).first()) } }
        tab("HOME")
        rule.scrollCommunicationTo("core-title")
        rule.onNodeWithTag("communication-core-VOC_001").assertDoesNotExist()
        assertMessage("Yo", "Agua", "Agua")
        tab("MY_VOCABULARY")
        rule.onNodeWithTag("personal-vocabulary").performScrollToKey("search")
        rule.onNodeWithTag("vocabulary-search").assertTextContains("VOC_001")
        rule.onNodeWithTag("personal-vocabulary").performScrollToKey("VOC_001")
        rule.onNodeWithTag("vocabulary-toggle-VOC_001").performScrollTo().performClick()
        rule.waitUntil(10_000) { runBlocking { "VOC_001" in requireNotNull(repository.vocabularyForProfile(a).first()) } }
        rule.scrollCommunicationTo("presentation")
        rule.onNodeWithTag("communication-presentation-FULL").performScrollTo().performClick()
        rule.waitUntil(10_000) { runBlocking { repository.presentationForProfile(a).first().selectedMode == CommunicationPresentationMode.FULL } }
        runBlocking {
            val preferences = repository.preferences.first()
            assertEquals(original.themeMode, preferences.themeMode)
            assertEquals(original.textScaleMode, preferences.textScaleMode)
            assertEquals(original.highContrast, preferences.highContrast)
            assertEquals(original.appMode, preferences.appMode)
        }
        assertMessage("Yo", "Agua", "Agua")
        rule.scrollCommunicationTo("categories")
        rule.onNodeWithTag("communication-category-grid").performScrollToKey("categories")
        rule.onNodeWithTag("communication-active-category").assertTextEquals("Categoría activa: Comida y bebida")
    }
    @Test fun editorBackAndSavedAnswersReturnToSettingsAndKeepMessage() {
        open()
        rule.scrollCommunicationTo("core-VOC_014"); rule.onNodeWithTag("communication-core-VOC_014").performClick()
        rule.scrollCommunicationTo("personalize"); rule.onNodeWithTag("communication-personalize").performClick()
        rule.onNodeWithTag("communication-bottom-bar").assertDoesNotExist()
        rule.onNodeWithTag("communication-age-ADULT").performScrollTo().performClick()
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("communication-profile-setup").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("communication-age-ADULT").performScrollTo().assertIsSelected()
        back()
        rule.awaitCommunicationCatalog()
        rule.onNodeWithTag("communication-destination-SETTINGS").assertIsSelected()
        runBlocking { assertEquals(CommunicationAgeRange.YEARS_9_TO_12, repository.setupForProfile(a).first()?.ageRange) }
        rule.scrollCommunicationTo("personalize"); rule.onNodeWithTag("communication-personalize").performClick()
        rule.onNodeWithTag("communication-age-ADULT").performScrollTo().performClick()
        rule.onNodeWithTag("communication-oral-VARIABLE_SPEECH").performScrollTo().performClick()
        rule.onNodeWithTag("communication-setup-save").performScrollTo().performClick()
        rule.awaitCommunicationCatalog()
        rule.onNodeWithTag("communication-destination-SETTINGS").assertIsSelected()
        rule.onNodeWithText("Rango de edad: 18 años o más").performScrollTo().assertIsDisplayed()
        assertMessage("Ayuda")
    }
    @Test fun switchingProfilesNeverMixesMessageSelectionOrPresentation() {
        open()
        rule.scrollCommunicationTo("core-VOC_006"); rule.onNodeWithTag("communication-core-VOC_006").performClick()
        rule.selectCommunicationCategory("CAT_003")
        rule.scrollCommunicationTo("presentation"); rule.onNodeWithTag("communication-presentation-FULL").performScrollTo().performClick()
        rule.waitUntil(10_000) { runBlocking { repository.presentationForProfile(a).first().selectedMode == CommunicationPresentationMode.FULL } }
        back()
        runBlocking { repository.saveVocabulary(b, setOf("VOC_014")); repository.selectProfile(b) }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Perfil activo: Comunicación B").fetchSemanticsNodes().isNotEmpty() }
        enter()
        rule.scrollCommunicationTo("message"); rule.onNodeWithText("Tu mensaje aparecerá aquí").assertIsDisplayed()
        rule.scrollCommunicationTo("core-VOC_014"); rule.onNodeWithTag("communication-core-VOC_014").performClick()
        rule.onNodeWithTag("communication-core-VOC_006").assertDoesNotExist()
        back()
        runBlocking { repository.selectProfile(a) }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Perfil activo: Comunicación A").fetchSemanticsNodes().isNotEmpty() }
        enter()
        assertMessage("Quiero")
        rule.scrollCommunicationTo("presentation"); rule.onNodeWithTag("communication-presentation-FULL").assertIsSelected()
        runBlocking { assertEquals(setOf("VOC_014"), repository.vocabularyForProfile(b).first()); assertNull(repository.presentationForProfile(b).first().selectedMode) }
    }
}
