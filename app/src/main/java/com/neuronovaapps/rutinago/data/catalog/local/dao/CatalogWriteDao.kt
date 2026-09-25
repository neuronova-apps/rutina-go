package com.neuronovaapps.rutinago.data.catalog.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.neuronovaapps.rutinago.data.catalog.local.entity.ActivityContextCrossRef
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogAchievementEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogActivityEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogCategoryEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogChallengeEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogGamificationReferenceEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogReinforcementMessageEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogRoutineEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogRoutineStepEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogVisualReferenceEntity

@Dao
interface CatalogWriteDao {
    @Upsert
    suspend fun upsertCategories(items: List<CatalogCategoryEntity>)

    @Upsert
    suspend fun upsertActivities(items: List<CatalogActivityEntity>)

    @Upsert
    suspend fun upsertActivityContexts(items: List<ActivityContextCrossRef>)

    @Upsert
    suspend fun upsertRoutines(items: List<CatalogRoutineEntity>)

    @Upsert
    suspend fun upsertRoutineSteps(items: List<CatalogRoutineStepEntity>)

    @Upsert
    suspend fun upsertAchievements(items: List<CatalogAchievementEntity>)

    @Upsert
    suspend fun upsertChallenges(items: List<CatalogChallengeEntity>)

    @Upsert
    suspend fun upsertGamificationReferences(
        items: List<CatalogGamificationReferenceEntity>,
    )

    @Upsert
    suspend fun upsertMessages(items: List<CatalogReinforcementMessageEntity>)

    @Upsert
    suspend fun upsertVisualReferences(items: List<CatalogVisualReferenceEntity>)

    @Query("DELETE FROM catalog_visual_references WHERE catalog_version != :catalogVersion")
    suspend fun deleteStaleVisualReferences(catalogVersion: String)

    @Query("DELETE FROM catalog_gamification_references WHERE catalog_version != :catalogVersion")
    suspend fun deleteStaleGamificationReferences(catalogVersion: String)

    @Query("DELETE FROM catalog_routine_steps WHERE catalog_version != :catalogVersion")
    suspend fun deleteStaleRoutineSteps(catalogVersion: String)

    @Query("DELETE FROM catalog_activity_contexts WHERE catalog_version != :catalogVersion")
    suspend fun deleteStaleActivityContexts(catalogVersion: String)

    @Query("DELETE FROM catalog_reinforcement_messages WHERE catalog_version != :catalogVersion")
    suspend fun deleteStaleMessages(catalogVersion: String)

    @Query("DELETE FROM catalog_achievements WHERE catalog_version != :catalogVersion")
    suspend fun deleteStaleAchievements(catalogVersion: String)

    @Query("DELETE FROM catalog_challenges WHERE catalog_version != :catalogVersion")
    suspend fun deleteStaleChallenges(catalogVersion: String)

    @Query("DELETE FROM catalog_routines WHERE catalog_version != :catalogVersion")
    suspend fun deleteStaleRoutines(catalogVersion: String)

    @Query("DELETE FROM catalog_activities WHERE catalog_version != :catalogVersion")
    suspend fun deleteStaleActivities(catalogVersion: String)

    @Query("DELETE FROM catalog_categories WHERE catalog_version != :catalogVersion")
    suspend fun deleteStaleCategories(catalogVersion: String)

    @Query("SELECT COUNT(*) FROM catalog_categories")
    suspend fun categoryCount(): Int

    @Query("SELECT COUNT(*) FROM catalog_activities")
    suspend fun activityCount(): Int

    @Query("SELECT COUNT(*) FROM catalog_activity_contexts")
    suspend fun activityContextCount(): Int

    @Query("SELECT COUNT(*) FROM catalog_routines")
    suspend fun routineCount(): Int

    @Query("SELECT COUNT(*) FROM catalog_routine_steps")
    suspend fun routineStepCount(): Int

    @Query("SELECT COUNT(*) FROM catalog_achievements")
    suspend fun achievementCount(): Int

    @Query("SELECT COUNT(*) FROM catalog_challenges")
    suspend fun challengeCount(): Int

    @Query("SELECT COUNT(*) FROM catalog_gamification_references")
    suspend fun gamificationReferenceCount(): Int

    @Query("SELECT COUNT(*) FROM catalog_reinforcement_messages")
    suspend fun messageCount(): Int

    @Query("SELECT COUNT(*) FROM catalog_visual_references")
    suspend fun visualReferenceCount(): Int
}
