package com.neuronovaapps.rutinago.data.arasaac.offline

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neuronovaapps.rutinago.data.catalog.local.RutinaGoDatabase

class PictogramDownloadWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ARASAAC_ID, 0)
        if (id <= 0) return Result.failure()
        val store = PictogramFileStore.fromFilesDir(applicationContext.filesDir)
        val references = PictogramReferenceRepository(RutinaGoDatabase.getInstance(applicationContext))
        // Work may start after the user removed the selection; never fetch an orphaned ID.
        val task = PictogramDownloadTask(
            references::isPictogramReferenced,
            store::exists,
            PictogramDownloader(store)::download,
        )
        return resultFor(task.run(id))
    }

    companion object {
        const val KEY_ARASAAC_ID = "arasaac_id"
        internal fun resultFor(result: PictogramDownloadResult): Result = when (result) {
            PictogramDownloadResult.READY -> Result.success()
            PictogramDownloadResult.PERMANENT_FAILURE -> Result.failure()
            PictogramDownloadResult.RETRY -> Result.retry()
        }
        fun uniqueName(id: Long): String {
            require(id > 0)
            return "arasaac-pictogram-$id"
        }
    }
}
