package com.neuronovaapps.rutinago.data.arasaac.offline

/** A worker cannot fetch an ID which is no longer persisted anywhere. */
class PictogramDownloadTask(
    private val isReferenced: suspend (Long) -> Boolean,
    private val localExists: (Long) -> Boolean,
    private val download: suspend (Long) -> PictogramDownloadResult,
) {
    suspend fun run(id: Long): PictogramDownloadResult {
        require(id > 0)
        if (!isReferenced(id) || localExists(id)) return PictogramDownloadResult.READY
        return download(id)
    }
}
