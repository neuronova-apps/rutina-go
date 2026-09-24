package com.neuronovaapps.rutinago

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.neuronovaapps.rutinago.data.demo.DemoContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RoutineExecutionNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun exitConfirmationCanContinueAtSameStepAndExitWithoutResult() {
        startRoutineFromHome()
        composeRule.onNodeWithText("Completar paso").performClick()
        composeRule.onNodeWithText("Paso 2 de ${DemoContent.routines.first().steps.size}").assertIsDisplayed()

        composeRule.onNodeWithText("Salir").performClick()
        composeRule.onNodeWithText("¿Deseas salir de la rutina?").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar rutina").performClick()
        composeRule.onNodeWithText("Paso 2 de ${DemoContent.routines.first().steps.size}").assertIsDisplayed()

        composeRule.onNodeWithText("Salir").performClick()
        composeRule.onNodeWithText("Salir de la rutina").performClick()
        composeRule.onNodeWithText("Hoy · Lunes").assertIsDisplayed()
        assertNoResultPanel()
    }

    @Test
    fun systemBackShowsConfirmationAndASecondBackDismissesIt() {
        startRoutineFromHome()
        pressSystemBack()
        composeRule.onNodeWithText("¿Deseas salir de la rutina?").assertIsDisplayed()

        pressSystemBack()
        composeRule.onNodeWithText("Paso 1 de ${DemoContent.routines.first().steps.size}").assertIsDisplayed()
        composeRule.onNodeWithText("Salir").assertIsDisplayed()
    }

    @Test
    fun completingAllStepsShowsCompletedResultAndClosesToOrigin() {
        startRoutineFromHome()
        repeat(DemoContent.routines.first().steps.size) {
            composeRule.onNodeWithText("Completar paso").performClick()
        }

        composeRule.onNodeWithText("Rutina completada").assertIsDisplayed()
        composeRule.onNodeWithText(
            "${DemoContent.routines.first().steps.size} de ${DemoContent.routines.first().steps.size} pasos realizados.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Cerrar y volver").performClick()
        composeRule.onNodeWithText("Hoy · Lunes").assertIsDisplayed()
    }

    @Test
    fun exitingRoutineStartedFromRoutinesReturnsToRoutines() {
        startAndEnsureProfile()
        composeRule.onNodeWithText("Rutinas").performClick()
        composeRule.onAllNodesWithText("Iniciar").onFirst().performClick()
        composeRule.onNodeWithText("Salir").performClick()
        composeRule.onNodeWithText("Salir de la rutina").performClick()

        composeRule.onNodeWithText("Mis rutinas").assertIsDisplayed()
        assertNoResultPanel()
    }

    @Test
    fun explicitPartialCompletionUsesRespectfulDistinctResult() {
        startRoutineFromHome()
        repeat(DemoContent.routines.first().steps.size) {
            composeRule.onNodeWithText("Omitir").performClick()
        }

        composeRule.onNodeWithText("Rutina realizada parcialmente").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Completaste 0 de ${DemoContent.routines.first().steps.size} pasos.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Cerrar y volver").performClick()
        composeRule.onNodeWithText("Hoy · Lunes").assertIsDisplayed()
    }

    private fun startRoutineFromHome() {
        startAndEnsureProfile()
        composeRule.onNodeWithText("Iniciar rutina").performClick()
        composeRule.onNodeWithText("Paso 1 de ${DemoContent.routines.first().steps.size}").assertIsDisplayed()
    }

    private fun startAndEnsureProfile() {
        composeRule.onNodeWithText("Comenzar").performClick()
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithText("Bienvenido a RutinaGo").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("Nombre del perfil").performTextInput("Ejecución")
            composeRule.onNodeWithText("Continuar").performClick()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Hoy · Lunes").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun pressSystemBack() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }

    private fun assertNoResultPanel() {
        assertTrue(composeRule.onAllNodesWithText("Rutina completada").fetchSemanticsNodes().isEmpty())
        assertTrue(
            composeRule.onAllNodesWithText("Rutina realizada parcialmente").fetchSemanticsNodes().isEmpty(),
        )
        assertTrue(composeRule.onAllNodesWithText("Rutina cancelada").fetchSemanticsNodes().isEmpty())
    }
}
