package com.neuronovaapps.rutinago.data.execution.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Relation

@Entity(
    tableName = "routine_executions",
    indices = [
        Index(value = ["profile_id", "status"]),
        Index(value = ["profile_id", "started_at_epoch_millis"]),
        Index(value = ["user_routine_id"]),
    ],
)
data class RoutineExecutionEntity(
    @androidx.room.PrimaryKey
    @ColumnInfo(name = "execution_id")
    val executionId: String,
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "user_routine_id")
    val userRoutineId: String,
    @ColumnInfo(name = "routine_name")
    val routineName: String,
    @ColumnInfo(name = "started_at_epoch_millis")
    val startedAtEpochMillis: Long,
    @ColumnInfo(name = "finished_at_epoch_millis")
    val finishedAtEpochMillis: Long?,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "total_steps")
    val totalSteps: Int,
    @ColumnInfo(name = "completed_steps")
    val completedSteps: Int,
    @ColumnInfo(name = "skipped_steps")
    val skippedSteps: Int,
    @ColumnInfo(name = "current_step_position")
    val currentStepPosition: Int?,
)

@Entity(
    tableName = "execution_steps",
    primaryKeys = ["execution_id", "position"],
    foreignKeys = [
        ForeignKey(
            entity = RoutineExecutionEntity::class,
            parentColumns = ["execution_id"],
            childColumns = ["execution_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["activity_id"])],
)
data class ExecutionStepEntity(
    @ColumnInfo(name = "execution_id")
    val executionId: String,
    @ColumnInfo(name = "position")
    val position: Int,
    @ColumnInfo(name = "activity_id")
    val activityId: String,
    @ColumnInfo(name = "activity_name")
    val activityName: String,
    @ColumnInfo(name = "instruction")
    val instruction: String,
    @ColumnInfo(name = "visual_reference")
    val visualReference: String?,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "performed_at_epoch_millis")
    val performedAtEpochMillis: Long?,
)

data class RoutineExecutionWithSteps(
    @Embedded val execution: RoutineExecutionEntity,
    @Relation(
        parentColumn = "execution_id",
        entityColumn = "execution_id",
        entity = ExecutionStepEntity::class,
    )
    val steps: List<ExecutionStepEntity>,
)
