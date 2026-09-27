package com.neuronovaapps.rutinago

import android.app.Application
import android.util.Log
import com.neuronovaapps.rutinago.data.catalog.CatalogContainer
import com.neuronovaapps.rutinago.domain.user.UserRoutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RutinaGoApplication : Application() {
    val catalogContainer: CatalogContainer by lazy { CatalogContainer(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            runCatching { catalogContainer.importer.importIfNeeded() }
                .onFailure { error ->
                    Log.e(TAG, "Master catalog import failed.", error)
                }
        }
        applicationScope.launch {
            runCatching { catalogContainer.offlinePictograms.reconcilePersistedReferences() }
                .onFailure { error -> Log.w(TAG, "Pictogram reconciliation failed: ${error.javaClass.simpleName}") }
        }
    }

    fun schedulePictogramsAfterSave(routine: UserRoutine) {
        applicationScope.launch {
            runCatching { catalogContainer.offlinePictograms.enqueueAfterSave(routine) }
                .onFailure { error -> Log.w(TAG, "Pictogram scheduling failed: ${error.javaClass.simpleName}") }
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }

    private companion object {
        const val TAG = "RutinaGoCatalog"
    }
}
