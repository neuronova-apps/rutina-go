package com.neuronovaapps.rutinago.domain.catalog

import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogReferenceType
import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant

data class CatalogCategory(
    val name: String,
    val order: Int,
)

data class CatalogActivity(
    val id: String,
    val category: String,
    val name: String,
    val simpleInstruction: String,
    val arasaacSearchTerm: String,
    val ageGroup: String,
    val autonomyLevel: String,
    val supervision: String,
    val simpleMode: Boolean,
    val advancedMode: Boolean,
    val visualResource: String,
    val status: String,
    val observations: String?,
)

data class CatalogRoutine(
    val id: String,
    val name: String,
    val primaryArea: String,
    val context: String,
    val ageGroup: String,
    val functionalObjective: String,
    val suggestedMoment: String,
    val simpleStepCount: Int,
    val completeStepCount: Int,
    val personalizable: Boolean,
    val keySupervision: String,
    val status: String,
    val observations: String?,
)

data class CatalogRoutineStep(
    val routineId: String,
    val activityId: String,
    val variant: RoutineVariant,
    val position: Int,
)

data class CatalogGamificationItem(
    val id: String,
    val type: String,
    val name: String,
    val category: String,
    val level: String,
    val ageGroup: String,
    val referenceType: CatalogReferenceType,
    val unlockCondition: String,
    val targetQuantity: Int,
    val windowDays: Int,
    val repeatable: Boolean,
    val visualReward: String,
    val penalizesIfIncomplete: Boolean,
    val defaultActivation: String,
    val status: String,
    val observations: String,
)

data class CatalogReinforcementMessage(
    val id: String,
    val eventType: String,
    val category: String,
    val tone: String,
    val ageGroup: String,
    val gamificationRequired: Boolean,
    val gamificationLevel: String,
    val message: String,
    val usageChannel: String,
    val length: String,
    val allowsRepetition: String,
    val eventOrigin: String,
    val safetyRule: String,
    val status: String,
    val observations: String,
    val recommendedFrequency: String,
    val ttsActivation: String,
)
