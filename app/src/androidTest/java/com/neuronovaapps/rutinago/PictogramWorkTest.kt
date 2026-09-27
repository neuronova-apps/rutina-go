package com.neuronovaapps.rutinago

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.neuronovaapps.rutinago.data.arasaac.offline.PictogramDownloadWorker
import com.neuronovaapps.rutinago.data.arasaac.offline.PictogramDownloadResult
import com.neuronovaapps.rutinago.data.arasaac.offline.WorkManagerPictogramQueue
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PictogramWorkTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun requestNeedsNetworkAndHasStableUniqueId() {
        val request = WorkManagerPictogramQueue.requestFor(10384)
        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
        assertEquals(10384L, request.workSpec.input.getLong(PictogramDownloadWorker.KEY_ARASAAC_ID, 0))
        assertEquals("arasaac-pictogram-10384", PictogramDownloadWorker.uniqueName(10384))
    }

    @Test fun workerRejectsMissingIdWithoutDownloading() = runBlocking {
        val worker = TestListenableWorkerBuilder<PictogramDownloadWorker>(context).build()
        assertTrue(worker.doWork() is ListenableWorker.Result.Failure)
    }

    @Test fun workerMapsSuccessRetryAndPermanentFailure() {
        assertTrue(PictogramDownloadWorker.resultFor(PictogramDownloadResult.READY)
            is ListenableWorker.Result.Success)
        assertTrue(PictogramDownloadWorker.resultFor(PictogramDownloadResult.RETRY)
            is ListenableWorker.Result.Retry)
        assertTrue(PictogramDownloadWorker.resultFor(PictogramDownloadResult.PERMANENT_FAILURE)
            is ListenableWorker.Result.Failure)
    }

    @Test fun workerSkipsAnIdWhichHasNoPersistedReference() = runBlocking {
        val worker = TestListenableWorkerBuilder<PictogramDownloadWorker>(context)
            .setInputData(workDataOf(PictogramDownloadWorker.KEY_ARASAAC_ID to Long.MAX_VALUE))
            .build()
        assertTrue(worker.doWork() is ListenableWorker.Result.Success)
    }
}
