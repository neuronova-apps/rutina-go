package com.neuronovaapps.rutinago.data.communication

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.neuronovaapps.rutinago.domain.communication.CommunicationAgeRange
import com.neuronovaapps.rutinago.domain.communication.CommunicationProfileSetup
import com.neuronovaapps.rutinago.domain.communication.CommunicationProfileSetupRepository
import com.neuronovaapps.rutinago.domain.communication.OralCommunication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One record per stable profile ID. No name, age in years, or clinical data is stored. */
class CommunicationSetupPreferences(private val store: DataStore<Preferences>) {
    fun observe(profileId: String): Flow<CommunicationProfileSetup?> {
        require(profileId.isNotBlank())
        return store.data.map { it[key(profileId)]?.let { value -> decode(profileId, value) } }
            .distinctUntilChanged()
    }

    suspend fun save(setup: CommunicationProfileSetup) {
        val record = JsonObject().apply {
            addProperty("schemaVersion", setup.schemaVersion)
            addProperty("ageRange", setup.ageRange.name)
            addProperty("oralCommunication", setup.oralCommunication.name)
            addProperty("setupCompleted", setup.setupCompleted)
        }
        store.edit { it[key(setup.profileId)] = record.toString() }
    }

    suspend fun remove(profileId: String) {
        require(profileId.isNotBlank())
        store.edit { it.remove(key(profileId)) }
    }

    private fun key(profileId: String) = stringPreferencesKey("profile:$profileId")

    private fun decode(profileId: String, value: String): CommunicationProfileSetup {
        val record = JsonParser.parseString(value).asJsonObject
        require(record.keySet() == setOf("schemaVersion", "ageRange", "oralCommunication", "setupCompleted"))
        require(record["schemaVersion"].isJsonPrimitive && record["schemaVersion"].asJsonPrimitive.isNumber)
        require(record["schemaVersion"].toString() == "1")
        listOf("ageRange", "oralCommunication").forEach {
            require(record[it].isJsonPrimitive && record[it].asJsonPrimitive.isString)
        }
        require(record["setupCompleted"].isJsonPrimitive && record["setupCompleted"].asJsonPrimitive.isBoolean)
        return CommunicationProfileSetup(
            profileId, CommunicationAgeRange.valueOf(record["ageRange"].asString),
            OralCommunication.valueOf(record["oralCommunication"].asString),
            record["setupCompleted"].asBoolean,
        )
    }
}

interface CommunicationProfileRegistry {
    suspend fun exists(profileId: String): Boolean
    suspend fun delete(profileId: String)
}

/** Serialize saves and deletion across repository instances within the app process. */
class CommunicationProfileSetupManager(
    private val store: CommunicationSetupPreferences,
    private val profiles: CommunicationProfileRegistry,
) : CommunicationProfileSetupRepository {
    override fun setupForProfile(profileId: String) = store.observe(profileId)

    override suspend fun saveSetup(setup: CommunicationProfileSetup) = mutationMutex.withLock {
        require(setup.setupCompleted) { "Only completed setup is persisted" }
        require(profiles.exists(setup.profileId)) { "Unknown profile" }
        store.save(setup)
    }

    suspend fun deleteProfile(profileId: String) = mutationMutex.withLock {
        require(profiles.exists(profileId)) { "Unknown profile" }
        // Remove contextual data first. An interruption must not leave it orphaned.
        store.remove(profileId)
        profiles.delete(profileId)
    }

    private companion object {
        val mutationMutex = Mutex()
    }
}
