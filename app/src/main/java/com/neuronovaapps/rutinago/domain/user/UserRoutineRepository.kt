package com.neuronovaapps.rutinago.domain.user

import kotlinx.coroutines.flow.Flow

interface UserRoutineRepository {
    fun activeRoutines(profileId: String): Flow<List<UserRoutine>>

    suspend fun save(profileId: String, draft: UserRoutineDraft): UserRoutine

    suspend fun archive(profileId: String, routineId: String)

    suspend fun delete(profileId: String, routineId: String)
}
