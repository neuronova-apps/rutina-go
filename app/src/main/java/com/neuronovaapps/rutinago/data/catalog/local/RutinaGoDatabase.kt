package com.neuronovaapps.rutinago.data.catalog.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.neuronovaapps.rutinago.data.catalog.local.dao.CatalogImportMetadataDao
import com.neuronovaapps.rutinago.data.catalog.local.dao.CatalogReadDao
import com.neuronovaapps.rutinago.data.catalog.local.dao.CatalogWriteDao
import com.neuronovaapps.rutinago.data.catalog.local.entity.ActivityContextCrossRef
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogAchievementEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogActivityEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogCategoryEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogChallengeEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogGamificationReferenceEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogImportMetadataEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogReinforcementMessageEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogRoutineEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogRoutineStepEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogVisualReferenceEntity
import com.neuronovaapps.rutinago.data.user.local.dao.UserRoutineDao
import com.neuronovaapps.rutinago.data.execution.local.dao.RoutineExecutionDao
import com.neuronovaapps.rutinago.data.execution.local.entity.ExecutionStepEntity
import com.neuronovaapps.rutinago.data.execution.local.entity.RoutineExecutionEntity
import com.neuronovaapps.rutinago.data.user.local.entity.UserRoutineEntity
import com.neuronovaapps.rutinago.data.user.local.entity.UserRoutineStepEntity

@Database(
    entities = [
        CatalogCategoryEntity::class,
        CatalogActivityEntity::class,
        ActivityContextCrossRef::class,
        CatalogRoutineEntity::class,
        CatalogRoutineStepEntity::class,
        CatalogAchievementEntity::class,
        CatalogChallengeEntity::class,
        CatalogGamificationReferenceEntity::class,
        CatalogReinforcementMessageEntity::class,
        CatalogVisualReferenceEntity::class,
        CatalogImportMetadataEntity::class,
        UserRoutineEntity::class,
        UserRoutineStepEntity::class,
        RoutineExecutionEntity::class,
        ExecutionStepEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(CatalogDatabaseConverters::class)
abstract class RutinaGoDatabase : RoomDatabase() {
    abstract fun catalogReadDao(): CatalogReadDao

    abstract fun catalogWriteDao(): CatalogWriteDao

    abstract fun catalogImportMetadataDao(): CatalogImportMetadataDao

    abstract fun userRoutineDao(): UserRoutineDao

    abstract fun routineExecutionDao(): RoutineExecutionDao

    companion object {
        const val DATABASE_NAME = "rutinago_catalog.db"

        @Volatile
        private var instance: RutinaGoDatabase? = null

        fun getInstance(context: Context): RutinaGoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RutinaGoDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { instance = it }
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_routines` (" +
                        "`user_routine_id` TEXT NOT NULL, " +
                        "`profile_id` TEXT NOT NULL, " +
                        "`source_template_id` TEXT, " +
                        "`name` TEXT NOT NULL, " +
                        "`description` TEXT, " +
                        "`created_at_epoch_millis` INTEGER NOT NULL, " +
                        "`modified_at_epoch_millis` INTEGER NOT NULL, " +
                        "`archived` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`user_routine_id`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_user_routines_profile_id_archived` " +
                        "ON `user_routines` (`profile_id`, `archived`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_user_routines_source_template_id` " +
                        "ON `user_routines` (`source_template_id`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_routine_steps` (" +
                        "`user_routine_id` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "`activity_id` TEXT NOT NULL, " +
                        "`custom_instruction` TEXT, " +
                        "`future_visual_reference` TEXT, " +
                        "PRIMARY KEY(`user_routine_id`, `position`), " +
                        "FOREIGN KEY(`user_routine_id`) REFERENCES `user_routines`" +
                        "(`user_routine_id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`activity_id`) REFERENCES `catalog_activities`" +
                        "(`activity_id`) ON UPDATE NO ACTION ON DELETE RESTRICT)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_user_routine_steps_activity_id` " +
                        "ON `user_routine_steps` (`activity_id`)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `routine_executions` (" +
                        "`execution_id` TEXT NOT NULL, " +
                        "`profile_id` TEXT NOT NULL, " +
                        "`user_routine_id` TEXT NOT NULL, " +
                        "`routine_name` TEXT NOT NULL, " +
                        "`started_at_epoch_millis` INTEGER NOT NULL, " +
                        "`finished_at_epoch_millis` INTEGER, " +
                        "`status` TEXT NOT NULL, " +
                        "`total_steps` INTEGER NOT NULL, " +
                        "`completed_steps` INTEGER NOT NULL, " +
                        "`skipped_steps` INTEGER NOT NULL, " +
                        "`current_step_position` INTEGER, " +
                        "PRIMARY KEY(`execution_id`))",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_routine_executions_profile_id_status` " +
                        "ON `routine_executions` (`profile_id`, `status`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_routine_executions_profile_id_started_at_epoch_millis` " +
                        "ON `routine_executions` (`profile_id`, `started_at_epoch_millis`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_routine_executions_user_routine_id` " +
                        "ON `routine_executions` (`user_routine_id`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `execution_steps` (" +
                        "`execution_id` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "`activity_id` TEXT NOT NULL, " +
                        "`activity_name` TEXT NOT NULL, " +
                        "`instruction` TEXT NOT NULL, " +
                        "`visual_reference` TEXT, " +
                        "`status` TEXT NOT NULL, " +
                        "`performed_at_epoch_millis` INTEGER, " +
                        "PRIMARY KEY(`execution_id`, `position`), " +
                        "FOREIGN KEY(`execution_id`) REFERENCES `routine_executions`" +
                        "(`execution_id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_execution_steps_activity_id` " +
                        "ON `execution_steps` (`activity_id`)",
                )
            }
        }
    }
}
