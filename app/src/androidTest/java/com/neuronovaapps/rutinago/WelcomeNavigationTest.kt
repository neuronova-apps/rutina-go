package com.neuronovaapps.rutinago

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WelcomeNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun startOpensHomeAndKeepsMainNavigationAvailable() {
        composeRule.onNodeWithText("RUTINAGO").assertIsDisplayed()
        startAndEnsureProfile()

        composeRule.onNodeWithText("Hoy · Lunes").assertIsDisplayed()
        composeRule.onNodeWithText("Rutinas").assertIsDisplayed()
        composeRule.onNodeWithText("Configuración").performClick()
        composeRule.onNodeWithText("Apariencia").assertIsDisplayed()
        composeRule.onNodeWithText("Muy grande").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Alto contraste").performScrollTo().assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Comenzar").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun aboutScreenOpensFromSettingsAndReturnsToSettings() {
        startAndEnsureProfile()
        composeRule.onNodeWithText("Configuración").performClick()

        composeRule.onNodeWithText("Acerca de RutinaGo")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("Sobre RutinaGo").assertIsDisplayed()
        composeRule.onNodeWithText("RUTINAGO").assertIsDisplayed()
        composeRule.onNodeWithText("Política de privacidad")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("‹ Volver").performClick()

        composeRule.onNodeWithText("Apariencia").assertIsDisplayed()
    }

    private fun startAndEnsureProfile() {
        composeRule.onNodeWithText("Comenzar").performClick()
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithText("Bienvenido a RutinaGo").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("Nombre del perfil").performTextInput("Perfil de prueba")
            composeRule.onNodeWithText("Continuar").performClick()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Hoy · Lunes").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
