package com.neuronovaapps.rutinago

import androidx.datastore.preferences.core.*
import com.neuronovaapps.rutinago.data.communication.*
import com.neuronovaapps.rutinago.domain.communication.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder

class CommunicationSpeechPreferencesTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var scope: CoroutineScope
    private lateinit var store: androidx.datastore.core.DataStore<Preferences>
    private lateinit var manager: CommunicationProfileSetupManager
    private val profiles = mutableSetOf("a", "b")
    private fun open() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(temporary.root, "speech.preferences_pb") })
        manager = CommunicationProfileSetupManager(CommunicationSetupPreferences(store), object : CommunicationProfileRegistry {
            override suspend fun exists(profileId: String) = profileId in profiles
            override suspend fun delete(profileId: String) { profiles.remove(profileId) }
        })
    }
    @Before fun prepare() { open() }
    @After fun close() = runBlocking { scope.coroutineContext[Job]!!.cancelAndJoin() }
    @Test fun absentRateIsNormalWithoutWritingOrMigration() = runBlocking {
        val before = store.data.first().asMap()
        assertEquals(CommunicationSpeechRate.NORMAL, manager.speechRateForProfile("a").first())
        assertEquals(before, store.data.first().asMap())
    }
    @Test fun ratesPersistAcrossReopeningAndRemainIndependent() = runBlocking {
        manager.setSpeechRate("a", CommunicationSpeechRate.SLOW); manager.setSpeechRate("b", CommunicationSpeechRate.FAST)
        scope.coroutineContext[Job]!!.cancelAndJoin(); open()
        assertEquals(CommunicationSpeechRate.SLOW, manager.speechRateForProfile("a").first())
        assertEquals(CommunicationSpeechRate.FAST, manager.speechRateForProfile("b").first())
    }
    @Test fun rateWritesDoNotOverwriteSetupPresentationVocabularyOrHome() = runBlocking {
        store.edit {
            it[stringPreferencesKey("profile:a")] = "existing setup"
            it[stringPreferencesKey("presentation:a")] = "existing presentation"
            it[stringSetPreferencesKey("vocabulary_visible_v1:a")] = setOf("VOC_015")
            it[stringSetPreferencesKey("home_vocabulary_v1:a")] = emptySet()
        }
        val before = store.data.first().asMap()
        manager.setSpeechRate("a", CommunicationSpeechRate.SLOW)
        assertEquals(before, store.data.first().asMap().filterKeys { it.name != "voice_rate_v1:a" })
    }
    @Test fun deletingProfileRemovesOnlyItsSpeechPreference() = runBlocking {
        manager.setSpeechRate("a", CommunicationSpeechRate.SLOW); manager.setSpeechRate("b", CommunicationSpeechRate.FAST)
        manager.deleteProfile("a")
        assertFalse(store.data.first().asMap().keys.any { it.name == "voice_rate_v1:a" })
        assertEquals(CommunicationSpeechRate.FAST, manager.speechRateForProfile("b").first())
    }
    @Test fun unknownStoredRateFallsBackWithoutDestroyingItsRecord() = runBlocking {
        store.edit { it[stringPreferencesKey("voice_rate_v1:a")] = "future-value" }
        assertEquals(CommunicationSpeechRate.NORMAL, manager.speechRateForProfile("a").first())
        assertEquals("future-value", store.data.first()[stringPreferencesKey("voice_rate_v1:a")])
    }
    @Test fun deletedOrUnknownProfileCannotReceiveRateWrites() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { manager.setSpeechRate("missing", CommunicationSpeechRate.SLOW) } }
    }
}
