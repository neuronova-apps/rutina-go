package com.neuronovaapps.rutinago.domain.catalog

sealed interface CatalogImportState {
    data object Missing : CatalogImportState

    data class Ready(val catalogVersion: String) : CatalogImportState

    data class UpdateAvailable(
        val installedVersion: String,
        val packagedVersion: String,
    ) : CatalogImportState

    data class Failed(
        val catalogVersion: String,
        val message: String?,
    ) : CatalogImportState

    data class Incompatible(
        val catalogVersion: String,
        val contractVersion: String,
    ) : CatalogImportState
}
