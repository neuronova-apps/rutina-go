package com.neuronovaapps.rutinago.data.user.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.neuronovaapps.rutinago.data.user.local.entity.UserRoutineEntity
import com.neuronovaapps.rutinago.data.user.local.entity.UserRoutineExecutionStep
import com.neuronovaapps.rutinago.data.user.local.entity.UserRoutineStepEntity
import com.neuronovaapps.rutinago.data.user.local.entity.UserRoutineWithSteps
import kotlinx.coroutines.flow.Flow

@Dao
interface UserRoutineDao {
    @Query("SELECT DISTINCT future_visual_reference FROM user_routine_steps " +
        "WHERE future_visual_reference IS NOT NULL")
    suspend fun allVisualReferences(): List<String>

    @Transaction
    @Query(
        "SELECT * FROM user_routines " +
            "WHERE profile_id = :profileId AND archived = 0 " +
            "ORDER BY modified_at_epoch_millis DESC, name COLLATE NOCASE",
    )
    fun observeActiveByProfile(profileId: String): Flow<List<UserRoutineWithSteps>>

    @Transaction
    @Query(
        "SELECT * FROM user_routines " +
            "WHERE user_routine_id = :routineId AND profile_id = :profileId",
    )
    suspend fun getByIdForProfile(routineId: String, profileId: String): UserRoutineWithSteps?

    @Query("SELECT profile_id FROM user_routines WHERE user_routine_id = :routineId")
    suspend fun getOwnerProfileId(routineId: String): String?

    @Query(
        "SELECT s.position AS position, s.activity_id AS activity_id, " +
            "a.name AS activity_name, " +
            "COALESCE(s.custom_instruction, a.simple_instruction) AS instruction, " +
            "s.future_visual_reference AS visual_reference " +
            "FROM user_routine_steps s " +
            "INNER JOIN catalog_activities a ON a.activity_id = s.activity_id " +
            "WHERE s.user_routine_id = :routineId ORDER BY s.position",
    )
    suspend fun getExecutionSteps(routineId: String): List<UserRoutineExecutionStep>

    @Query("SELECT activity_id FROM catalog_activities WHERE activity_id IN (:activityIds)")
    suspend fun getExistingActivityIds(activityIds: List<String>): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM catalog_routines WHERE routine_id = :routineId)")
    suspend fun templateExists(routineId: String): Boolean

    @Upsert
    suspend fun upsertRoutine(routine: UserRoutineEntity)

    @Query("DELETE FROM user_routine_steps WHERE user_routine_id = :routineId")
    suspend fun deleteSteps(routineId: String)

    @Insert
    suspend fun insertSteps(steps: List<UserRoutineStepEntity>)

    @Query(
        "UPDATE user_routines SET archived = 1, modified_at_epoch_millis = :modifiedAt " +
            "WHERE user_routine_id = :routineId AND profile_id = :profileId",
    )
    suspend fun archive(routineId: String, profileId: String, modifiedAt: Long): Int

    @Query(
        "DELETE FROM user_routines " +
            "WHERE user_routine_id = :routineId AND profile_id = :profileId",
    )
    suspend fun deleteOwned(routineId: String, profileId: String): Int

    @Query("SELECT COUNT(*) FROM user_routine_steps WHERE user_routine_id = :routineId")
    suspend fun stepCount(routineId: String): Int

    @Query("SELECT COUNT(*) FROM user_routines WHERE profile_id = :profileId")
    suspend fun routineCount(profileId: String): Int
}
