package com.neuronovaapps.rutinago.data.communication

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File

/** Reuses Preferences DataStore without adding these contextual answers to Android backup. */
object CommunicationSetupStorage {
    const val FILE_NAME = "communication_profile_setup.preferences_pb"
    @Volatile private var instance: DataStore<Preferences>? = null

    fun get(context: Context): CommunicationSetupPreferences {
        val appContext = context.applicationContext
        val store = instance ?: synchronized(this) {
            instance ?: PreferenceDataStoreFactory.create(
                produceFile = { File(appContext.noBackupFilesDir, FILE_NAME) },
            ).also { instance = it }
        }
        return CommunicationSetupPreferences(store)
    }
}
