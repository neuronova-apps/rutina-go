package com.neuronovaapps.rutinago

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.neuronovaapps.rutinago.data.preferences.UserPreferences
import com.neuronovaapps.rutinago.data.preferences.UserPreferencesRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationPresentationMode
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CommunicationPresentationUiTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()
    private lateinit var repository: UserPreferencesRepository
    private lateinit var original: UserPreferences
    private val firstId = "presentation-ui-${UUID.randomUUID()}"
    private val secondId = "presentation-ui-${UUID.randomUUID()}"

    @Before fun prepareProfiles() = runBlocking {
        repository = UserPreferencesRepository(composeRule.activity)
        original = repository.preferences.first()
        repository.createProfile("Presentación A", firstId)
        repository.createProfile("Presentación B", secondId)
        repository.selectProfile(firstId)
    }

    @After fun cleanOnlyTestProfiles() = runBlocking {
        listOf(firstId, secondId).forEach { id ->
            repository.deleteProfile(id)
            assertNull(repository.setupForProfile(id).first())
            assertNull(repository.presentationForProfile(id).first().selectedMode)
            assertFalse(repository.presentationForProfile(id).first().introductionSeen)
        }
        original.activeProfile?.id?.let { id ->
            if (repository.preferences.first().profiles.any { it.id == id }) repository.selectProfile(id)
        }
    }

    @Test fun referenceProfileChangesViewsWithoutLosingMessageCategoryUndoOrPreference() {
        composeRule.onNodeWithText("Comenzar").performClick()
        awaitProfile("Presentación A")
        composeRule.onNodeWithTag("module-communication").performClick()
        saveAnswers("YEARS_9_TO_12", "EMERGING_SPEECH")
        composeRule.awaitCommunicationCatalog()
        composeRule.scrollCommunicationTo("presentation")
        composeRule.onNodeWithTag("communication-presentation-GUIDED").assertIsSelected()
        composeRule.onNodeWithTag("communication-presentation-introduction").performScrollTo().assertIsDisplayed()
        selectMode(CommunicationPresentationMode.FULL)
        assertCompleteCatalog()
        clickCore("VOC_001")
        clickCore("VOC_006")
        composeRule.selectCommunicationCategory("CAT_003")
        composeRule.scrollCommunicationTo("VOC_015")
        composeRule.onNodeWithTag("communication-word-VOC_015").performClick()
        assertMessage()
        selectMode(CommunicationPresentationMode.GUIDED)
        assertMessage()
        assertCategory("CAT_003")
        clickCore("VOC_006")
        composeRule.scrollCommunicationTo("message")
        composeRule.onNodeWithTag("communication-message-item-4").assertTextEquals("Quiero")
        messageAction("communication-remove-last")
        assertMessage()
        messageAction("communication-clear")
        selectMode(CommunicationPresentationMode.FULL)
        messageAction("communication-undo-clear")
        assertMessage()
        composeRule.selectCommunicationCategory("CAT_004")
        assertMessage()
        composeRule.scrollCommunicationTo("personalize")
        composeRule.onNodeWithTag("communication-personalize").performClick()
        saveAnswers("YEARS_3_TO_5", "NO_CURRENT_SPEECH")
        composeRule.awaitCommunicationCatalog()
        assertMode(CommunicationPresentationMode.FULL)
        assertMessage()
        assertCategory("CAT_004")
        systemBack()
        composeRule.onNodeWithTag("module-selector").assertIsDisplayed()
        composeRule.onNodeWithTag("module-communication").performClick()
        composeRule.awaitCommunicationCatalog()
        assertMode(CommunicationPresentationMode.FULL)
        composeRule.onNodeWithTag("communication-presentation-introduction").assertDoesNotExist()
        assertMessage()
        composeRule.activityRule.scenario.recreate()
        composeRule.awaitCommunicationCatalog()
        assertMode(CommunicationPresentationMode.FULL)
        composeRule.onNodeWithTag("communication-presentation-introduction").assertDoesNotExist()
        assertMessage()
        returnToSelector()
        runBlocking { repository.selectProfile(secondId) }
        awaitProfile("Presentación B")
        composeRule.onNodeWithTag("module-communication").performClick()
        saveAnswers("NOT_SPECIFIED", "NOT_SPECIFIED")
        composeRule.awaitCommunicationCatalog()
        assertMode(CommunicationPresentationMode.GUIDED)
        selectMode(CommunicationPresentationMode.GUIDED)
        assertCompleteCatalog()
        runBlocking {
            assertEquals(CommunicationPresentationMode.FULL, repository.presentationForProfile(firstId).first().selectedMode)
            assertEquals(CommunicationPresentationMode.GUIDED, repository.presentationForProfile(secondId).first().selectedMode)
            repository.renameProfile(firstId, "Presentación A renombrada")
        }
        returnToSelector()
        runBlocking { repository.selectProfile(firstId) }
        awaitProfile("Presentación A renombrada")
        composeRule.onNodeWithTag("module-communication").performClick()
        composeRule.awaitCommunicationCatalog()
        assertMode(CommunicationPresentationMode.FULL)
        assertMessage()
        assertCategory("CAT_004")
        systemBack()
        composeRule.onNodeWithTag("module-selector").assertIsDisplayed()
    }

    private fun awaitProfile(name: String) = composeRule.waitUntil(5_000) {
        composeRule.onAllNodesWithText("Perfil activo: $name").fetchSemanticsNodes().isNotEmpty()
    }
    private fun saveAnswers(age: String, oral: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("communication-profile-setup").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("communication-age-$age").performScrollTo().performClick()
        composeRule.onNodeWithTag("communication-oral-$oral").performScrollTo().performClick()
        composeRule.onNodeWithTag("communication-setup-save").performScrollTo().performClick()
    }
    private fun selectMode(mode: CommunicationPresentationMode) {
        composeRule.scrollCommunicationTo("presentation")
        val option = composeRule.onNodeWithTag("communication-presentation-${mode.name}")
        option.performScrollTo().assertIsEnabled().performClick()
        composeRule.waitUntil(5_000) {
            option.fetchSemanticsNode().config.getOrElse(SemanticsProperties.Selected) { false } &&
                composeRule.onAllNodesWithText("Guardando presentación…").fetchSemanticsNodes().isEmpty()
        }
        assertMode(mode)
    }
    private fun assertMode(mode: CommunicationPresentationMode) {
        composeRule.scrollCommunicationTo("presentation")
        composeRule.onNodeWithTag("communication-presentation-${mode.name}").performScrollTo().assertIsSelected()
    }
    private fun assertCategory(id: String) {
        composeRule.openCommunicationCategories()
        composeRule.onNodeWithTag("communication-category-$id").performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag("communication-category-toggle").performScrollTo().performClick()
    }
    private fun assertCompleteCatalog() {
        composeRule.selectCommunicationCategory(null)
        composeRule.scrollCommunicationTo("count")
        composeRule.onNodeWithTag("communication-vocabulary-count").assertTextEquals("132 palabras")
        composeRule.openCommunicationCategories()
        composeRule.onAllNodes(SemanticsMatcher("Las 18 categorías originales") {
            it.config.getOrElse(SemanticsProperties.TestTag) { "" }.startsWith("communication-category-CAT_")
        }).assertCountEquals(18)
        composeRule.onNodeWithTag("communication-category-all").performScrollTo().assertIsSelected().performClick()
        listOf("HOME", "ROUTINES", "CREATE", "PROGRESS", "SETTINGS").forEach {
            composeRule.onNodeWithTag("destination-$it").assertDoesNotExist()
        }
        listOf("Hablar", "Repetir").forEach { composeRule.onNodeWithText(it).assertDoesNotExist() }
    }
    private fun clickCore(id: String) {
        composeRule.scrollCommunicationTo("core-$id")
        composeRule.onNodeWithTag("communication-core-$id").performClick()
    }
    private fun assertMessage() {
        composeRule.scrollCommunicationTo("message")
        listOf("Yo", "Quiero", "Agua").forEachIndexed { index, word ->
            composeRule.onNodeWithTag("communication-message-item-${index + 1}").assertTextEquals(word)
        }
    }
    private fun messageAction(tag: String) {
        composeRule.scrollCommunicationTo("message")
        composeRule.onNodeWithTag(tag).performScrollTo().performClick()
    }
    private fun returnToSelector() {
        composeRule.scrollCommunicationTo("back")
        composeRule.onNodeWithTag("communication-back-to-modules").performClick()
        composeRule.onNodeWithTag("module-selector").assertIsDisplayed()
    }
    private fun systemBack() {
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }
}
