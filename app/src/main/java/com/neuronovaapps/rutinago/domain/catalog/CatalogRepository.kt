package com.neuronovaapps.rutinago.domain.catalog

import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun categories(): Flow<List<CatalogCategory>>

    fun activitiesByCategory(category: String): Flow<List<CatalogActivity>>

    fun activityById(activityId: String): Flow<CatalogActivity?>

    fun routinesByCategory(category: String): Flow<List<CatalogRoutine>>

    fun routineById(routineId: String): Flow<CatalogRoutine?>

    fun routineSteps(routineId: String): Flow<List<CatalogRoutineStep>>

    fun routineSteps(
        routineId: String,
        variant: RoutineVariant,
    ): Flow<List<CatalogRoutineStep>>

    fun achievements(): Flow<List<CatalogGamificationItem>>

    fun challenges(): Flow<List<CatalogGamificationItem>>

    fun reinforcementMessages(): Flow<List<CatalogReinforcementMessage>>
}
