package com.neuronovaapps.rutinago.domain.catalog

import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant
import com.neuronovaapps.rutinago.data.preferences.AppMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

sealed interface CatalogUiState {
    data object Loading : CatalogUiState

    data class Ready(val catalog: CatalogSnapshot) : CatalogUiState

    data class Error(val message: String) : CatalogUiState
}

data class CatalogSnapshot(
    val categories: List<CatalogCategory>,
    val activities: List<CatalogActivity>,
    val routines: List<CatalogRoutine>,
    val steps: List<CatalogRoutineStep>,
) {
    private val activitiesById = activities.associateBy(CatalogActivity::id)
    private val routinesById = routines.associateBy(CatalogRoutine::id)
    private val stepsByRoutine = steps.groupBy(CatalogRoutineStep::routineId)

    fun routine(routineId: String?): CatalogRoutine? = routinesById[routineId]

    fun activity(activityId: String): CatalogActivity? = activitiesById[activityId]

    fun steps(routineId: String, variant: RoutineVariant): List<CatalogRoutineStep> =
        stepsByRoutine[routineId].orEmpty()
            .filter { it.variant == variant }
            .sortedBy(CatalogRoutineStep::position)

    fun activitiesFor(
        routineId: String,
        variant: RoutineVariant,
    ): List<CatalogStepActivity> = steps(routineId, variant).mapNotNull { step ->
        activity(step.activityId)?.let { CatalogStepActivity(step, it) }
    }
}

data class CatalogStepActivity(
    val step: CatalogRoutineStep,
    val activity: CatalogActivity,
)

fun CatalogSnapshot.routinesFor(query: String, category: String?): List<CatalogRoutine> {
    val normalizedQuery = query.trim()
    return routines.filter { routine ->
        val matchesCategory = category == null || routine.primaryArea == category
        val matchesQuery = normalizedQuery.isBlank() ||
            routine.name.contains(normalizedQuery, ignoreCase = true) ||
            routine.functionalObjective.contains(normalizedQuery, ignoreCase = true) ||
            routine.context.contains(normalizedQuery, ignoreCase = true) ||
            stepsByRoutineActivityNames(routine.id).any {
                it.contains(normalizedQuery, ignoreCase = true)
            }
        matchesCategory && matchesQuery
    }
}

fun CatalogSnapshot.activitiesFor(query: String, category: String?): List<CatalogActivity> {
    val normalizedQuery = query.trim()
    return activities.filter { activity ->
        val matchesCategory = category == null || activity.category == category
        val matchesQuery = normalizedQuery.isBlank() ||
            activity.name.contains(normalizedQuery, ignoreCase = true) ||
            activity.simpleInstruction.contains(normalizedQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }
}

fun AppMode.catalogVariant(): RoutineVariant = when (this) {
    AppMode.SIMPLE -> RoutineVariant.SIMPLE
    AppMode.ADVANCED -> RoutineVariant.COMPLETE
}

fun observeCatalogUiState(
    importState: Flow<CatalogImportState>,
    repository: CatalogRepository,
): Flow<CatalogUiState> = combine(
    importState,
    repository.categories(),
    repository.activities(),
    repository.routines(),
    repository.routineSteps(),
) { state, categories, activities, routines, steps ->
    catalogUiState(state, categories, activities, routines, steps)
}.catch { error ->
    emit(CatalogUiState.Error(error.message ?: "No se pudo leer el catálogo local."))
}

internal fun catalogUiState(
    importState: CatalogImportState,
    categories: List<CatalogCategory>,
    activities: List<CatalogActivity>,
    routines: List<CatalogRoutine>,
    steps: List<CatalogRoutineStep>,
): CatalogUiState = when (importState) {
    CatalogImportState.Missing,
    is CatalogImportState.UpdateAvailable,
    -> CatalogUiState.Loading

    is CatalogImportState.Failed -> CatalogUiState.Error(
        importState.message?.takeIf(String::isNotBlank)
            ?: "La importación del catálogo no se completó.",
    )

    is CatalogImportState.Incompatible -> CatalogUiState.Error(
        "El catálogo ${importState.catalogVersion} no es compatible con esta versión de RutinaGo.",
    )

    is CatalogImportState.Ready -> {
        val countsAreValid = categories.size == EXPECTED_CATEGORIES &&
            activities.size == EXPECTED_ACTIVITIES &&
            routines.size == EXPECTED_ROUTINES &&
            steps.size == EXPECTED_STEPS
        if (countsAreValid) {
            CatalogUiState.Ready(CatalogSnapshot(categories, activities, routines, steps))
        } else {
            CatalogUiState.Error(
                "El catálogo local está incompleto. Cierra y vuelve a abrir RutinaGo para reintentar.",
            )
        }
    }
}

private fun CatalogSnapshot.stepsByRoutineActivityNames(routineId: String): List<String> =
    RoutineVariant.entries.flatMap { variant ->
        activitiesFor(routineId, variant).map { it.activity.name }
    }

private const val EXPECTED_CATEGORIES = 17
private const val EXPECTED_ACTIVITIES = 203
private const val EXPECTED_ROUTINES = 52
private const val EXPECTED_STEPS = 490
