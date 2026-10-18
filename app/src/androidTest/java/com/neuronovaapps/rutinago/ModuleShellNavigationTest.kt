package com.neuronovaapps.rutinago

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

internal fun ComposeContentTestRule.enterRutinaGoModule() {
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Elige un módulo").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithTag("module-rutinago").performClick()
}

internal fun ComposeContentTestRule.awaitCommunicationCatalog() {
    // Existing communicator cases explicitly choose the full bank on first setup.
    waitUntil(timeoutMillis = 10_000) {
        onAllNodesWithTag("communication-bottom-bar").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithTag("vocabulary-start-all").fetchSemanticsNodes().isNotEmpty()
    }
    if (onAllNodesWithTag("vocabulary-start-all").fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithTag("vocabulary-start-all").performScrollTo().performClick()
        onNodeWithTag("vocabulary-confirm-initial").performScrollTo().performClick()
    }
    waitUntil(timeoutMillis = 10_000) {
        onAllNodesWithTag("communication-bottom-bar").fetchSemanticsNodes().isNotEmpty()
    }
}

internal fun ComposeContentTestRule.openCommunicationCategories() {
    scrollCommunicationTo("categories")
    if (onAllNodesWithTag("communication-category-all").fetchSemanticsNodes().isEmpty()) {
        onNodeWithTag("communication-category-toggle").performScrollTo().performClick()
    }
}

internal fun ComposeContentTestRule.selectCommunicationCategory(id: String?) {
    openCommunicationCategories()
    onNodeWithTag("communication-category-${id ?: "all"}").performScrollTo().performClick()
    waitForIdle()
}

/** Legacy navigation cases explicitly decline optional answers during test preparation. */
internal fun ComposeContentTestRule.completeCommunicationSetupIfNeeded() {
    waitUntil(timeoutMillis = 10_000) {
        onAllNodesWithTag("communication-bottom-bar").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithTag("personal-vocabulary").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithTag("communication-profile-setup").fetchSemanticsNodes().isNotEmpty()
    }
    if (onAllNodesWithTag("communication-profile-setup").fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithTag("communication-age-NOT_SPECIFIED").performScrollTo().performClick()
        onNodeWithTag("communication-oral-NOT_SPECIFIED").performScrollTo().performClick()
        onNodeWithTag("communication-setup-save").performScrollTo().performClick()
    }
}

/** Follow the visible public tabs; use the existing lazy content keys inside each tab. */
internal fun ComposeContentTestRule.scrollCommunicationTo(key: Any) {
    val value = key.toString()
    val destination = when {
        value in listOf("personalize", "presentation", "back") -> "SETTINGS"
        value in listOf("categories", "vocabulary-title", "count") || value.startsWith("VOC_") -> "EXPLORER"
        else -> "HOME"
    }
    if (destination == "EXPLORER") {
        if (onAllNodesWithTag("communication-category-grid").fetchSemanticsNodes().isEmpty()) {
            onNodeWithTag("communication-destination-HOME").performClick()
            onNodeWithTag("communication-home").performScrollToKey("explore")
            onNodeWithTag("communication-explore").performClick()
        }
    } else onNodeWithTag("communication-destination-$destination").performClick()
    waitForIdle()
    when (destination) {
        "SETTINGS" -> when (value) {
            "personalize" -> onNodeWithTag("communication-personalize").performScrollTo()
            "presentation" -> onNodeWithTag("communication-presentation-GUIDED").performScrollTo()
            else -> onNodeWithTag("communication-back-to-modules").performScrollTo()
        }
        else -> onNodeWithTag(if (destination == "HOME") "communication-home" else "communication-category-grid")
            .performScrollToKey(key)
    }
}

class ModuleShellNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun communicationIsIndependentAndBothBackActionsReturnToSelector() {
        openSelector()
        val profileText = composeRule.onAllNodesWithText("Perfil activo:", substring = true)
            .fetchSemanticsNodes().single().config[androidx.compose.ui.semantics.SemanticsProperties.Text]
            .single().text
        composeRule.onNodeWithTag("module-communication").performClick()
        composeRule.completeCommunicationSetupIfNeeded()
        composeRule.awaitCommunicationCatalog()
        composeRule.onNodeWithTag("communication-home").assertIsDisplayed()
        composeRule.onNodeWithText("ComunicaGo").assertIsDisplayed()
        composeRule.onNodeWithText(profileText).assertIsDisplayed()
        composeRule.onNodeWithTag("communication-message-area").assertIsDisplayed()
        composeRule.onNodeWithText("Tu mensaje aparecerá aquí").assertIsDisplayed()
        composeRule.scrollCommunicationTo("count")
        composeRule.onNodeWithText("132 palabras").assertIsDisplayed()
        composeRule.scrollCommunicationTo("VOC_001")
        composeRule.onNodeWithText("Yo").assertIsDisplayed()
        listOf("HOME", "ROUTINES", "CREATE", "PROGRESS", "SETTINGS").forEach {
            composeRule.onNodeWithTag("destination-$it").assertDoesNotExist()
        }
        composeRule.scrollCommunicationTo("back")
        composeRule.onNodeWithTag("communication-back-to-modules").performClick()
        composeRule.onNodeWithTag("module-selector").assertIsDisplayed()
        composeRule.onNodeWithTag("module-communication").performClick()
        composeRule.completeCommunicationSetupIfNeeded()
        composeRule.awaitCommunicationCatalog()
        pressSystemBack()
        composeRule.onNodeWithTag("module-selector").assertIsDisplayed()
    }

    @Test
    fun rutinaGoEntryOffersVisibleReturnAndSystemBackFromEveryMainTab() {
        openSelector()
        composeRule.enterRutinaGoModule()
        composeRule.onNodeWithTag("rutinago-back-to-modules").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("module-selector").assertIsDisplayed()

        listOf("HOME", "ROUTINES", "CREATE", "PROGRESS", "SETTINGS").forEach { destination ->
            composeRule.enterRutinaGoModule()
            composeRule.onNodeWithTag("destination-$destination").performClick()
            pressSystemBack()
            composeRule.onNodeWithTag("module-selector").assertIsDisplayed()
        }
    }

    @Test
    fun internalBackReturnsToRutinaGoBeforeLeavingTheModule() {
        openSelector()
        composeRule.enterRutinaGoModule()
        composeRule.onNodeWithTag("destination-SETTINGS").performClick()
        composeRule.onNodeWithText("Gestionar perfiles").performScrollTo().performClick()
        pressSystemBack()
        composeRule.onNodeWithText("Apariencia").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("module-selector").assertDoesNotExist()
        pressSystemBack()
        composeRule.onNodeWithTag("module-selector").assertIsDisplayed()
    }

    @Test
    fun switchingModulesPreservesRutinaGoDestinationAndSurvivesRecreation() {
        openSelector()
        val profileText = composeRule.onAllNodesWithText("Perfil activo:", substring = true)
            .fetchSemanticsNodes().single().config[androidx.compose.ui.semantics.SemanticsProperties.Text]
            .single().text
        composeRule.enterRutinaGoModule()
        composeRule.onNodeWithTag("destination-SETTINGS").performClick()
        composeRule.onNodeWithText("Cambiar módulo").performScrollTo().performClick()
        composeRule.onNodeWithTag("module-communication").performClick()
        composeRule.completeCommunicationSetupIfNeeded()
        composeRule.awaitCommunicationCatalog()
        composeRule.activityRule.scenario.recreate()
        composeRule.awaitCommunicationCatalog()
        composeRule.onNodeWithTag("communication-home").assertIsDisplayed()
        composeRule.onNodeWithText(profileText).assertIsDisplayed()
        composeRule.scrollCommunicationTo("back")
        composeRule.onNodeWithTag("communication-back-to-modules").performClick()
        composeRule.enterRutinaGoModule()
        composeRule.onNodeWithText("Apariencia").assertIsDisplayed()
        composeRule.onNodeWithTag("destination-ROUTINES").performClick()
        composeRule.onNodeWithTag("destination-CREATE").performClick()
        composeRule.onNodeWithText("Crear una rutina").assertIsDisplayed()
        composeRule.onNodeWithTag("destination-PROGRESS").performClick()
        composeRule.onNodeWithTag("destination-HOME").performClick()
        composeRule.onNodeWithTag("rutinago-back-to-modules").assertIsDisplayed()
    }

    private fun pressSystemBack() {
        composeRule.waitForIdle()
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }

    private fun openSelector() {
        composeRule.onNodeWithText("Comenzar").performClick()
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithText("Bienvenido a RutinaGo").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("Nombre del perfil").performTextInput("Perfil compartido")
            composeRule.onNodeWithText("Continuar").performClick()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Elige un módulo").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
