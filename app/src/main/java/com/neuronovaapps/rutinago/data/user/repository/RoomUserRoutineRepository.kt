package com.neuronovaapps.rutinago.data.user.repository

import androidx.room.withTransaction
import com.neuronovaapps.rutinago.data.catalog.local.RutinaGoDatabase
import com.neuronovaapps.rutinago.data.user.local.entity.UserRoutineEntity
import com.neuronovaapps.rutinago.data.user.local.entity.UserRoutineStepEntity
import com.neuronovaapps.rutinago.data.user.local.entity.UserRoutineWithSteps
import com.neuronovaapps.rutinago.domain.user.UserRoutine
import com.neuronovaapps.rutinago.domain.user.UserRoutineDraft
import com.neuronovaapps.rutinago.domain.user.UserRoutineDeletionBlockedException
import com.neuronovaapps.rutinago.domain.user.UserRoutineRepository
import com.neuronovaapps.rutinago.domain.user.UserRoutineStep
import com.neuronovaapps.rutinago.domain.user.UserRoutineValidationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomUserRoutineRepository(
    private val database: RutinaGoDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : UserRoutineRepository {
    private val dao = database.userRoutineDao()

    override fun activeRoutines(profileId: String): Flow<List<UserRoutine>> {
        requireProfileId(profileId)
        return dao.observeActiveByProfile(profileId).map { routines ->
            routines.map(UserRoutineWithSteps::toModel)
        }
    }

    override suspend fun save(profileId: String, draft: UserRoutineDraft): UserRoutine {
        requireProfileId(profileId)
        val routineId = draft.id.trim()
        val name = draft.name.trim()
        if (routineId.isEmpty()) throw UserRoutineValidationException("El borrador no tiene un ID válido.")
        if (name.isEmpty()) throw UserRoutineValidationException("Escribe un nombre para la rutina.")
        if (draft.steps.isEmpty()) {
            throw UserRoutineValidationException("Selecciona al menos una actividad.")
        }
        val activityIds = draft.steps.map { it.activityId.trim() }
        if (activityIds.any(String::isEmpty) || activityIds.size != activityIds.toSet().size) {
            throw UserRoutineValidationException("La secuencia contiene actividades inválidas o duplicadas.")
        }
        val sourceTemplateId = draft.sourceTemplateId?.trim()?.takeIf(String::isNotEmpty)

        return database.withTransaction {
            val owner = dao.getOwnerProfileId(routineId)
            if (owner != null && owner != profileId) {
                throw UserRoutineValidationException("La rutina pertenece a otro perfil.")
            }
            if (sourceTemplateId != null && !dao.templateExists(sourceTemplateId)) {
                throw UserRoutineValidationException("La plantilla de origen ya no está disponible.")
            }
            val existingIds = dao.getExistingActivityIds(activityIds).toSet()
            val missingIds = activityIds.filterNot(existingIds::contains)
            if (missingIds.isNotEmpty()) {
                throw UserRoutineValidationException("La secuencia contiene actividades que ya no existen.")
            }

            val existing = dao.getByIdForProfile(routineId, profileId)
            val now = clock()
            val entity = UserRoutineEntity(
                userRoutineId = routineId,
                profileId = profileId,
                sourceTemplateId = sourceTemplateId,
                name = name,
                description = draft.description?.trim()?.takeIf(String::isNotEmpty),
                createdAtEpochMillis = existing?.routine?.createdAtEpochMillis ?: now,
                modifiedAtEpochMillis = now,
                archived = false,
            )
            dao.upsertRoutine(entity)
            dao.deleteSteps(routineId)
            dao.insertSteps(
                draft.steps.mapIndexed { index, step ->
                    UserRoutineStepEntity(
                        userRoutineId = routineId,
                        position = index + 1,
                        activityId = step.activityId.trim(),
                        customInstruction = step.customInstruction?.trim()?.takeIf(String::isNotEmpty),
                        futureVisualReference = step.futureVisualReference
                            ?.trim()?.takeIf(String::isNotEmpty),
                    )
                },
            )
            requireNotNull(dao.getByIdForProfile(routineId, profileId)).toModel()
        }
    }

    override suspend fun archive(profileId: String, routineId: String) {
        requireProfileId(profileId)
        if (dao.archive(routineId, profileId, clock()) != 1) {
            throw UserRoutineValidationException("No se encontró la rutina en el perfil activo.")
        }
    }

    override suspend fun delete(profileId: String, routineId: String) {
        requireProfileId(profileId)
        if (routineId.isBlank()) {
            throw UserRoutineValidationException("No se encontró la rutina en el perfil activo.")
        }
        database.withTransaction {
            if (database.routineExecutionDao().hasInProgressForRoutine(profileId, routineId)) {
                throw UserRoutineDeletionBlockedException()
            }
            if (dao.deleteOwned(routineId, profileId) != 1) {
                throw UserRoutineValidationException("No se encontró la rutina en el perfil activo.")
            }
            // Room's existing CASCADE removes only this definition's personal steps.
            // Execution snapshots have no FK to user_routines and remain in history.
        }
    }

    private fun requireProfileId(profileId: String) {
        if (profileId.isBlank()) {
            throw UserRoutineValidationException("Selecciona un perfil antes de guardar.")
        }
    }
}

private fun UserRoutineWithSteps.toModel() = UserRoutine(
    id = routine.userRoutineId,
    profileId = routine.profileId,
    sourceTemplateId = routine.sourceTemplateId,
    name = routine.name,
    description = routine.description,
    createdAtEpochMillis = routine.createdAtEpochMillis,
    modifiedAtEpochMillis = routine.modifiedAtEpochMillis,
    archived = routine.archived,
    steps = steps.sortedBy(UserRoutineStepEntity::position).map { step ->
        UserRoutineStep(
            position = step.position,
            activityId = step.activityId,
            customInstruction = step.customInstruction,
            futureVisualReference = step.futureVisualReference,
        )
    },
)
