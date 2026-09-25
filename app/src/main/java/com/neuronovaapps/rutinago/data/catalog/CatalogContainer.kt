package com.neuronovaapps.rutinago.data.catalog

import android.content.Context
import com.neuronovaapps.rutinago.data.catalog.importer.AndroidCatalogAssetSource
import com.neuronovaapps.rutinago.data.catalog.importer.MasterCatalogImporter
import com.neuronovaapps.rutinago.data.catalog.local.RutinaGoDatabase
import com.neuronovaapps.rutinago.data.catalog.repository.CatalogImportStateRepository
import com.neuronovaapps.rutinago.data.catalog.repository.RoomCatalogRepository
import com.neuronovaapps.rutinago.domain.catalog.CatalogRepository
import com.neuronovaapps.rutinago.domain.catalog.CatalogUiState
import com.neuronovaapps.rutinago.domain.catalog.observeCatalogUiState
import kotlinx.coroutines.flow.Flow

class CatalogContainer(context: Context) {
    val database: RutinaGoDatabase = RutinaGoDatabase.getInstance(context)
    val importer = MasterCatalogImporter(
        database,
        AndroidCatalogAssetSource(context),
    )
    val repository: CatalogRepository = RoomCatalogRepository(database.catalogReadDao())
    val importState = CatalogImportStateRepository(
        database.catalogImportMetadataDao(),
        PACKAGED_CATALOG_VERSION,
    )
    val uiState: Flow<CatalogUiState> = observeCatalogUiState(importState.state, repository)

    companion object {
        const val PACKAGED_CATALOG_VERSION = "1.0.0"
    }
}
