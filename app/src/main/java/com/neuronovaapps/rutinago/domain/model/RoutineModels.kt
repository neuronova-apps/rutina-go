package com.neuronovaapps.rutinago.domain.model

enum class RoutineCategory(val label: String) {
    ALL("Todas"),
    MORNING("Mañana"),
    AFTERNOON("Tarde"),
    NIGHT("Noche"),
    AS_NEEDED("Según necesidad"),
}

data class ActivityStep(
    val id: String,
    val title: String,
    val instruction: String,
    val visualLabel: String,
)

data class Routine(
    val id: String,
    val title: String,
    val category: RoutineCategory,
    val steps: List<ActivityStep>,
    val scheduleLabel: String,
    val isTemplate: Boolean = false,
)

enum class RoutineOutcome(val label: String) {
    COMPLETED("Completada"),
    PARTIAL("Parcial"),
    CANCELLED("Cancelada"),
}

data class ProgressRecord(
    val routineTitle: String,
    val whenLabel: String,
    val outcome: RoutineOutcome,
)

fun List<Routine>.filterFor(query: String, category: RoutineCategory): List<Routine> {
    val normalizedQuery = query.trim()
    return filter { routine ->
        val matchesCategory = category == RoutineCategory.ALL || routine.category == category
        val matchesQuery = normalizedQuery.isBlank() ||
            routine.title.contains(normalizedQuery, ignoreCase = true) ||
            routine.steps.any { it.title.contains(normalizedQuery, ignoreCase = true) }
        matchesCategory && matchesQuery
    }
}
