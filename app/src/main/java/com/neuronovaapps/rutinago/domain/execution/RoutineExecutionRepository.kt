package com.neuronovaapps.rutinago.domain.execution

import kotlinx.coroutines.flow.Flow

interface RoutineExecutionRepository {
    fun activeExecution(profileId: String): Flow<RoutineExecution?>

    fun execution(executionId: String): Flow<RoutineExecution?>

    fun progress(profileId: String): Flow<ProfileProgress>

    suspend fun start(profileId: String, userRoutineId: String): RoutineExecution

    suspend fun completeStep(profileId: String, executionId: String, position: Int): RoutineExecution

    suspend fun skipStep(profileId: String, executionId: String, position: Int): RoutineExecution

    suspend fun finishPartial(profileId: String, executionId: String): RoutineExecution

    suspend fun abandon(profileId: String, executionId: String): RoutineExecution
}
