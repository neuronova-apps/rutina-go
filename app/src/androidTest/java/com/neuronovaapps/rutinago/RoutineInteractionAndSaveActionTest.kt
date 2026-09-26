package com.neuronovaapps.rutinago

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant
import com.neuronovaapps.rutinago.data.preferences.AppMode
import com.neuronovaapps.rutinago.domain.catalog.CatalogActivity
import com.neuronovaapps.rutinago.domain.catalog.CatalogRoutine
import com.neuronovaapps.rutinago.domain.catalog.CatalogRoutineStep
import com.neuronovaapps.rutinago.domain.catalog.CatalogSnapshot
import com.neuronovaapps.rutinago.domain.catalog.CatalogUiState
import com.neuronovaapps.rutinago.domain.user.UserRoutine
import com.neuronovaapps.rutinago.domain.user.UserRoutineDraft
import com.neuronovaapps.rutinago.domain.user.UserRoutineStep
import com.neuronovaapps.rutinago.ui.screens.create.CreateScreen
import com.neuronovaapps.rutinago.ui.screens.routines.RoutinesScreen
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class RoutineInteractionAndSaveActionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sameNameRoutinesKeepIndependentIdsAndOnlyPressedRoutineStartsOnce() {
        val startGate = CompletableDeferred<Unit>()
        val starts = AtomicInteger(0)
        val startedIds = mutableListOf<String>()
        val first = userRoutine("personal-1", "Rutina repetida")
        val second = userRoutine("personal-2", "Rutina repetida")

        composeRule.setContent {
            RutinaGoTheme {
                RoutinesScreen(
                    appMode = AppMode.SIMPLE,
                    catalogState = CatalogUiState.Ready(testCatalog(activityCount = 1)),
                    activeProfileName = "Pruebas",
                    userRoutines = listOf(first, second),
                    activeExecution = null,
                    onOpenRoutine = {},
                    onUseTemplate = {},
                    onEditUserRoutine = {},
                    onStartUserRoutine = { id ->
                        starts.incrementAndGet()
                        startedIds += id
                        startGate.await()
                    },
                    onResumeExecution = {},
                    onArchiveUserRoutine = {},
                )
            }
        }

        val firstButton = composeRule.onNodeWithTag("start-routine-personal-1")
        val secondButton = composeRule.onNodeWithTag("start-routine-personal-2")
        firstButton.performClick()
        composeRule.waitForIdle()

        firstButton.assert(hasStateDescription("Iniciando rutina"))
        secondButton.assert(hasStateDescription("Disponible")).assertIsEnabled()
        firstButton.performClick()
        secondButton.performClick()
        composeRule.waitForIdle()

        assertEquals(1, starts.get())
        assertEquals(listOf("personal-1"), startedIds)
        startGate.complete(Unit)
    }

    @Test
    fun scratchEditorKeepsSingleSaveActionVisibleWhileContentScrollsAndKeyboardIsOpen() {
        var savedDraft: UserRoutineDraft? = null
        composeRule.setContent {
            RutinaGoTheme(highContrast = true, textScale = 1.45f) {
                CreateScreen(
                    appMode = AppMode.ADVANCED,
                    catalogState = CatalogUiState.Ready(testCatalog(activityCount = 30)),
                    onSaveRoutine = { draft ->
                        savedDraft = draft
                        draft.toStoredRoutine()
                    },
                    onViewRoutines = {},
                )
            }
        }

        composeRule.onNodeWithText("Elegir origen").performClick()
        composeRule.onNodeWithText("Crear desde cero").performClick()
        val saveAction = composeRule.onNodeWithTag("save-routine-action")
        saveAction.assertIsDisplayed()

        composeRule.onNodeWithText("Nombre de la rutina").performClick().performTextInput("Rutina larga")
        saveAction.assertIsDisplayed()
        composeRule.onNodeWithText("Actividad 01").performScrollTo().performClick()
        composeRule.onNodeWithText("Actividad 30").performScrollTo().assertIsDisplayed()
        saveAction.assertIsDisplayed().assertIsEnabled().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { savedDraft != null }

        assertEquals("Rutina larga", savedDraft?.name)
        assertNull(savedDraft?.sourceTemplateId)
        assertEquals(listOf("ACT-001"), savedDraft?.steps?.map { it.activityId })
    }

    @Test
    fun templateEditorUsesTheSameFixedSaveActionAndPreservesTemplateSource() {
        var savedDraft: UserRoutineDraft? = null
        val catalog = testCatalog(activityCount = 30, includeTemplate = true)
        composeRule.setContent {
            RutinaGoTheme {
                CreateScreen(
                    appMode = AppMode.ADVANCED,
                    catalogState = CatalogUiState.Ready(catalog),
                    suggestedTemplateId = "RUT-TEST",
                    onSaveRoutine = { draft ->
                        savedDraft = draft
                        draft.toStoredRoutine()
                    },
                    onViewRoutines = {},
                )
            }
        }

        composeRule.onNodeWithText("Personalizar plantilla").assertIsDisplayed()
        composeRule.onNodeWithText("Actividad 30").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("save-routine-action")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { savedDraft != null }

        assertEquals("RUT-TEST", savedDraft?.sourceTemplateId)
        assertEquals(listOf("ACT-001"), savedDraft?.steps?.map { it.activityId })
    }

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private fun userRoutine(id: String, name: String) = UserRoutine(
        id = id,
        profileId = "profile-1",
        sourceTemplateId = null,
        name = name,
        description = null,
        createdAtEpochMillis = 1L,
        modifiedAtEpochMillis = 1L,
        archived = false,
        steps = listOf(UserRoutineStep(1, "ACT-001", null, null)),
    )

    private fun testCatalog(
        activityCount: Int,
        includeTemplate: Boolean = false,
    ): CatalogSnapshot {
        val activities = (1..activityCount).map { index ->
            CatalogActivity(
                id = "ACT-${index.toString().padStart(3, '0')}",
                category = "Autonomía",
                name = "Actividad ${index.toString().padStart(2, '0')}",
                simpleInstruction = "Completa la actividad $index",
                arasaacSearchTerm = "actividad",
                ageGroup = "Todas",
                autonomyLevel = "Variable",
                supervision = "Según necesidad",
                simpleMode = true,
                advancedMode = true,
                visualResource = "pending",
                status = "active",
                observations = null,
            )
        }
        val routines = if (includeTemplate) {
            listOf(
                CatalogRoutine(
                    id = "RUT-TEST",
                    name = "Plantilla extensa",
                    primaryArea = "Autonomía",
                    context = "Prueba",
                    ageGroup = "Todas",
                    functionalObjective = "Validar la edición",
                    suggestedMoment = "Cualquier momento",
                    simpleStepCount = 1,
                    completeStepCount = 1,
                    personalizable = true,
                    keySupervision = "Según necesidad",
                    status = "active",
                    observations = null,
                ),
            )
        } else {
            emptyList()
        }
        val steps = if (includeTemplate) {
            listOf(CatalogRoutineStep("RUT-TEST", "ACT-001", RoutineVariant.COMPLETE, 1))
        } else {
            emptyList()
        }
        return CatalogSnapshot(
            categories = emptyList(),
            activities = activities,
            routines = routines,
            steps = steps,
        )
    }

    private fun UserRoutineDraft.toStoredRoutine() = UserRoutine(
        id = id,
        profileId = "profile-1",
        sourceTemplateId = sourceTemplateId,
        name = name,
        description = description,
        createdAtEpochMillis = 1L,
        modifiedAtEpochMillis = 1L,
        archived = false,
        steps = steps.mapIndexed { index, step ->
            UserRoutineStep(
                position = index + 1,
                activityId = step.activityId,
                customInstruction = step.customInstruction,
                futureVisualReference = step.futureVisualReference,
            )
        },
    )
}
