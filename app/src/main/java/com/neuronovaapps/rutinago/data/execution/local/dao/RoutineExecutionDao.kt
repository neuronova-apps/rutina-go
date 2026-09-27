package com.neuronovaapps.rutinago.data.execution.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.neuronovaapps.rutinago.data.execution.local.entity.ExecutionStepEntity
import com.neuronovaapps.rutinago.data.execution.local.entity.RoutineExecutionEntity
import com.neuronovaapps.rutinago.data.execution.local.entity.RoutineExecutionWithSteps
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineExecutionDao {
    @Query("SELECT DISTINCT visual_reference FROM execution_steps " +
        "WHERE visual_reference IS NOT NULL")
    suspend fun allVisualReferences(): List<String>

    @Transaction
    @Query(
        "SELECT * FROM routine_executions " +
            "WHERE profile_id = :profileId AND status = 'IN_PROGRESS' " +
            "ORDER BY started_at_epoch_millis DESC LIMIT 1",
    )
    fun observeActive(profileId: String): Flow<RoutineExecutionWithSteps?>

    @Transaction
    @Query("SELECT * FROM routine_executions WHERE execution_id = :executionId")
    fun observeById(executionId: String): Flow<RoutineExecutionWithSteps?>

    @Transaction
    @Query(
        "SELECT * FROM routine_executions WHERE profile_id = :profileId " +
            "ORDER BY started_at_epoch_millis DESC",
    )
    fun observeHistory(profileId: String): Flow<List<RoutineExecutionWithSteps>>

    @Transaction
    @Query(
        "SELECT * FROM routine_executions " +
            "WHERE profile_id = :profileId AND status = 'IN_PROGRESS' " +
            "ORDER BY started_at_epoch_millis DESC LIMIT 1",
    )
    suspend fun getActive(profileId: String): RoutineExecutionWithSteps?

    @Transaction
    @Query(
        "SELECT * FROM routine_executions " +
            "WHERE execution_id = :executionId AND profile_id = :profileId",
    )
    suspend fun getByIdForProfile(executionId: String, profileId: String): RoutineExecutionWithSteps?

    @Query(
        "SELECT EXISTS(SELECT 1 FROM routine_executions " +
            "WHERE profile_id = :profileId AND user_routine_id = :routineId " +
            "AND status = 'IN_PROGRESS')",
    )
    suspend fun hasInProgressForRoutine(profileId: String, routineId: String): Boolean

    @Insert
    suspend fun insertExecution(execution: RoutineExecutionEntity)

    @Insert
    suspend fun insertSteps(steps: List<ExecutionStepEntity>)

    @Query(
        "UPDATE execution_steps SET status = :newStatus, performed_at_epoch_millis = :performedAt " +
            "WHERE execution_id = :executionId AND position = :position AND status = 'PENDING'",
    )
    suspend fun transitionPendingStep(
        executionId: String,
        position: Int,
        newStatus: String,
        performedAt: Long,
    ): Int

    @Query(
        "SELECT position FROM execution_steps " +
            "WHERE execution_id = :executionId AND status = 'PENDING' " +
            "ORDER BY position LIMIT 1",
    )
    suspend fun firstPendingPosition(executionId: String): Int?

    @Query(
        "SELECT COUNT(*) FROM execution_steps " +
            "WHERE execution_id = :executionId AND status = :status",
    )
    suspend fun stepCount(executionId: String, status: String): Int

    @Query(
        "UPDATE routine_executions SET completed_steps = :completedSteps, " +
            "skipped_steps = :skippedSteps, current_step_position = :currentPosition, " +
            "status = :status, finished_at_epoch_millis = :finishedAt " +
            "WHERE execution_id = :executionId AND profile_id = :profileId AND status = 'IN_PROGRESS'",
    )
    suspend fun updateProgress(
        executionId: String,
        profileId: String,
        completedSteps: Int,
        skippedSteps: Int,
        currentPosition: Int?,
        status: String,
        finishedAt: Long?,
    ): Int
}
