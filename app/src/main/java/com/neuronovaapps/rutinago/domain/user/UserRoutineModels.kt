package com.neuronovaapps.rutinago.domain.user

data class UserRoutine(
    val id: String,
    val profileId: String,
    val sourceTemplateId: String?,
    val name: String,
    val description: String?,
    val createdAtEpochMillis: Long,
    val modifiedAtEpochMillis: Long,
    val archived: Boolean,
    val steps: List<UserRoutineStep>,
)

data class UserRoutineStep(
    val position: Int,
    val activityId: String,
    val customInstruction: String?,
    val futureVisualReference: String?,
)

data class UserRoutineDraft(
    val id: String,
    val sourceTemplateId: String?,
    val name: String,
    val description: String? = null,
    val steps: List<UserRoutineDraftStep>,
)

data class UserRoutineDraftStep(
    val activityId: String,
    val customInstruction: String? = null,
    val futureVisualReference: String? = null,
)

class UserRoutineValidationException(message: String) : IllegalArgumentException(message)

class UserRoutineDeletionBlockedException : IllegalStateException(
    "No puedes eliminar esta rutina mientras tiene una ejecución en curso.",
)
