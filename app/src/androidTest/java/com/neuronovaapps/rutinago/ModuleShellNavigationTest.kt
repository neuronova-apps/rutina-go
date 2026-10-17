package com.neuronovaapps.rutinago

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

internal fun ComposeContentTestRule.enterRutinaGoModule() {
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText("Elige un módulo").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithTag("module-rutinago").performClick()
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
        composeRule.onNodeWithTag("communication-placeholder").assertIsDisplayed()
        composeRule.onNodeWithText(profileText).assertIsDisplayed()
        composeRule.onNodeWithText("Próximamente").assertIsDisplayed()
        composeRule.onNodeWithTag("destination-HOME").assertDoesNotExist()
        composeRule.onNodeWithText("Volver al selector").performClick()
        composeRule.onNodeWithTag("module-selector").assertIsDisplayed()
        composeRule.onNodeWithTag("module-communication").performClick()
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithTag("module-selector").assertIsDisplayed()
    }

    @Test
    fun switchingModulesPreservesRutinaGoDestinationAndSurvivesRecreation() {
        openSelector()
        composeRule.enterRutinaGoModule()
        composeRule.onNodeWithTag("destination-SETTINGS").performClick()
        composeRule.onNodeWithText("Cambiar módulo").performScrollTo().performClick()
        composeRule.onNodeWithTag("module-communication").performClick()
        composeRule.activityRule.scenario.recreate()
        composeRule.onNodeWithTag("communication-placeholder").assertIsDisplayed()
        composeRule.onNodeWithText("Volver al selector").performClick()
        composeRule.enterRutinaGoModule()
        composeRule.onNodeWithText("Apariencia").assertIsDisplayed()
        composeRule.onNodeWithTag("destination-ROUTINES").performClick()
        composeRule.onNodeWithTag("destination-CREATE").performClick()
        composeRule.onNodeWithText("Crear una rutina").assertIsDisplayed()
        composeRule.onNodeWithTag("destination-PROGRESS").performClick()
        composeRule.onNodeWithTag("destination-HOME").performClick()
        composeRule.onNodeWithText("Hoy · Lunes").assertIsDisplayed()
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
