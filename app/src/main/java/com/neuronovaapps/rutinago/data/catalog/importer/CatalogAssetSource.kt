package com.neuronovaapps.rutinago.data.catalog.importer

import android.content.Context
import java.io.FileNotFoundException

fun interface CatalogAssetSource {
    fun readBytes(filename: String): ByteArray
}

class AndroidCatalogAssetSource(
    context: Context,
    private val basePath: String = DEFAULT_BASE_PATH,
) : CatalogAssetSource {
    private val assets = context.applicationContext.assets

    override fun readBytes(filename: String): ByteArray = try {
        assets.open("$basePath/$filename").use { it.readBytes() }
    } catch (error: FileNotFoundException) {
        throw CatalogImportException(
            CatalogImportErrorCode.ASSET_MISSING,
            "Catalog asset is missing: $basePath/$filename",
            error,
        )
    }

    companion object {
        const val DEFAULT_BASE_PATH = "master_catalog/v1"
    }
}
