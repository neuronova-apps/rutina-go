package com.neuronovaapps.rutinago.data.catalog.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogAchievementEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogActivityEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogCategoryEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogChallengeEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogReinforcementMessageEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogRoutineEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogRoutineStepEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogReadDao {
    @Query("SELECT * FROM catalog_categories ORDER BY sort_order")
    fun observeCategories(): Flow<List<CatalogCategoryEntity>>

    @Query(
        "SELECT * FROM catalog_activities " +
            "WHERE category_name = :category ORDER BY name COLLATE NOCASE",
    )
    fun observeActivitiesByCategory(category: String): Flow<List<CatalogActivityEntity>>

    @Query("SELECT * FROM catalog_activities WHERE activity_id = :activityId")
    fun observeActivityById(activityId: String): Flow<CatalogActivityEntity?>

    @Query("SELECT * FROM catalog_activities WHERE activity_id = :activityId")
    suspend fun getActivityById(activityId: String): CatalogActivityEntity?

    @Query(
        "SELECT * FROM catalog_routines " +
            "WHERE primary_area = :category ORDER BY name COLLATE NOCASE",
    )
    fun observeRoutinesByCategory(category: String): Flow<List<CatalogRoutineEntity>>

    @Query("SELECT * FROM catalog_routines WHERE routine_id = :routineId")
    fun observeRoutineById(routineId: String): Flow<CatalogRoutineEntity?>

    @Query("SELECT * FROM catalog_routines WHERE routine_id = :routineId")
    suspend fun getRoutineById(routineId: String): CatalogRoutineEntity?

    @Query(
        "SELECT * FROM catalog_routine_steps " +
            "WHERE routine_id = :routineId ORDER BY variant, position",
    )
    fun observeRoutineSteps(routineId: String): Flow<List<CatalogRoutineStepEntity>>

    @Query(
        "SELECT * FROM catalog_routine_steps " +
            "WHERE routine_id = :routineId AND variant = :variant ORDER BY position",
    )
    fun observeRoutineSteps(
        routineId: String,
        variant: RoutineVariant,
    ): Flow<List<CatalogRoutineStepEntity>>

    @Query(
        "SELECT * FROM catalog_routine_steps " +
            "WHERE routine_id = :routineId AND variant = :variant ORDER BY position",
    )
    suspend fun getRoutineSteps(
        routineId: String,
        variant: RoutineVariant,
    ): List<CatalogRoutineStepEntity>

    @Query("SELECT * FROM catalog_achievements ORDER BY achievement_id")
    fun observeAchievements(): Flow<List<CatalogAchievementEntity>>

    @Query("SELECT * FROM catalog_challenges ORDER BY challenge_id")
    fun observeChallenges(): Flow<List<CatalogChallengeEntity>>

    @Query("SELECT * FROM catalog_reinforcement_messages ORDER BY message_id")
    fun observeReinforcementMessages(): Flow<List<CatalogReinforcementMessageEntity>>
}
