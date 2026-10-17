package com.neuronovaapps.rutinago

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.neuronovaapps.rutinago.data.preferences.UserPreferencesRepository
import com.neuronovaapps.rutinago.domain.user.UserRoutineDraft
import com.neuronovaapps.rutinago.domain.user.UserRoutineDraftStep
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RoutineExecutionNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun exitConfirmationCanContinueAtSameStepAndExitWithoutResult() {
        startPersonalRoutine()
        composeRule.onNodeWithText("Completar paso").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Paso 2 de 2").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Paso 2 de 2").assertIsDisplayed()

        composeRule.onNodeWithText("Salir").performClick()
        composeRule.onNodeWithText("¿Deseas salir de la rutina?").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar rutina").performClick()
        composeRule.onNodeWithText("Paso 2 de 2").assertIsDisplayed()

        composeRule.onNodeWithText("Salir").performClick()
        composeRule.onNodeWithText("Salir de la rutina").performClick()
        composeRule.onNodeWithText("Mis rutinas").assertIsDisplayed()
        assertNoResultPanel()
    }

    @Test
    fun systemBackShowsConfirmationAndASecondBackDismissesIt() {
        startPersonalRoutine()
        pressSystemBack()
        composeRule.onNodeWithText("¿Deseas salir de la rutina?").assertIsDisplayed()

        pressSystemBack()
        composeRule.onNodeWithText("Paso 1 de 2").assertIsDisplayed()
        composeRule.onNodeWithText("Salir").assertIsDisplayed()
    }

    @Test
    fun completingAllStepsShowsRealCompletedResultAndClosesToOrigin() {
        startPersonalRoutine()
        repeat(2) { composeRule.onNodeWithText("Completar paso").performClick() }

        composeRule.onNodeWithText("Rutina completada").assertIsDisplayed()
        composeRule.onNodeWithText("2 completados · 0 omitidos · 2 pasos en total").assertIsDisplayed()
        composeRule.onNodeWithText("Cerrar y volver").performClick()
        composeRule.onNodeWithText("Mis rutinas").assertIsDisplayed()
    }

    @Test
    fun omittedStepsProduceRespectfulPartialResult() {
        startPersonalRoutine()
        repeat(2) { composeRule.onNodeWithText("Omitir").performClick() }

        composeRule.onNodeWithText("Rutina realizada parcialmente").assertIsDisplayed()
        composeRule.onNodeWithText("0 completados · 2 omitidos · 2 pasos en total").assertIsDisplayed()
    }

    private fun startPersonalRoutine() {
        startAndEnsureProfile()
        val application = composeRule.activity.application as RutinaGoApplication
        val preferencesRepository = UserPreferencesRepository(composeRule.activity)
        runBlocking {
            application.catalogContainer.importer.importIfNeeded()
            val profileId = requireNotNull(preferencesRepository.preferences.first().activeProfile?.id)
            application.catalogContainer.executions.activeExecution(profileId).first()?.let {
                application.catalogContainer.executions.abandon(profileId, it.id)
            }
            application.catalogContainer.userRoutines.save(
                profileId,
                UserRoutineDraft(
                    id = TEST_ROUTINE_ID,
                    sourceTemplateId = null,
                    name = "Rutina de prueba real",
                    steps = listOf(
                        UserRoutineDraftStep("ACT-001", "Haz la primera actividad"),
                        UserRoutineDraftStep("ACT-002", "Haz la segunda actividad"),
                    ),
                ),
            )
        }
        composeRule.onNodeWithText("Rutinas").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Rutina de prueba real").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Iniciar rutina").onFirst().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Paso 1 de 2").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun startAndEnsureProfile() {
        composeRule.onNodeWithText("Comenzar").performClick()
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithText("Bienvenido a RutinaGo").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("Nombre del perfil").performTextInput("Ejecución")
            composeRule.onNodeWithText("Continuar").performClick()
        }
        composeRule.enterRutinaGoModule()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Perfil activo").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun pressSystemBack() {
        composeRule.waitForIdle()
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
    }

    private companion object {
        const val TEST_ROUTINE_ID = "navigation-real-routine"
    }
}
