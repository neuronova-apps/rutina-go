package com.neuronovaapps.rutinago

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WelcomeNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun startOpensHomeAndKeepsMainNavigationAvailable() {
        composeRule.onNodeWithText("RUTINAGO").assertIsDisplayed()
        composeRule.onNodeWithText("Comenzar").performClick()

        composeRule.onNodeWithText("Hoy · Lunes").assertIsDisplayed()
        composeRule.onNodeWithText("Rutinas").assertIsDisplayed()
        composeRule.onNodeWithText("Configuración").performClick()
        composeRule.onNodeWithText("Apariencia").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Comenzar").fetchSemanticsNodes().isEmpty())
    }
}
