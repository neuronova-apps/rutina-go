package com.neuronovaapps.rutinago.data.catalog.repository

import com.neuronovaapps.rutinago.data.catalog.local.dao.CatalogImportMetadataDao
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogImportStatus
import com.neuronovaapps.rutinago.domain.catalog.CatalogImportState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CatalogImportStateRepository(
    metadataDao: CatalogImportMetadataDao,
    private val packagedCatalogVersion: String,
) {
    val state: Flow<CatalogImportState> = metadataDao.observeLatestAttempt().map { metadata ->
        when (metadata?.status) {
            null -> CatalogImportState.Missing
            CatalogImportStatus.SUCCESS -> {
                if (metadata.catalogVersion == packagedCatalogVersion) {
                    CatalogImportState.Ready(metadata.catalogVersion)
                } else {
                    CatalogImportState.UpdateAvailable(
                        installedVersion = metadata.catalogVersion,
                        packagedVersion = packagedCatalogVersion,
                    )
                }
            }
            CatalogImportStatus.FAILED -> CatalogImportState.Failed(
                metadata.catalogVersion,
                metadata.errorMessage,
            )
            CatalogImportStatus.INCOMPATIBLE -> CatalogImportState.Incompatible(
                metadata.catalogVersion,
                metadata.contractVersion,
            )
        }
    }
}
