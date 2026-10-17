package com.neuronovaapps.rutinago

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class CreateNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun templateAndScratchReturnToOriginWithInterfaceAndSystemBack() {
        openCreateOrigin()
        composeRule.onNodeWithText("Usar una plantilla").performClick()
        composeRule.onNodeWithText("‹ Volver a Elegir origen").performClick()
        composeRule.onNodeWithText("Elegir origen").assertIsDisplayed()

        composeRule.onNodeWithText("Crear desde cero").performClick()
        pressSystemBack()
        composeRule.onNodeWithText("Elegir origen").assertIsDisplayed()

        pressSystemBack()
        composeRule.onNodeWithText("Crear una rutina").assertIsDisplayed()
    }

    @Test
    fun dirtyDraftRequiresConfirmationAndCanContinueEditing() {
        openCreateOrigin()
        composeRule.onNodeWithText("Crear desde cero").performClick()
        composeRule.onNodeWithText("Nombre de la rutina").performTextInput("Rutina de prueba")
        pressSystemBack()

        composeRule.onNodeWithText("¿Deseas salir sin guardar los cambios?").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar editando").performClick()
        composeRule.onNodeWithText("Rutina de prueba").assertIsDisplayed()

        pressSystemBack()
        composeRule.onNodeWithText("Salir sin guardar").performClick()
        composeRule.onNodeWithText("Elegir origen").assertIsDisplayed()
    }

    @Test
    fun savedRoutineAppearsInMyRoutinesAndOpensForEditing() {
        val routineName = "Rutina de navegación ${System.nanoTime()}"
        openCreateOrigin()
        composeRule.onNodeWithText("Crear desde cero").performClick()
        composeRule.onNodeWithText("Nombre de la rutina").performTextInput(routineName)

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Despertarse"))
        composeRule.onNodeWithText("Despertarse").performClick()
        composeRule.onNodeWithText("Guardar rutina").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Rutina guardada correctamente").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(routineName).fetchSemanticsNodes().isNotEmpty()
        }
        val cardTag = composeRule.onNodeWithText(routineName).fetchSemanticsNode()
            .config[SemanticsProperties.TestTag]
        composeRule.onNode(hasText("Editar") and hasAnyAncestor(hasTestTag(cardTag)))
            .performScrollTo().performClick()
        composeRule.onNodeWithText("Nombre de la rutina").assertIsDisplayed()
        composeRule.onNodeWithText(routineName).assertIsDisplayed()
    }

    private fun openCreateOrigin() {
        composeRule.onNodeWithText("Comenzar").performClick()
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithText("Bienvenido a RutinaGo").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("Nombre del perfil").performTextInput("Navegación")
            composeRule.onNodeWithText("Continuar").performClick()
        }
        composeRule.enterRutinaGoModule()
        composeRule.onNodeWithTag("rutinago-back-to-modules").assertIsDisplayed()
        composeRule.onNodeWithText("Crear").performClick()
        composeRule.onNodeWithText("Elegir origen").performClick()
    }

    private fun pressSystemBack() {
        composeRule.waitForIdle()
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }
}
