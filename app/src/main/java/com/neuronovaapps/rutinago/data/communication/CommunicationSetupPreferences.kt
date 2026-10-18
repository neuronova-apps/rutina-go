package com.neuronovaapps.rutinago.data.communication

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.neuronovaapps.rutinago.domain.communication.CommunicationPersonalVocabularyRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationHomeVocabularyRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationHomeVocabulary
import com.neuronovaapps.rutinago.domain.communication.CommunicationCatalogRepository
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.neuronovaapps.rutinago.domain.communication.CommunicationAgeRange
import com.neuronovaapps.rutinago.domain.communication.CommunicationProfileSetup
import com.neuronovaapps.rutinago.domain.communication.CommunicationProfileSetupRepository
import com.neuronovaapps.rutinago.domain.communication.OralCommunication
import com.neuronovaapps.rutinago.domain.communication.CommunicationPresentationMode
import com.neuronovaapps.rutinago.domain.communication.CommunicationPresentationPreferences
import com.neuronovaapps.rutinago.domain.communication.CommunicationPresentationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.neuronovaapps.rutinago.domain.communication.CommunicationSpeechRate
import com.neuronovaapps.rutinago.domain.communication.CommunicationSpeechPreferencesRepository

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
        store.edit {
            it.remove(key(profileId))
            it.remove(presentationKey(profileId))
            it.remove(vocabularyKey(profileId))
            it.remove(homeVocabularyKey(profileId))
            it.remove(speechRateKey(profileId))
        }
    }

    private fun speechRateKey(profileId: String) = stringPreferencesKey("voice_rate_v1:$profileId")
    fun observeSpeechRate(profileId: String): Flow<CommunicationSpeechRate> {
        require(profileId.isNotBlank())
        return store.data.map { stored -> CommunicationSpeechRate.entries.firstOrNull {
            it.name == stored[speechRateKey(profileId)]
        } ?: CommunicationSpeechRate.NORMAL }.distinctUntilChanged()
    }
    suspend fun saveSpeechRate(profileId: String, rate: CommunicationSpeechRate) {
        require(profileId.isNotBlank())
        store.edit { it[speechRateKey(profileId)] = rate.name }
    }

    fun observeVocabulary(profileId: String): Flow<Set<String>?> {
        require(profileId.isNotBlank())
        return store.data.map { stored -> stored[vocabularyKey(profileId)]?.let {
            java.util.Collections.unmodifiableSet(HashSet(it))
        } }.distinctUntilChanged()
    }

    suspend fun saveVocabulary(profileId: String, visibleIds: Set<String>) {
        require(profileId.isNotBlank())
        val snapshot = visibleIds.toSet()
        store.edit { it[vocabularyKey(profileId)] = snapshot }
    }

    private fun vocabularyKey(profileId: String) = stringSetPreferencesKey("vocabulary_visible_v1:$profileId")

    fun observeHomeVocabulary(profileId: String): Flow<Set<String>?> {
        require(profileId.isNotBlank())
        return store.data.map { stored -> stored[homeVocabularyKey(profileId)]?.let {
            java.util.Collections.unmodifiableSet(HashSet(it))
        } }.distinctUntilChanged()
    }

    /** Read-modify-write in one DataStore transaction; hiding never removes assigned IDs. */
    suspend fun setHomeWord(profileId: String, vocabularyId: String, inHome: Boolean, knownIds: Set<String>) {
        require(profileId.isNotBlank())
        store.edit { stored ->
            val available = stored[vocabularyKey(profileId)]
            require(available != null) { "Configure available vocabulary first" }
            require(!inHome || vocabularyId in available) { "Home words must be available" }
            val assigned = CommunicationHomeVocabulary.assignedIds(stored[homeVocabularyKey(profileId)], available).intersect(knownIds)
            stored[homeVocabularyKey(profileId)] = if (inHome) assigned + vocabularyId else assigned - vocabularyId
        }
    }

    private fun homeVocabularyKey(profileId: String) = stringSetPreferencesKey("home_vocabulary_v1:$profileId")

    fun observePresentation(profileId: String): Flow<CommunicationPresentationPreferences> {
        require(profileId.isNotBlank())
        return store.data.map { decodePresentation(profileId, it[presentationKey(profileId)]) }.distinctUntilChanged()
    }

    suspend fun setPresentationMode(profileId: String, mode: CommunicationPresentationMode) {
        require(profileId.isNotBlank())
        store.edit { stored ->
            val current = decodePresentation(profileId, stored[presentationKey(profileId)])
            stored[presentationKey(profileId)] = encodePresentation(current.copy(selectedMode = mode, introductionSeen = true))
        }
    }

    suspend fun markIntroductionSeen(profileId: String) {
        require(profileId.isNotBlank())
        store.edit { stored ->
            val current = decodePresentation(profileId, stored[presentationKey(profileId)])
            stored[presentationKey(profileId)] = encodePresentation(current.copy(introductionSeen = true))
        }
    }

    private fun presentationKey(profileId: String) = stringPreferencesKey("presentation:$profileId")

    private fun encodePresentation(preferences: CommunicationPresentationPreferences) = JsonObject().apply {
        addProperty("schemaVersion", 1)
        addProperty("presentationMode", preferences.selectedMode?.name)
        addProperty("introductionSeen", preferences.introductionSeen)
    }.toString()

    private fun decodePresentation(profileId: String, value: String?): CommunicationPresentationPreferences {
        if (value == null) return CommunicationPresentationPreferences(profileId)
        // A damaged/future presentation record must never prevent access to communication.
        // Reading the fallback does not overwrite that record or the profile's characteristics.
        return runCatching {
            val record = JsonParser.parseString(value).asJsonObject
            require(record.keySet() == setOf("schemaVersion", "presentationMode", "introductionSeen"))
            require(record["schemaVersion"].toString() == "1")
            require(record["introductionSeen"].isJsonPrimitive && record["introductionSeen"].asJsonPrimitive.isBoolean)
            val mode = record["presentationMode"].takeUnless { it.isJsonNull }?.let {
                require(it.isJsonPrimitive && it.asJsonPrimitive.isString)
                CommunicationPresentationMode.valueOf(it.asString)
            }
            CommunicationPresentationPreferences(profileId, mode, record["introductionSeen"].asBoolean)
        }.getOrDefault(CommunicationPresentationPreferences(profileId))
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
    private val catalog: (() -> CommunicationCatalogRepository)? = null,
) : CommunicationProfileSetupRepository, CommunicationPresentationRepository, CommunicationPersonalVocabularyRepository, CommunicationHomeVocabularyRepository, CommunicationSpeechPreferencesRepository {
    override fun speechRateForProfile(profileId: String) = store.observeSpeechRate(profileId)
    override suspend fun setSpeechRate(profileId: String, rate: CommunicationSpeechRate) = mutationMutex.withLock {
        require(profiles.exists(profileId)) { "Unknown profile" }
        store.saveSpeechRate(profileId, rate)
    }
    override fun homeVocabularyForProfile(profileId: String) = store.observeHomeVocabulary(profileId)

    override suspend fun setHomeWord(profileId: String, vocabularyId: String, inHome: Boolean) = mutationMutex.withLock {
        require(profiles.exists(profileId)) { "Unknown profile" }
        val knownIds = requireNotNull(catalog) { "Vocabulary catalog required" }().vocabulary.map { it.vocabId }.toSet()
        require(vocabularyId in knownIds) {
            "Unknown vocabulary ID"
        }
        store.setHomeWord(profileId, vocabularyId, inHome, knownIds)
    }
    override fun vocabularyForProfile(profileId: String) = store.observeVocabulary(profileId)

    override suspend fun saveVocabulary(profileId: String, visibleIds: Set<String>) {
        val snapshot = visibleIds.toSet()
        mutationMutex.withLock {
            require(profiles.exists(profileId)) { "Unknown profile" }
            val knownIds = requireNotNull(catalog) { "Vocabulary catalog required" }().vocabulary.map { it.vocabId }.toSet()
            require(snapshot.all { it in knownIds }) { "Unknown vocabulary ID" }
            store.saveVocabulary(profileId, snapshot)
        }
    }
    override fun setupForProfile(profileId: String) = store.observe(profileId)
    override fun presentationForProfile(profileId: String) = store.observePresentation(profileId)

    override suspend fun setPresentationMode(profileId: String, mode: CommunicationPresentationMode) =
        mutationMutex.withLock {
            require(profiles.exists(profileId)) { "Unknown profile" }
            store.setPresentationMode(profileId, mode)
        }

    override suspend fun markPresentationIntroductionSeen(profileId: String) = mutationMutex.withLock {
        require(profiles.exists(profileId)) { "Unknown profile" }
        store.markIntroductionSeen(profileId)
    }

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
