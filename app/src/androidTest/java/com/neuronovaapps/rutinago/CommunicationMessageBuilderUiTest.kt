package com.neuronovaapps.rutinago

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class CommunicationMessageBuilderUiTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun flexibleMessageSupportsQuickAccessCategoriesRepetitionAndReturningThroughRutinaGo() {
        val profile = openCommunication()
        composeRule.scrollCommunicationTo("message")
        composeRule.onNodeWithText("Tu mensaje aparecerá aquí").assertIsDisplayed()
        composeRule.onNodeWithTag("communication-remove-last").assertIsNotEnabled()
        composeRule.onNodeWithTag("communication-clear").assertIsNotEnabled()
        clickCore("VOC_006")
        selectCategory("CAT_003")
        clickWord("VOC_015")
        assertMessage(1 to "Quiero", 2 to "Agua")
        messageAction("communication-remove-last")
        assertMessage(1 to "Quiero")
        composeRule.onNodeWithTag("communication-message-item-2").assertDoesNotExist()
        messageAction("communication-clear")
        composeRule.onNodeWithText("Tu mensaje aparecerá aquí").assertIsDisplayed()
        composeRule.onNodeWithTag("communication-remove-last").assertIsNotEnabled()
        composeRule.onNodeWithTag("communication-clear").assertIsNotEnabled()
        messageAction("communication-undo-clear")
        assertMessage(1 to "Quiero")
        messageAction("communication-clear")
        clickCore("VOC_006")
        clickCore("VOC_006")
        selectCategory("CAT_001")
        clickWord("VOC_001")
        assertMessage(3 to "Quiero", 4 to "Quiero", 5 to "Yo")
        composeRule.onNodeWithTag("communication-undo-clear").assertDoesNotExist()

        listOf("VOC_001", "VOC_006", "VOC_007", "VOC_063", "VOC_053", "VOC_014").forEach { id ->
            composeRule.scrollCommunicationTo("core-$id")
            composeRule.onNodeWithTag("communication-core-$id").assertIsDisplayed().assertHasClickAction()
        }
        assertNoRutinaGoBarOrSpeech()
        composeRule.scrollCommunicationTo("back")
        composeRule.onNodeWithTag("communication-back-to-modules").performClick()
        composeRule.onNodeWithTag("module-selector").assertIsDisplayed()
        composeRule.enterRutinaGoModule()
        composeRule.onNodeWithTag("destination-HOME").assertIsDisplayed()
        composeRule.onNodeWithTag("rutinago-back-to-modules").performClick()
        composeRule.onNodeWithTag("module-communication").performClick()
        composeRule.awaitCommunicationCatalog()
        assertMessage(3 to "Quiero", 4 to "Quiero", 5 to "Yo")
        composeRule.scrollCommunicationTo("profile")
        composeRule.onNodeWithText(profile).assertIsDisplayed()
        assertNoRutinaGoBarOrSpeech()
        systemBack()
        composeRule.onNodeWithTag("module-selector").assertIsDisplayed()
    }

    @Test fun activityRecreationRestoresMessageCategoryAndUndoClear() {
        openCommunication()
        clickCore("VOC_007")
        selectCategory("CAT_004")
        clickWord("VOC_014")
        composeRule.activityRule.scenario.recreate()
        composeRule.awaitCommunicationCatalog()
        assertMessage(1 to "Necesito", 2 to "Ayuda")
        composeRule.openCommunicationCategories()
        composeRule.onNodeWithTag("communication-category-CAT_004").performScrollTo().assertIsSelected()
        messageAction("communication-clear")
        composeRule.activityRule.scenario.recreate()
        composeRule.awaitCommunicationCatalog()
        messageAction("communication-undo-clear")
        assertMessage(1 to "Necesito", 2 to "Ayuda")
        systemBack()
        composeRule.onNodeWithTag("module-selector").assertIsDisplayed()
        composeRule.onNodeWithTag("module-communication").performClick()
        composeRule.awaitCommunicationCatalog()
        assertMessage(1 to "Necesito", 2 to "Ayuda")
    }

    private fun openCommunication(): String {
        composeRule.onNodeWithText("Comenzar").performClick()
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithText("Bienvenido a RutinaGo").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("Nombre del perfil").performTextInput("Perfil comunicación")
            composeRule.onNodeWithText("Continuar").performClick()
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("module-selector").fetchSemanticsNodes().isNotEmpty()
        }
        val profile = composeRule.onAllNodesWithText("Perfil activo:", substring = true)
            .fetchSemanticsNodes().single().config[androidx.compose.ui.semantics.SemanticsProperties.Text].single().text
        composeRule.onNodeWithTag("module-communication").performClick()
        composeRule.completeCommunicationSetupIfNeeded()
        composeRule.awaitCommunicationCatalog()
        return profile
    }
    private fun clickCore(id: String) {
        composeRule.scrollCommunicationTo("core-$id")
        composeRule.onNodeWithTag("communication-core-$id").performClick()
    }
    private fun clickWord(id: String) {
        composeRule.scrollCommunicationTo(id)
        composeRule.onNodeWithTag("communication-word-$id").performClick()
    }
    private fun selectCategory(id: String) {
        composeRule.selectCommunicationCategory(id)
    }
    private fun assertMessage(vararg words: Pair<Int, String>) {
        composeRule.scrollCommunicationTo("message")
        words.forEach { (selectionId, text) ->
            composeRule.onNodeWithTag("communication-message-item-$selectionId").assertIsDisplayed().assertTextEquals(text)
        }
    }
    private fun messageAction(tag: String) {
        composeRule.scrollCommunicationTo("message")
        composeRule.onNodeWithTag(tag).performScrollTo().performClick()
    }
    private fun assertNoRutinaGoBarOrSpeech() {
        listOf("HOME", "ROUTINES", "CREATE", "PROGRESS", "SETTINGS").forEach {
            composeRule.onNodeWithTag("destination-$it").assertDoesNotExist()
        }
        composeRule.onNodeWithText("Hablar").assertDoesNotExist()
    }
    private fun systemBack() {
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }
}
