package com.neuronovaapps.rutinago

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.neuronovaapps.rutinago.ui.screens.profiles.FirstProfileScreen
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ProfileScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstProfileValidatesAndSubmitsTrimmedName() {
        var createdName: String? = null
        composeRule.setContent {
            RutinaGoTheme {
                FirstProfileScreen(onCreateProfile = { createdName = it })
            }
        }

        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.onNodeWithText("Introduce un nombre para continuar.").assertIsDisplayed()
        assertNull(createdName)

        composeRule.onNodeWithText("Nombre del perfil").performTextInput("  Mi perfil  ")
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.runOnIdle { assertEquals("Mi perfil", createdName) }
    }
}
