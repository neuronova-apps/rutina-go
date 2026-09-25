package com.neuronovaapps.rutinago.domain.execution

enum class ExecutionStatus(val label: String) {
    IN_PROGRESS("En curso"),
    COMPLETED("Completada"),
    PARTIAL("Parcial"),
    ABANDONED("Abandonada"),
}

enum class ExecutionStepStatus {
    PENDING,
    COMPLETED,
    SKIPPED,
}

data class ExecutionStep(
    val position: Int,
    val activityId: String,
    val activityName: String,
    val instruction: String,
    val visualReference: String?,
    val status: ExecutionStepStatus,
    val performedAtEpochMillis: Long?,
)

data class RoutineExecution(
    val id: String,
    val profileId: String,
    val userRoutineId: String,
    val routineName: String,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long?,
    val status: ExecutionStatus,
    val totalSteps: Int,
    val completedSteps: Int,
    val skippedSteps: Int,
    val currentStepPosition: Int?,
    val steps: List<ExecutionStep>,
) {
    val currentStep: ExecutionStep?
        get() = currentStepPosition?.let { position -> steps.firstOrNull { it.position == position } }
}

data class ProfileProgress(
    val completedRoutines: Int = 0,
    val partialRoutines: Int = 0,
    val completedActivities: Int = 0,
    val history: List<RoutineExecution> = emptyList(),
)

class RoutineExecutionValidationException(message: String) : IllegalArgumentException(message)
