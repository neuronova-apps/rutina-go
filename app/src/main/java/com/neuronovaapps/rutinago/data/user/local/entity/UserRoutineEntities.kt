package com.neuronovaapps.rutinago.data.user.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Relation
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogActivityEntity

@Entity(
    tableName = "user_routines",
    indices = [
        Index(value = ["profile_id", "archived"]),
        Index(value = ["source_template_id"]),
    ],
)
data class UserRoutineEntity(
    @androidx.room.PrimaryKey
    @ColumnInfo(name = "user_routine_id")
    val userRoutineId: String,
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "source_template_id")
    val sourceTemplateId: String?,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "modified_at_epoch_millis")
    val modifiedAtEpochMillis: Long,
    @ColumnInfo(name = "archived")
    val archived: Boolean,
)

@Entity(
    tableName = "user_routine_steps",
    primaryKeys = ["user_routine_id", "position"],
    foreignKeys = [
        ForeignKey(
            entity = UserRoutineEntity::class,
            parentColumns = ["user_routine_id"],
            childColumns = ["user_routine_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CatalogActivityEntity::class,
            parentColumns = ["activity_id"],
            childColumns = ["activity_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["activity_id"])],
)
data class UserRoutineStepEntity(
    @ColumnInfo(name = "user_routine_id")
    val userRoutineId: String,
    @ColumnInfo(name = "position")
    val position: Int,
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "custom_instruction")
    val customInstruction: String?,
    @ColumnInfo(name = "future_visual_reference")
    val futureVisualReference: String?,
)

data class UserRoutineWithSteps(
    @Embedded val routine: UserRoutineEntity,
    @Relation(
        parentColumn = "user_routine_id",
        entityColumn = "user_routine_id",
        entity = UserRoutineStepEntity::class,
    )
    val steps: List<UserRoutineStepEntity>,
)
