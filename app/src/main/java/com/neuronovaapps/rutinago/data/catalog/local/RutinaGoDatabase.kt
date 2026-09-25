package com.neuronovaapps.rutinago.data.catalog.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(CatalogDatabaseConverters::class)
abstract class RutinaGoDatabase : RoomDatabase() {
    abstract fun catalogReadDao(): CatalogReadDao

    abstract fun catalogWriteDao(): CatalogWriteDao

    abstract fun catalogImportMetadataDao(): CatalogImportMetadataDao

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
                ).build().also { instance = it }
            }
    }
}
