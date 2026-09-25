package com.neuronovaapps.rutinago.data.catalog.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogImportMetadataEntity
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogImportStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogImportMetadataDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: CatalogImportMetadataEntity)

    @Query(
        "SELECT * FROM catalog_import_metadata " +
            "ORDER BY attempted_at_epoch_millis DESC LIMIT 1",
    )
    fun observeLatestAttempt(): Flow<CatalogImportMetadataEntity?>

    @Query(
        "SELECT * FROM catalog_import_metadata " +
            "ORDER BY attempted_at_epoch_millis DESC LIMIT 1",
    )
    suspend fun latestAttempt(): CatalogImportMetadataEntity?

    @Query("SELECT COUNT(*) FROM catalog_import_metadata")
    suspend fun attemptCount(): Int

    @Query(
        "SELECT * FROM catalog_import_metadata " +
            "WHERE status = :status ORDER BY attempted_at_epoch_millis DESC LIMIT 1",
    )
    suspend fun latestWithStatus(
        status: CatalogImportStatus = CatalogImportStatus.SUCCESS,
    ): CatalogImportMetadataEntity?
}
