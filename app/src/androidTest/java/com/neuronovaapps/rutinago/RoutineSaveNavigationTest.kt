package com.neuronovaapps.rutinago

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant
import com.neuronovaapps.rutinago.data.preferences.AppMode
import com.neuronovaapps.rutinago.data.preferences.UserPreferences
import com.neuronovaapps.rutinago.data.preferences.UserProfile
import com.neuronovaapps.rutinago.domain.catalog.CatalogActivity
import com.neuronovaapps.rutinago.domain.catalog.CatalogRoutine
import com.neuronovaapps.rutinago.domain.catalog.CatalogRoutineStep
import com.neuronovaapps.rutinago.domain.catalog.CatalogSnapshot
import com.neuronovaapps.rutinago.domain.catalog.CatalogUiState
import com.neuronovaapps.rutinago.domain.execution.ProfileProgress
import com.neuronovaapps.rutinago.domain.user.UserRoutine
import com.neuronovaapps.rutinago.domain.user.UserRoutineDraft
import com.neuronovaapps.rutinago.domain.user.UserRoutineStep
import com.neuronovaapps.rutinago.ui.navigation.RutinaGoApp
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RoutineSaveNavigationTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun scratchSaveConfirmsAboveNavigationAndAppearsInMyRoutinesImmediately() {
        var savedId: String? = null
        showApp(onSave = { draft ->
            savedId = draft.id
            draft.toRoutine()
        })

        composeRule.onNodeWithTag("destination-CREATE").performClick()
        composeRule.onNodeWithText("Elegir origen").performClick()
        composeRule.onNodeWithText("Crear desde cero").performClick()
        composeRule.onNodeWithText("Nombre de la rutina").performTextInput("Mi nueva rutina")
        composeRule.onNodeWithText("Actividad 01").performClick()
        composeRule.onNodeWithTag("save-routine-action").performClick()

        composeRule.waitUntil(5_000) { savedId != null }
        composeRule.onNodeWithText("Rutina guardada correctamente").assertIsDisplayed()
        composeRule.onNodeWithText("Mis rutinas").assertIsDisplayed()
        composeRule.onNodeWithText("Mi nueva rutina").assertIsDisplayed()
        composeRule.onNodeWithTag("routine-save-confirmation")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun templateFromRoutinesUsesTheSameSuccessMessage() {
        showApp()

        composeRule.onNodeWithTag("destination-ROUTINES").performClick()
        composeRule.onNodeWithText("Usar como plantilla").performScrollTo().performClick()
        composeRule.onNodeWithText("Personalizar plantilla").assertIsDisplayed()
        composeRule.onNodeWithTag("save-routine-action").performClick()

        composeRule.onNodeWithText("Rutina guardada correctamente").assertIsDisplayed()
        composeRule.onNodeWithText("Perfil Pruebas · 1 guardadas").assertIsDisplayed()
    }

    @Test
    fun templateFromCreateUsesTheSameSuccessMessage() {
        showApp()

        composeRule.onNodeWithTag("destination-CREATE").performClick()
        composeRule.onNodeWithText("Elegir origen").performClick()
        composeRule.onNodeWithText("Usar una plantilla").performClick()
        composeRule.onNodeWithText("Plantilla de prueba").performClick()
        composeRule.onNodeWithText("Personalizar plantilla").assertIsDisplayed()
        composeRule.onNodeWithTag("save-routine-action").performClick()

        composeRule.onNodeWithText("Rutina guardada correctamente").assertIsDisplayed()
        composeRule.onNodeWithText("Perfil Pruebas · 1 guardadas").assertIsDisplayed()
    }

    @Test
    fun editSaveShowsDistinctMessageAndUpdatedRoutineImmediately() {
        var savedDraft: UserRoutineDraft? = null
        showApp(initial = listOf(existingRoutine()), onSave = { draft ->
            savedDraft = draft
            draft.toRoutine()
        })

        composeRule.onNodeWithTag("destination-ROUTINES").performClick()
        composeRule.onNodeWithText("Editar").performClick()
        composeRule.onNodeWithText("Nombre de la rutina").performTextInput(" editada")
        composeRule.onNodeWithTag("save-routine-action").performClick()

        composeRule.waitUntil(5_000) { savedDraft != null }
        assertEquals("stable-id", savedDraft?.id)
        composeRule.onNodeWithText("Cambios guardados correctamente").assertIsDisplayed()
        composeRule.onNodeWithText("Antes editada").assertIsDisplayed()
    }

    private fun showApp(
        initial: List<UserRoutine> = emptyList(),
        onSave: suspend (UserRoutineDraft) -> UserRoutine = { it.toRoutine() },
    ) {
        composeRule.setContent {
            RutinaGoTheme {
                RutinaGoApp(
                    preferences = UserPreferences(
                        appMode = AppMode.SIMPLE,
                        profiles = listOf(UserProfile("profile-1", "Pruebas")),
                        activeProfileId = "profile-1",
                    ),
                    catalogState = CatalogUiState.Ready(catalog()),
                    userRoutines = initial,
                    activeExecution = null,
                    progress = ProfileProgress(),
                    onSaveUserRoutine = onSave,
                    onArchiveUserRoutine = {},
                    onStartExecution = { error("No se espera iniciar una ejecución") },
                    onLoadExecution = { null },
                    onCompleteExecutionStep = { _, _ -> error("No se espera ejecutar") },
                    onSkipExecutionStep = { _, _ -> error("No se espera ejecutar") },
                    onFinishPartialExecution = { error("No se espera ejecutar") },
                    onAbandonExecution = { error("No se espera ejecutar") },
                    onThemeModeChange = {},
                    onTextScaleModeChange = {},
                    onHighContrastChange = {},
                    onAppModeChange = {},
                    onCreateProfile = {},
                    onRenameProfile = { _, _ -> },
                    onSelectProfile = {},
                )
            }
        }
        composeRule.onNodeWithText("Comenzar").performClick()
    }

    private fun catalog() = CatalogSnapshot(
        categories = emptyList(),
        activities = listOf(
            CatalogActivity(
                id = "ACT-001",
                category = "Autonomía",
                name = "Actividad 01",
                simpleInstruction = "Completa la actividad",
                arasaacSearchTerm = "actividad",
                ageGroup = "Todas",
                autonomyLevel = "Variable",
                supervision = "Según necesidad",
                simpleMode = true,
                advancedMode = true,
                visualResource = "pending",
                status = "active",
                observations = null,
            ),
        ),
        routines = listOf(
            CatalogRoutine(
                id = "RUT-TEST",
                name = "Plantilla de prueba",
                primaryArea = "Autonomía",
                context = "Prueba",
                ageGroup = "Todas",
                functionalObjective = "Validar el guardado",
                suggestedMoment = "Cualquier momento",
                simpleStepCount = 1,
                completeStepCount = 1,
                personalizable = true,
                keySupervision = "Según necesidad",
                status = "active",
                observations = null,
            ),
        ),
        steps = listOf(
            CatalogRoutineStep("RUT-TEST", "ACT-001", RoutineVariant.SIMPLE, 1),
        ),
    )

    private fun existingRoutine() = UserRoutine(
        id = "stable-id",
        profileId = "profile-1",
        sourceTemplateId = null,
        name = "Antes",
        description = null,
        createdAtEpochMillis = 1,
        modifiedAtEpochMillis = 1,
        archived = false,
        steps = listOf(UserRoutineStep(1, "ACT-001", null, null)),
    )

    private fun UserRoutineDraft.toRoutine() = UserRoutine(
        id = id,
        profileId = "profile-1",
        sourceTemplateId = sourceTemplateId,
        name = name,
        description = description,
        createdAtEpochMillis = 1,
        modifiedAtEpochMillis = 2,
        archived = false,
        steps = steps.mapIndexed { index, step ->
            UserRoutineStep(index + 1, step.activityId, step.customInstruction, step.futureVisualReference)
        },
    )
}
