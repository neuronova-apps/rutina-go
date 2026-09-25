package com.neuronovaapps.rutinago

import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant
import com.neuronovaapps.rutinago.data.preferences.AppMode
import com.neuronovaapps.rutinago.domain.catalog.CatalogActivity
import com.neuronovaapps.rutinago.domain.catalog.CatalogCategory
import com.neuronovaapps.rutinago.domain.catalog.CatalogImportState
import com.neuronovaapps.rutinago.domain.catalog.CatalogRoutine
import com.neuronovaapps.rutinago.domain.catalog.CatalogRoutineStep
import com.neuronovaapps.rutinago.domain.catalog.CatalogSnapshot
import com.neuronovaapps.rutinago.domain.catalog.CatalogUiState
import com.neuronovaapps.rutinago.domain.catalog.activitiesFor
import com.neuronovaapps.rutinago.domain.catalog.catalogUiState
import com.neuronovaapps.rutinago.domain.catalog.catalogVariant
import com.neuronovaapps.rutinago.domain.catalog.routinesFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogPresentationTest {
    @Test
    fun simpleCompleteAndOptionalSequencesStayOrderedAndSeparate() {
        val snapshot = fixtureSnapshot()

        assertEquals(
            listOf("ACT-001", "ACT-002"),
            snapshot.steps("RUT-001", RoutineVariant.SIMPLE).map { it.activityId },
        )
        assertEquals(
            listOf("ACT-001", "ACT-003", "ACT-002"),
            snapshot.steps("RUT-001", RoutineVariant.COMPLETE).map { it.activityId },
        )
        assertEquals(
            listOf("ACT-004"),
            snapshot.steps("RUT-001", RoutineVariant.OPTIONAL).map { it.activityId },
        )
    }

    @Test
    fun stepPresentationUsesOnlyRealReferencedActivities() {
        val snapshot = fixtureSnapshot()
        val knownIds = snapshot.activities.map { it.id }.toSet()
        val presented = RoutineVariant.entries.flatMap { variant ->
            snapshot.activitiesFor("RUT-001", variant)
        }

        assertTrue(presented.all { it.step.activityId == it.activity.id })
        assertTrue(presented.all { it.activity.id in knownIds })
    }

    @Test
    fun realCategoriesAreIndependentFromSuggestedMoments() {
        val snapshot = fixtureSnapshot()

        assertEquals(listOf("Inicio del día"), snapshot.routinesFor("", "Inicio del día").map { it.primaryArea })
        assertTrue(snapshot.routinesFor("", "Mañana").isEmpty())
    }

    @Test
    fun routineAndActivitySearchUseMasterContent() {
        val snapshot = fixtureSnapshot()

        assertEquals("RUT-001", snapshot.routinesFor("lavarse", null).single().id)
        assertEquals("ACT-003", snapshot.activitiesFor("dientes", null).single().id)
    }

    @Test
    fun changingModeSelectsASequenceWithoutDuplicatingCatalogRecords() {
        val snapshot = fixtureSnapshot()
        val routineCount = snapshot.routines.size
        val activityCount = snapshot.activities.size

        assertEquals(RoutineVariant.SIMPLE, AppMode.SIMPLE.catalogVariant())
        assertEquals(RoutineVariant.COMPLETE, AppMode.ADVANCED.catalogVariant())
        assertEquals(routineCount, snapshot.routines.size)
        assertEquals(activityCount, snapshot.activities.size)
        assertEquals(activityCount, snapshot.activities.map { it.id }.toSet().size)
    }

    @Test
    fun failedImportProducesAnExplicitErrorInsteadOfEmptyReadyContent() {
        val state = catalogUiState(
            CatalogImportState.Failed("1.0.0", "Fallo de validación"),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
        )

        assertTrue(state is CatalogUiState.Error)
        assertEquals("Fallo de validación", (state as CatalogUiState.Error).message)
    }

    @Test
    fun authorizedCountsProduceReadyCatalog() {
        val categories = (1..17).map { CatalogCategory("Categoría $it", it) }
        val activities = (1..203).map { activity("ACT-${it.toString().padStart(3, '0')}", "Actividad $it") }
        val routines = (1..52).map { routine("RUT-${it.toString().padStart(3, '0')}", "Rutina $it") }
        val steps = (1..490).map { index ->
            CatalogRoutineStep(
                routineId = routines[(index - 1) % routines.size].id,
                activityId = activities[(index - 1) % activities.size].id,
                variant = RoutineVariant.COMPLETE,
                position = index,
            )
        }

        val state = catalogUiState(
            CatalogImportState.Ready("1.0.0"),
            categories,
            activities,
            routines,
            steps,
        )

        assertTrue(state is CatalogUiState.Ready)
        val catalog = (state as CatalogUiState.Ready).catalog
        assertEquals(17, catalog.categories.size)
        assertEquals(203, catalog.activities.size)
        assertEquals(52, catalog.routines.size)
        assertEquals(490, catalog.steps.size)
    }

    private fun fixtureSnapshot(): CatalogSnapshot {
        val activities = listOf(
            activity("ACT-001", "Despertarse"),
            activity("ACT-002", "Vestirse"),
            activity("ACT-003", "Lavarse los dientes"),
            activity("ACT-004", "Preparar la mochila"),
        )
        val steps = listOf(
            step("ACT-002", RoutineVariant.SIMPLE, 2),
            step("ACT-001", RoutineVariant.SIMPLE, 1),
            step("ACT-002", RoutineVariant.COMPLETE, 3),
            step("ACT-001", RoutineVariant.COMPLETE, 1),
            step("ACT-003", RoutineVariant.COMPLETE, 2),
            step("ACT-004", RoutineVariant.OPTIONAL, 1),
        )
        return CatalogSnapshot(
            categories = listOf(CatalogCategory("Inicio del día", 1)),
            activities = activities,
            routines = listOf(routine("RUT-001", "Prepararse para el colegio")),
            steps = steps,
        )
    }

    private fun activity(id: String, name: String) = CatalogActivity(
        id = id,
        category = "Inicio del día",
        name = name,
        simpleInstruction = "Instrucción de $name",
        arasaacSearchTerm = name,
        ageGroup = "General",
        autonomyLevel = "Variable",
        supervision = "Según necesidad",
        simpleMode = true,
        advancedMode = true,
        visualResource = "Pendiente",
        status = "Activo",
        observations = null,
    )

    private fun routine(id: String, name: String) = CatalogRoutine(
        id = id,
        name = name,
        primaryArea = "Inicio del día",
        context = "Hogar",
        ageGroup = "General",
        functionalObjective = "Preparar la autonomía",
        suggestedMoment = "Mañana",
        simpleStepCount = 2,
        completeStepCount = 3,
        personalizable = true,
        keySupervision = "Según necesidad",
        status = "Activo",
        observations = null,
    )

    private fun step(activityId: String, variant: RoutineVariant, position: Int) =
        CatalogRoutineStep("RUT-001", activityId, variant, position)
}
