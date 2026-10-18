package com.neuronovaapps.rutinago

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

class CommunicationPersonalVocabularyUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var repository: UserPreferencesRepository
    private lateinit var original: UserPreferences
    private val a = "vocabulary-ui-${UUID.randomUUID()}"
    private val b = "vocabulary-ui-${UUID.randomUUID()}"
    private val grid get() = rule.onNodeWithTag("communication-home")
    private val admin get() = rule.onNodeWithTag("personal-vocabulary")
    @Before fun prepare() = runBlocking {
        repository = UserPreferencesRepository(rule.activity)
        original = repository.preferences.first()
        for (id in listOf(a, b)) {
            repository.createProfile(if (id == a) "Vocabulario A" else "Vocabulario B", id)
            repository.saveSetup(CommunicationProfileSetup(id, CommunicationAgeRange.YEARS_9_TO_12, OralCommunication.EMERGING_SPEECH))
        }
        repository.selectProfile(a)
    }
    @After fun cleanup() = runBlocking {
        for (id in listOf(a, b)) {
            repository.deleteProfile(id)
            assertNull(repository.vocabularyForProfile(id).first())
        }
        original.activeProfile?.id?.let { repository.selectProfile(it) }
    }

    @Test fun initialReviewHideCancelRecoverResetAndNavigationPreserveMessageAndProfileIsolation() {
        rule.onNodeWithText("Comenzar").performClick()
        rule.onNodeWithTag("module-communication").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("vocabulary-start-suggested").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("vocabulary-start-suggested").performScrollTo().performClick()
        // Review a real word outside the suggestion before committing the selection.
        search("VOC_132")
        admin.performScrollToKey("VOC_132")
        rule.onNodeWithTag("vocabulary-toggle-VOC_132").performClick()
        runBlocking { assertNull(repository.vocabularyForProfile(a).first()) }
        admin.performScrollToKey("initial")
        rule.onNodeWithTag("vocabulary-confirm-initial").performScrollTo().performClick()
        rule.awaitCommunicationCatalog()
        runBlocking { assertTrue("VOC_132" in requireNotNull(repository.vocabularyForProfile(a).first())) }
        grid.performScrollToKey("core-VOC_014")
        rule.onNodeWithTag("communication-core-VOC_014").performClick()
        openAdmin()
        search("VOC_014")
        admin.performScrollToKey("VOC_014")
        rule.onNodeWithTag("vocabulary-toggle-VOC_014").performClick()
        rule.onNodeWithText("Cancelar").performClick()
        runBlocking { assertTrue("VOC_014" in requireNotNull(repository.vocabularyForProfile(a).first())) }
        rule.onNodeWithTag("vocabulary-toggle-VOC_014").performClick()
        rule.onNodeWithTag("vocabulary-confirm-hide").performClick()
        awaitSaved(a, "VOC_014", false)
        backFromAdmin()
        grid.performScrollToKey("message")
        rule.onNodeWithTag("communication-message-item-1").assertTextEquals("Ayuda")
        grid.performScrollToKey("core-title")
        rule.onNodeWithTag("communication-core-VOC_014").assertDoesNotExist()
        grid.performScrollToKey("presentation")
        rule.onNodeWithTag("communication-presentation-FULL").performScrollTo().performClick()
        grid.performScrollToKey("message")
        rule.onNodeWithTag("communication-clear").performScrollTo().performClick()
        openAdmin()
        search("VOC_014")
        admin.performScrollToKey("filters")
        rule.onNodeWithTag("vocabulary-filter-HIDDEN").performScrollTo().performClick()
        admin.performScrollToKey("VOC_014")
        rule.onNodeWithText("Recuperar Ayuda").performClick()
        awaitSaved(a, "VOC_014", true)
        backFromAdmin()
        grid.performScrollToKey("message")
        rule.onNodeWithTag("communication-undo-clear").performScrollTo().performClick()
        rule.onNodeWithTag("communication-message-item-1").assertTextEquals("Ayuda")
        openAdmin()
        admin.performScrollToKey("reset")
        rule.onNodeWithTag("vocabulary-reset").performClick()
        rule.onNodeWithText("Cancelar").performClick()
        rule.onNodeWithTag("vocabulary-reset").performClick()
        rule.onNodeWithTag("vocabulary-reset-all").performClick()
        rule.onNodeWithTag("vocabulary-confirm-reset").performClick()
        rule.waitUntil(10_000) { runBlocking { repository.vocabularyForProfile(a).first()?.size == 132 } }
        backFromAdmin()
        rule.activityRule.scenario.recreate()
        rule.awaitCommunicationCatalog()
        grid.performScrollToKey("message")
        rule.onNodeWithTag("communication-message-item-1").assertTextEquals("Ayuda")
        systemBack()
        rule.onNodeWithTag("module-selector").assertIsDisplayed()
        runBlocking { repository.selectProfile(b) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Perfil activo: Vocabulario B").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("module-communication").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("vocabulary-start-all").fetchSemanticsNodes().isNotEmpty() }
        runBlocking { assertNull(repository.vocabularyForProfile(b).first()); assertEquals(132, repository.vocabularyForProfile(a).first()?.size) }
        systemBack()
        rule.onNodeWithTag("module-selector").assertIsDisplayed()
    }

    private fun openAdmin() {
        grid.performScrollToKey("my-vocabulary")
        rule.onNodeWithTag("communication-my-vocabulary").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("vocabulary-search").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun search(value: String) {
        admin.performScrollToKey("search")
        rule.onNodeWithTag("vocabulary-search").performTextReplacement(value)
    }
    private fun backFromAdmin() { systemBack(); rule.awaitCommunicationCatalog() }
    private fun systemBack() { rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }
    private fun awaitSaved(id: String, word: String, visible: Boolean) {
        rule.waitUntil(10_000) { runBlocking { (word in requireNotNull(repository.vocabularyForProfile(id).first())) == visible } }
        rule.waitForIdle()
    }
}
