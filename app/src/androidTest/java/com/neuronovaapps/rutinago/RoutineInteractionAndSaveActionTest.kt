package com.neuronovaapps.rutinago

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant
import com.neuronovaapps.rutinago.data.preferences.AppMode
import com.neuronovaapps.rutinago.domain.catalog.CatalogActivity
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacPictogramCandidate
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacRepository
import com.neuronovaapps.rutinago.domain.catalog.CatalogRoutine
import com.neuronovaapps.rutinago.domain.catalog.CatalogRoutineStep
import com.neuronovaapps.rutinago.domain.catalog.CatalogSnapshot
import com.neuronovaapps.rutinago.domain.catalog.CatalogUiState
import com.neuronovaapps.rutinago.domain.user.UserRoutine
import com.neuronovaapps.rutinago.domain.user.UserRoutineDraft
import com.neuronovaapps.rutinago.domain.user.UserRoutineStep
import com.neuronovaapps.rutinago.ui.screens.create.CreateScreen
import com.neuronovaapps.rutinago.ui.screens.arasaac.ARASAAC_ATTRIBUTION
import com.neuronovaapps.rutinago.ui.screens.create.UserRoutineEditorScreen
import com.neuronovaapps.rutinago.ui.screens.routines.RoutinesScreen
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RoutineInteractionAndSaveActionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun manualArasaacSelectionIsPreviewOnlyAndDoesNotEnterSavedRoutine() {
        val queries = mutableListOf<String>()
        var savedDraft: UserRoutineDraft? = null
        val candidate = ArasaacPictogramCandidate(
            id = 6456,
            keywords = listOf("comer"),
            description = "Alimentación",
            categories = listOf("Acciones"),
            previewUrl = "https://static.arasaac.org/pictograms/6456/6456_300.png",
        )
        val fake = object : ArasaacRepository {
            override suspend fun searchPictograms(query: String): List<ArasaacPictogramCandidate> {
                queries += query
                return listOf(candidate)
            }
            override suspend fun getPictogram(id: Long): ArasaacPictogramCandidate {
                assertEquals(6456L, id)
                return candidate
            }
        }
        composeRule.setContent {
            RutinaGoTheme(highContrast = true, textScale = 1.45f) {
                CreateScreen(
                    appMode = AppMode.SIMPLE,
                    catalogState = CatalogUiState.Ready(testCatalog(activityCount = 1)),
                    onSaveRoutine = { draft ->
                        savedDraft = draft
                        draft.toStoredRoutine()
                    },
                    onSaved = {},
                    arasaacRepository = fake,
                )
            }
        }
        composeRule.onNodeWithText("Elegir origen").performClick()
        composeRule.onNodeWithText("Crear desde cero").performClick()
        composeRule.onNodeWithText("Nombre de la rutina").performTextInput("Prueba manual")
        composeRule.onNodeWithText("Actividad 01").performClick()
        composeRule.onNodeWithText("Buscar pictograma").performClick()
        composeRule.onNodeWithTag("arasaac-query").assertTextContains("actividad")
            .performTextReplacement("comer")
        composeRule.waitUntil(5_000) { queries.contains("comer") }
        composeRule.onNodeWithTag("arasaac-result-6456").performClick()
        composeRule.onNodeWithText(ARASAAC_ATTRIBUTION).assertIsDisplayed()
        composeRule.onNodeWithTag("arasaac-provisional-select").assertIsEnabled().performClick()
        composeRule.onNodeWithText("Selección provisional. No guardada en el catálogo ni en la rutina.")
            .assertIsDisplayed()
        assertNull(savedDraft)
        composeRule.onNodeWithText("‹ Volver a resultados").performClick()
        composeRule.onNodeWithText("‹ Volver a Crear").performClick()
        composeRule.onNodeWithTag("save-routine-action").performClick()
        composeRule.waitUntil(5_000) { savedDraft != null }
        assertEquals(listOf("ACT-001"), savedDraft?.steps?.map { it.activityId })
        assertTrue(savedDraft?.steps?.all { it.futureVisualReference == null } == true)
        assertTrue(queries.all { it == "actividad" || it == "comer" })
    }

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
                    onSaved = {},
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
                    onSaved = {},
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

    @Test
    fun saveSuccessWaitsForPersistenceAndRapidTapsEmitOnlyOnce() {
        val gate = CompletableDeferred<Unit>()
        val writes = AtomicInteger(0)
        val confirmations = AtomicInteger(0)
        val highContrast = mutableStateOf(false)
        composeRule.setContent {
            RutinaGoTheme(highContrast = highContrast.value) {
                CreateScreen(
                    appMode = AppMode.SIMPLE,
                    catalogState = CatalogUiState.Ready(testCatalog(activityCount = 1, includeTemplate = true)),
                    suggestedTemplateId = "RUT-TEST",
                    onSaveRoutine = { draft ->
                        writes.incrementAndGet()
                        gate.await()
                        draft.toStoredRoutine()
                    },
                    onSaved = { confirmations.incrementAndGet() },
                )
            }
        }

        val saveAction = composeRule.onNodeWithTag("save-routine-action")
        saveAction.performClick()
        saveAction.performClick()
        composeRule.waitForIdle()
        assertEquals(1, writes.get())
        assertEquals(0, confirmations.get())
        saveAction.assertIsNotEnabled()

        gate.complete(Unit)
        composeRule.waitUntil(5_000) { confirmations.get() == 1 }
        composeRule.waitForIdle()
        composeRule.runOnUiThread { highContrast.value = true }
        composeRule.waitForIdle()
        saveAction.assertIsNotEnabled()
        assertEquals(1, confirmations.get())
        assertEquals(1, writes.get())
    }

    @Test
    fun failedSaveKeepsScratchDraftAndAllowsRetryWithoutFalseSuccess() {
        val attempts = AtomicInteger(0)
        val confirmations = AtomicInteger(0)
        var successfulDraft: UserRoutineDraft? = null
        composeRule.setContent {
            RutinaGoTheme {
                CreateScreen(
                    appMode = AppMode.SIMPLE,
                    catalogState = CatalogUiState.Ready(testCatalog(activityCount = 1)),
                    onSaveRoutine = { draft ->
                        if (attempts.incrementAndGet() == 1) error("Fallo de Room")
                        successfulDraft = draft
                        draft.toStoredRoutine()
                    },
                    onSaved = { confirmations.incrementAndGet() },
                )
            }
        }

        composeRule.onNodeWithText("Elegir origen").performClick()
        composeRule.onNodeWithText("Crear desde cero").performClick()
        composeRule.onNodeWithText("Nombre de la rutina").performTextInput("Mi borrador")
        composeRule.onNodeWithText("Actividad 01").performClick()
        val saveAction = composeRule.onNodeWithTag("save-routine-action")
        saveAction.performClick()
        composeRule.waitUntil(5_000) { attempts.get() == 1 }
        composeRule.waitForIdle()

        assertEquals(0, confirmations.get())
        composeRule.onNodeWithText("Mi borrador").assertIsDisplayed()
        composeRule.onAllNodesWithText("No se pudo guardar la rutina").onFirst().assertIsDisplayed()
        saveAction.assertIsEnabled().performClick()
        composeRule.waitUntil(5_000) { confirmations.get() == 1 }
        assertEquals("Mi borrador", successfulDraft?.name)
        assertEquals(listOf("ACT-001"), successfulDraft?.steps?.map { it.activityId })
        assertEquals(2, attempts.get())
    }

    @Test
    fun editReturnsStoredRoutineWithSameIdAndUpdatedName() {
        var savedDraft: UserRoutineDraft? = null
        var confirmed: UserRoutine? = null
        composeRule.setContent {
            RutinaGoTheme {
                UserRoutineEditorScreen(
                    routine = userRoutine("stable-id", "Antes"),
                    appMode = AppMode.SIMPLE,
                    catalogState = CatalogUiState.Ready(testCatalog(activityCount = 1)),
                    onBack = {},
                    onSaveRoutine = { draft ->
                        savedDraft = draft
                        draft.toStoredRoutine()
                    },
                    onSaved = { confirmed = it },
                )
            }
        }

        composeRule.onNodeWithText("Guardar cambios").assertIsDisplayed()
        composeRule.onNodeWithText("Nombre de la rutina").performTextInput(" editada")
        composeRule.onNodeWithTag("save-routine-action").performClick()
        composeRule.waitUntil(5_000) { confirmed != null }
        assertEquals("stable-id", savedDraft?.id)
        assertEquals("stable-id", confirmed?.id)
        assertTrue(confirmed?.name?.contains("editada") == true)
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
