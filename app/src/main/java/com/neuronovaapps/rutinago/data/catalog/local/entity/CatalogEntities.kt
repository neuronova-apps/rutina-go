package com.neuronovaapps.rutinago.data.catalog.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

enum class RoutineVariant {
    SIMPLE,
    COMPLETE,
    OPTIONAL,
}

enum class GamificationOwnerType {
    ACHIEVEMENT,
    CHALLENGE,
}

enum class CatalogReferenceType {
    ACT,
    RUT,
}

enum class CatalogImportStatus {
    SUCCESS,
    FAILED,
    INCOMPATIBLE,
}

@Entity(tableName = "catalog_categories")
data class CatalogCategoryEntity(
    @androidx.room.PrimaryKey
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "catalog_version")
    val catalogVersion: String,
)

@Entity(
    tableName = "catalog_activities",
    foreignKeys = [
        ForeignKey(
            entity = CatalogCategoryEntity::class,
            parentColumns = ["name"],
            childColumns = ["category_name"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("category_name")],
)
data class CatalogActivityEntity(
    @androidx.room.PrimaryKey
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "category_name")
    val categoryName: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "simple_instruction")
    val simpleInstruction: String,
    @ColumnInfo(name = "arasaac_search_term")
    val arasaacSearchTerm: String,
    @ColumnInfo(name = "age_group")
    val ageGroup: String,
    @ColumnInfo(name = "autonomy_level")
    val autonomyLevel: String,
    @ColumnInfo(name = "supervision")
    val supervision: String,
    @ColumnInfo(name = "simple_mode")
    val simpleMode: Boolean,
    @ColumnInfo(name = "advanced_mode")
    val advancedMode: Boolean,
    @ColumnInfo(name = "visual_resource")
    val visualResource: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "observations")
    val observations: String?,
    @ColumnInfo(name = "catalog_version")
    val catalogVersion: String,
)

@Entity(
    tableName = "catalog_activity_contexts",
    primaryKeys = ["activity_id", "context"],
    foreignKeys = [
        ForeignKey(
            entity = CatalogActivityEntity::class,
            parentColumns = ["activity_id"],
            childColumns = ["activity_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("context")],
)
data class ActivityContextCrossRef(
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "context")
    val context: String,
    @ColumnInfo(name = "catalog_version")
    val catalogVersion: String,
)

@Entity(
    tableName = "catalog_routines",
    foreignKeys = [
        ForeignKey(
            entity = CatalogCategoryEntity::class,
            parentColumns = ["name"],
            childColumns = ["primary_area"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("primary_area")],
)
data class CatalogRoutineEntity(
    @androidx.room.PrimaryKey
    @ColumnInfo(name = "routine_id")
    val routineId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "primary_area")
    val primaryArea: String,
    @ColumnInfo(name = "context")
    val context: String,
    @ColumnInfo(name = "age_group")
    val ageGroup: String,
    @ColumnInfo(name = "functional_objective")
    val functionalObjective: String,
    @ColumnInfo(name = "suggested_moment")
    val suggestedMoment: String,
    @ColumnInfo(name = "declared_simple_step_count")
    val declaredSimpleStepCount: Int,
    @ColumnInfo(name = "declared_complete_step_count")
    val declaredCompleteStepCount: Int,
    @ColumnInfo(name = "personalizable")
    val personalizable: Boolean,
    @ColumnInfo(name = "key_supervision")
    val keySupervision: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "observations")
    val observations: String?,
    @ColumnInfo(name = "catalog_version")
    val catalogVersion: String,
)

@Entity(
    tableName = "catalog_routine_steps",
    primaryKeys = ["routine_id", "variant", "position"],
    foreignKeys = [
        ForeignKey(
            entity = CatalogRoutineEntity::class,
            parentColumns = ["routine_id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CatalogActivityEntity::class,
            parentColumns = ["activity_id"],
            childColumns = ["activity_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("activity_id")],
)
data class CatalogRoutineStepEntity(
    @ColumnInfo(name = "routine_id")
    val routineId: String,
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "variant")
    val variant: RoutineVariant,
    @ColumnInfo(name = "position")
    val position: Int,
    @ColumnInfo(name = "catalog_version")
    val catalogVersion: String,
)

@Entity(
    tableName = "catalog_achievements",
    indices = [Index("category")],
)
data class CatalogAchievementEntity(
    @androidx.room.PrimaryKey
    @ColumnInfo(name = "achievement_id")
    val achievementId: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "gamification_level")
    val gamificationLevel: String,
    @ColumnInfo(name = "age_group")
    val ageGroup: String,
    @ColumnInfo(name = "reference_type")
    val referenceType: CatalogReferenceType,
    @ColumnInfo(name = "unlock_condition")
    val unlockCondition: String,
    @ColumnInfo(name = "target_quantity")
    val targetQuantity: Int,
    @ColumnInfo(name = "window_days")
    val windowDays: Int,
    @ColumnInfo(name = "repeatable")
    val repeatable: Boolean,
    @ColumnInfo(name = "visual_reward")
    val visualReward: String,
    @ColumnInfo(name = "penalizes_if_incomplete")
    val penalizesIfIncomplete: Boolean,
    @ColumnInfo(name = "default_activation")
    val defaultActivation: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "observations")
    val observations: String,
    @ColumnInfo(name = "catalog_version")
    val catalogVersion: String,
)

@Entity(
    tableName = "catalog_challenges",
    indices = [Index("category")],
)
data class CatalogChallengeEntity(
    @androidx.room.PrimaryKey
    @ColumnInfo(name = "challenge_id")
    val challengeId: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "gamification_level")
    val gamificationLevel: String,
    @ColumnInfo(name = "age_group")
    val ageGroup: String,
    @ColumnInfo(name = "reference_type")
    val referenceType: CatalogReferenceType,
    @ColumnInfo(name = "unlock_condition")
    val unlockCondition: String,
    @ColumnInfo(name = "target_quantity")
    val targetQuantity: Int,
    @ColumnInfo(name = "window_days")
    val windowDays: Int,
    @ColumnInfo(name = "repeatable")
    val repeatable: Boolean,
    @ColumnInfo(name = "visual_reward")
    val visualReward: String,
    @ColumnInfo(name = "penalizes_if_incomplete")
    val penalizesIfIncomplete: Boolean,
    @ColumnInfo(name = "default_activation")
    val defaultActivation: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "observations")
    val observations: String,
    @ColumnInfo(name = "catalog_version")
    val catalogVersion: String,
)

@Entity(
    tableName = "catalog_gamification_references",
    primaryKeys = ["owner_id", "position"],
    indices = [Index("reference_id"), Index("owner_type")],
)
data class CatalogGamificationReferenceEntity(
    @ColumnInfo(name = "owner_id")
    val ownerId: String,
    @ColumnInfo(name = "owner_type")
    val ownerType: GamificationOwnerType,
    @ColumnInfo(name = "reference_type")
    val referenceType: CatalogReferenceType,
    @ColumnInfo(name = "reference_id")
    val referenceId: String,
    @ColumnInfo(name = "position")
    val position: Int,
    @ColumnInfo(name = "catalog_version")
    val catalogVersion: String,
)

@Entity(
    tableName = "catalog_reinforcement_messages",
    indices = [Index("event_type"), Index("category")],
)
data class CatalogReinforcementMessageEntity(
    @androidx.room.PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "event_type")
    val eventType: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "tone")
    val tone: String,
    @ColumnInfo(name = "age_group")
    val ageGroup: String,
    @ColumnInfo(name = "gamification_required")
    val gamificationRequired: Boolean,
    @ColumnInfo(name = "gamification_level")
    val gamificationLevel: String,
    @ColumnInfo(name = "message")
    val message: String,
    @ColumnInfo(name = "usage_channel")
    val usageChannel: String,
    @ColumnInfo(name = "length")
    val length: String,
    @ColumnInfo(name = "allows_repetition")
    val allowsRepetition: String,
    @ColumnInfo(name = "event_origin")
    val eventOrigin: String,
    @ColumnInfo(name = "safety_rule")
    val safetyRule: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "observations")
    val observations: String,
    @ColumnInfo(name = "recommended_frequency")
    val recommendedFrequency: String,
    @ColumnInfo(name = "tts_activation")
    val ttsActivation: String,
    @ColumnInfo(name = "catalog_version")
    val catalogVersion: String,
)

@Entity(
    tableName = "catalog_visual_references",
    foreignKeys = [
        ForeignKey(
            entity = CatalogActivityEntity::class,
            parentColumns = ["activity_id"],
            childColumns = ["activity_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CatalogVisualReferenceEntity(
    @androidx.room.PrimaryKey
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "visual_source")
    val visualSource: String,
    @ColumnInfo(name = "arasaac_id")
    val arasaacId: Int?,
    @ColumnInfo(name = "variant")
    val variant: String?,
    @ColumnInfo(name = "local_file")
    val localFile: String?,
    @ColumnInfo(name = "alternative_text")
    val alternativeText: String,
    @ColumnInfo(name = "review_status")
    val reviewStatus: String,
    @ColumnInfo(name = "attribution")
    val attribution: String?,
    @ColumnInfo(name = "license")
    val license: String?,
    @ColumnInfo(name = "catalog_version")
    val catalogVersion: String,
)

@Entity(
    tableName = "catalog_import_metadata",
    indices = [
        Index("catalog_version"),
        Index("status"),
        Index("attempted_at_epoch_millis"),
    ],
)
data class CatalogImportMetadataEntity(
    @androidx.room.PrimaryKey
    @ColumnInfo(name = "attempt_id")
    val attemptId: String,
    @ColumnInfo(name = "catalog_version")
    val catalogVersion: String,
    @ColumnInfo(name = "contract_version")
    val contractVersion: String,
    @ColumnInfo(name = "manifest_checksum")
    val manifestChecksum: String,
    @ColumnInfo(name = "attempted_at_epoch_millis")
    val attemptedAtEpochMillis: Long,
    @ColumnInfo(name = "status")
    val status: CatalogImportStatus,
    @ColumnInfo(name = "category_count")
    val categoryCount: Int,
    @ColumnInfo(name = "activity_count")
    val activityCount: Int,
    @ColumnInfo(name = "routine_count")
    val routineCount: Int,
    @ColumnInfo(name = "routine_step_count")
    val routineStepCount: Int,
    @ColumnInfo(name = "achievement_count")
    val achievementCount: Int,
    @ColumnInfo(name = "challenge_count")
    val challengeCount: Int,
    @ColumnInfo(name = "message_count")
    val messageCount: Int,
    @ColumnInfo(name = "visual_reference_count")
    val visualReferenceCount: Int,
    @ColumnInfo(name = "master_record_count")
    val masterRecordCount: Int,
    @ColumnInfo(name = "error_message")
    val errorMessage: String?,
)
