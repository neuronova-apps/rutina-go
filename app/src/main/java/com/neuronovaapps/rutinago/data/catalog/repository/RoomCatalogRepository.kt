package com.neuronovaapps.rutinago.data.catalog.repository

import com.neuronovaapps.rutinago.data.catalog.local.dao.CatalogReadDao
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogAchievementEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogActivityEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogCategoryEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogChallengeEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogReinforcementMessageEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogRoutineEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogRoutineStepEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant
import com.neuronovaapps.rutinago.domain.catalog.CatalogActivity
import com.neuronovaapps.rutinago.domain.catalog.CatalogCategory
import com.neuronovaapps.rutinago.domain.catalog.CatalogGamificationItem
import com.neuronovaapps.rutinago.domain.catalog.CatalogReinforcementMessage
import com.neuronovaapps.rutinago.domain.catalog.CatalogRepository
import com.neuronovaapps.rutinago.domain.catalog.CatalogRoutine
import com.neuronovaapps.rutinago.domain.catalog.CatalogRoutineStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCatalogRepository(
    private val dao: CatalogReadDao,
) : CatalogRepository {
    override fun categories(): Flow<List<CatalogCategory>> =
        dao.observeCategories().map { items -> items.map(CatalogCategoryEntity::toModel) }

    override fun activities(): Flow<List<CatalogActivity>> =
        dao.observeActivities().map { items -> items.map(CatalogActivityEntity::toModel) }

    override fun activitiesByCategory(category: String): Flow<List<CatalogActivity>> =
        dao.observeActivitiesByCategory(category).map { items ->
            items.map(CatalogActivityEntity::toModel)
        }

    override fun activityById(activityId: String): Flow<CatalogActivity?> =
        dao.observeActivityById(activityId).map { it?.toModel() }

    override fun routines(): Flow<List<CatalogRoutine>> =
        dao.observeRoutines().map { items -> items.map(CatalogRoutineEntity::toModel) }

    override fun routinesByCategory(category: String): Flow<List<CatalogRoutine>> =
        dao.observeRoutinesByCategory(category).map { items ->
            items.map(CatalogRoutineEntity::toModel)
        }

    override fun routineById(routineId: String): Flow<CatalogRoutine?> =
        dao.observeRoutineById(routineId).map { it?.toModel() }

    override fun routineSteps(): Flow<List<CatalogRoutineStep>> =
        dao.observeRoutineSteps().map { items -> items.map(CatalogRoutineStepEntity::toModel) }

    override fun routineSteps(routineId: String): Flow<List<CatalogRoutineStep>> =
        dao.observeRoutineSteps(routineId).map { items ->
            items.map(CatalogRoutineStepEntity::toModel)
        }

    override fun routineSteps(
        routineId: String,
        variant: RoutineVariant,
    ): Flow<List<CatalogRoutineStep>> =
        dao.observeRoutineSteps(routineId, variant).map { items ->
            items.map(CatalogRoutineStepEntity::toModel)
        }

    override fun achievements(): Flow<List<CatalogGamificationItem>> =
        dao.observeAchievements().map { items ->
            items.map(CatalogAchievementEntity::toModel)
        }

    override fun challenges(): Flow<List<CatalogGamificationItem>> =
        dao.observeChallenges().map { items ->
            items.map(CatalogChallengeEntity::toModel)
        }

    override fun reinforcementMessages(): Flow<List<CatalogReinforcementMessage>> =
        dao.observeReinforcementMessages().map { items ->
            items.map(CatalogReinforcementMessageEntity::toModel)
        }
}

private fun CatalogCategoryEntity.toModel() = CatalogCategory(name, sortOrder)

private fun CatalogActivityEntity.toModel() = CatalogActivity(
    activityId,
    categoryName,
    name,
    simpleInstruction,
    arasaacSearchTerm,
    ageGroup,
    autonomyLevel,
    supervision,
    simpleMode,
    advancedMode,
    visualResource,
    status,
    observations,
)

private fun CatalogRoutineEntity.toModel() = CatalogRoutine(
    routineId,
    name,
    primaryArea,
    context,
    ageGroup,
    functionalObjective,
    suggestedMoment,
    declaredSimpleStepCount,
    declaredCompleteStepCount,
    personalizable,
    keySupervision,
    status,
    observations,
)

private fun CatalogRoutineStepEntity.toModel() =
    CatalogRoutineStep(routineId, activityId, variant, position)

private fun CatalogAchievementEntity.toModel() = CatalogGamificationItem(
    achievementId,
    type,
    name,
    category,
    gamificationLevel,
    ageGroup,
    referenceType,
    unlockCondition,
    targetQuantity,
    windowDays,
    repeatable,
    visualReward,
    penalizesIfIncomplete,
    defaultActivation,
    status,
    observations,
)

private fun CatalogChallengeEntity.toModel() = CatalogGamificationItem(
    challengeId,
    type,
    name,
    category,
    gamificationLevel,
    ageGroup,
    referenceType,
    unlockCondition,
    targetQuantity,
    windowDays,
    repeatable,
    visualReward,
    penalizesIfIncomplete,
    defaultActivation,
    status,
    observations,
)

private fun CatalogReinforcementMessageEntity.toModel() =
    CatalogReinforcementMessage(
        messageId,
        eventType,
        category,
        tone,
        ageGroup,
        gamificationRequired,
        gamificationLevel,
        message,
        usageChannel,
        length,
        allowsRepetition,
        eventOrigin,
        safetyRule,
        status,
        observations,
        recommendedFrequency,
        ttsActivation,
    )
