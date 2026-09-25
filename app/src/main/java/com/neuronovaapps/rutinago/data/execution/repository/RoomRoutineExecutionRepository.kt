package com.neuronovaapps.rutinago.data.execution.repository

import androidx.room.withTransaction
import com.neuronovaapps.rutinago.data.catalog.local.RutinaGoDatabase
import com.neuronovaapps.rutinago.data.execution.local.entity.ExecutionStepEntity
import com.neuronovaapps.rutinago.data.execution.local.entity.RoutineExecutionEntity
import com.neuronovaapps.rutinago.data.execution.local.entity.RoutineExecutionWithSteps
import com.neuronovaapps.rutinago.domain.execution.ExecutionStatus
import com.neuronovaapps.rutinago.domain.execution.ExecutionStep
import com.neuronovaapps.rutinago.domain.execution.ExecutionStepStatus
import com.neuronovaapps.rutinago.domain.execution.ProfileProgress
import com.neuronovaapps.rutinago.domain.execution.RoutineExecution
import com.neuronovaapps.rutinago.domain.execution.RoutineExecutionRepository
import com.neuronovaapps.rutinago.domain.execution.RoutineExecutionValidationException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRoutineExecutionRepository(
    private val database: RutinaGoDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : RoutineExecutionRepository {
    private val executionDao = database.routineExecutionDao()
    private val routineDao = database.userRoutineDao()

    override fun activeExecution(profileId: String): Flow<RoutineExecution?> {
        requireProfile(profileId)
        return executionDao.observeActive(profileId).map { it?.toModel() }
    }

    override fun execution(executionId: String): Flow<RoutineExecution?> =
        executionDao.observeById(executionId).map { it?.toModel() }

    override fun progress(profileId: String): Flow<ProfileProgress> {
        requireProfile(profileId)
        return executionDao.observeHistory(profileId).map { rows ->
            val history = rows.map(RoutineExecutionWithSteps::toModel)
            ProfileProgress(
                completedRoutines = history.count { it.status == ExecutionStatus.COMPLETED },
                partialRoutines = history.count { it.status == ExecutionStatus.PARTIAL },
                completedActivities = history.sumOf(RoutineExecution::completedSteps),
                history = history,
            )
        }
    }

    override suspend fun start(profileId: String, userRoutineId: String): RoutineExecution {
        requireProfile(profileId)
        return database.withTransaction {
            executionDao.getActive(profileId)?.let { active ->
                if (active.execution.userRoutineId == userRoutineId) return@withTransaction active.toModel()
                throw RoutineExecutionValidationException(
                    "Ya hay una rutina en curso. Reanúdala o sal de ella antes de iniciar otra.",
                )
            }
            val routine = routineDao.getByIdForProfile(userRoutineId, profileId)
                ?: throw RoutineExecutionValidationException(
                    "La rutina no pertenece al perfil activo o ya no está disponible.",
                )
            if (routine.routine.archived) {
                throw RoutineExecutionValidationException("La rutina está archivada y no se puede iniciar.")
            }
            val sourceSteps = routineDao.getExecutionSteps(userRoutineId)
            if (sourceSteps.isEmpty()) {
                throw RoutineExecutionValidationException("La rutina no contiene actividades para ejecutar.")
            }
            val now = clock()
            val executionId = idFactory()
            executionDao.insertExecution(
                RoutineExecutionEntity(
                    executionId = executionId,
                    profileId = profileId,
                    userRoutineId = userRoutineId,
                    routineName = routine.routine.name,
                    startedAtEpochMillis = now,
                    finishedAtEpochMillis = null,
                    status = ExecutionStatus.IN_PROGRESS.name,
                    totalSteps = sourceSteps.size,
                    completedSteps = 0,
                    skippedSteps = 0,
                    currentStepPosition = sourceSteps.first().position,
                ),
            )
            executionDao.insertSteps(sourceSteps.map { step ->
                ExecutionStepEntity(
                    executionId = executionId,
                    position = step.position,
                    activityId = step.activityId,
                    activityName = step.activityName,
                    instruction = step.instruction,
                    visualReference = step.visualReference,
                    status = ExecutionStepStatus.PENDING.name,
                    performedAtEpochMillis = null,
                )
            })
            requireNotNull(executionDao.getByIdForProfile(executionId, profileId)).toModel()
        }
    }

    override suspend fun completeStep(
        profileId: String,
        executionId: String,
        position: Int,
    ): RoutineExecution = transitionStep(
        profileId,
        executionId,
        position,
        ExecutionStepStatus.COMPLETED,
    )

    override suspend fun skipStep(
        profileId: String,
        executionId: String,
        position: Int,
    ): RoutineExecution = transitionStep(
        profileId,
        executionId,
        position,
        ExecutionStepStatus.SKIPPED,
    )

    override suspend fun finishPartial(profileId: String, executionId: String): RoutineExecution {
        requireProfile(profileId)
        return database.withTransaction {
            val current = ownedExecution(profileId, executionId)
            if (current.execution.status != ExecutionStatus.IN_PROGRESS.name) return@withTransaction current.toModel()
            val completed = executionDao.stepCount(executionId, ExecutionStepStatus.COMPLETED.name)
            val skipped = executionDao.stepCount(executionId, ExecutionStepStatus.SKIPPED.name)
            executionDao.updateProgress(
                executionId = executionId,
                profileId = profileId,
                completedSteps = completed,
                skippedSteps = skipped,
                currentPosition = null,
                status = ExecutionStatus.PARTIAL.name,
                finishedAt = clock(),
            )
            ownedExecution(profileId, executionId).toModel()
        }
    }

    override suspend fun abandon(profileId: String, executionId: String): RoutineExecution {
        requireProfile(profileId)
        return database.withTransaction {
            val current = ownedExecution(profileId, executionId)
            if (current.execution.status != ExecutionStatus.IN_PROGRESS.name) return@withTransaction current.toModel()
            val completed = executionDao.stepCount(executionId, ExecutionStepStatus.COMPLETED.name)
            val skipped = executionDao.stepCount(executionId, ExecutionStepStatus.SKIPPED.name)
            executionDao.updateProgress(
                executionId = executionId,
                profileId = profileId,
                completedSteps = completed,
                skippedSteps = skipped,
                currentPosition = null,
                status = ExecutionStatus.ABANDONED.name,
                finishedAt = clock(),
            )
            ownedExecution(profileId, executionId).toModel()
        }
    }

    private suspend fun transitionStep(
        profileId: String,
        executionId: String,
        position: Int,
        newStatus: ExecutionStepStatus,
    ): RoutineExecution {
        requireProfile(profileId)
        return database.withTransaction {
            val current = ownedExecution(profileId, executionId)
            if (current.execution.status != ExecutionStatus.IN_PROGRESS.name) return@withTransaction current.toModel()
            val currentPosition = current.execution.currentStepPosition
            if (position != currentPosition) {
                throw RoutineExecutionValidationException("La actividad ya fue registrada o no es el paso actual.")
            }
            if (executionDao.transitionPendingStep(executionId, position, newStatus.name, clock()) == 0) {
                return@withTransaction ownedExecution(profileId, executionId).toModel()
            }
            val completed = executionDao.stepCount(executionId, ExecutionStepStatus.COMPLETED.name)
            val skipped = executionDao.stepCount(executionId, ExecutionStepStatus.SKIPPED.name)
            val next = executionDao.firstPendingPosition(executionId)
            val status = when {
                next != null -> ExecutionStatus.IN_PROGRESS
                skipped == 0 -> ExecutionStatus.COMPLETED
                else -> ExecutionStatus.PARTIAL
            }
            executionDao.updateProgress(
                executionId = executionId,
                profileId = profileId,
                completedSteps = completed,
                skippedSteps = skipped,
                currentPosition = next,
                status = status.name,
                finishedAt = if (status == ExecutionStatus.IN_PROGRESS) null else clock(),
            )
            ownedExecution(profileId, executionId).toModel()
        }
    }

    private suspend fun ownedExecution(profileId: String, executionId: String) =
        executionDao.getByIdForProfile(executionId, profileId)
            ?: throw RoutineExecutionValidationException("La ejecución no pertenece al perfil activo.")

    private fun requireProfile(profileId: String) {
        if (profileId.isBlank()) throw RoutineExecutionValidationException("Selecciona un perfil.")
    }
}

private fun RoutineExecutionWithSteps.toModel() = RoutineExecution(
    id = execution.executionId,
    profileId = execution.profileId,
    userRoutineId = execution.userRoutineId,
    routineName = execution.routineName,
    startedAtEpochMillis = execution.startedAtEpochMillis,
    finishedAtEpochMillis = execution.finishedAtEpochMillis,
    status = ExecutionStatus.valueOf(execution.status),
    totalSteps = execution.totalSteps,
    completedSteps = execution.completedSteps,
    skippedSteps = execution.skippedSteps,
    currentStepPosition = execution.currentStepPosition,
    steps = steps.sortedBy(ExecutionStepEntity::position).map { step ->
        ExecutionStep(
            position = step.position,
            activityId = step.activityId,
            activityName = step.activityName,
            instruction = step.instruction,
            visualReference = step.visualReference,
            status = ExecutionStepStatus.valueOf(step.status),
            performedAtEpochMillis = step.performedAtEpochMillis,
        )
    },
)
