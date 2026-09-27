package com.neuronovaapps.rutinago.data.arasaac.offline

/** Returns a private File for a valid download, otherwise the remote URL for Coil. */
class PictogramImageResolver(private val store: PictogramFileStore) {
    fun modelFor(id: Long?, remoteUrl: String): Any =
        id?.let(store::getLocalFile) ?: remoteUrl
}
