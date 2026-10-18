package com.neuronovaapps.rutinago

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.neuronovaapps.rutinago.data.communication.CommunicationProfileRegistry
import com.neuronovaapps.rutinago.data.communication.CommunicationProfileSetupManager
import com.neuronovaapps.rutinago.data.communication.CommunicationSetupPreferences
import com.neuronovaapps.rutinago.domain.communication.CommunicationAgeRange
import com.neuronovaapps.rutinago.domain.communication.CommunicationProfileSetup
import com.neuronovaapps.rutinago.domain.communication.OralCommunication
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CommunicationProfileSetupTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var records: CommunicationSetupPreferences
    private lateinit var manager: CommunicationProfileSetupManager
    private val profiles = mutableMapOf("profile-a" to "Mismo nombre", "profile-b" to "Mismo nombre")
    private var failProfileDelete = false
    private val registry = object : CommunicationProfileRegistry {
        override suspend fun exists(profileId: String) = profiles.containsKey(profileId)
        override suspend fun delete(profileId: String) {
            check(records.observe(profileId).first() == null)
            if (failProfileDelete) throw IOException("simulated profile write failure")
            profiles.remove(profileId)
        }
    }
    private fun answers(id: String = "profile-a") = CommunicationProfileSetup(
        id, CommunicationAgeRange.YEARS_9_TO_12, OralCommunication.EMERGING_SPEECH,
    )
    private fun openStore() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope,
            produceFile = { File(temporary.root, "setup.preferences_pb") })
        records = CommunicationSetupPreferences(dataStore)
        manager = CommunicationProfileSetupManager(records, registry)
    }
    @Before fun prepare() = openStore()
    @After fun close() = runBlocking { scope.coroutineContext[Job]!!.cancelAndJoin() }

    @Test fun firstEntryHasNoInventedOrDefaultAnswers() = runBlocking {
        assertNull(manager.setupForProfile("profile-a").first())
        assertTrue(dataStore.data.first().asMap().isEmpty())
    }

    @Test fun explicitAnswersAreStoredWithCompletionAndVersion() = runBlocking {
        manager.saveSetup(answers())
        val actual = manager.setupForProfile("profile-a").first()
        assertEquals(answers(), actual)
        assertTrue(requireNotNull(actual).setupCompleted)
        assertEquals(1, actual.schemaVersion)
    }

    @Test fun answersSurviveClosingAndReopeningTheRealDataStore() = runBlocking {
        manager.saveSetup(answers())
        scope.coroutineContext[Job]!!.cancelAndJoin()
        openStore()
        assertEquals(answers(), manager.setupForProfile("profile-a").first())
    }

    @Test fun editingReplacesOnlyTheSelectedProfilesAnswers() = runBlocking {
        manager.saveSetup(answers())
        manager.saveSetup(answers("profile-b"))
        val edited = answers().copy(ageRange = CommunicationAgeRange.ADULT,
            oralCommunication = OralCommunication.VARIABLE_SPEECH)
        manager.saveSetup(edited)
        assertEquals(edited, manager.setupForProfile("profile-a").first())
        assertEquals(answers("profile-b"), manager.setupForProfile("profile-b").first())
        assertEquals(2, dataStore.data.first().asMap().size)
    }

    @Test fun identicalNamesNeverShareSetupAndRenameDoesNotLoseIt() = runBlocking {
        manager.saveSetup(answers())
        assertNull(manager.setupForProfile("profile-b").first())
        profiles["profile-a"] = "Nombre nuevo"
        assertEquals(answers(), manager.setupForProfile("profile-a").first())
    }

    @Test fun bothNotSpecifiedAnswersAreACompletedConfiguration() = runBlocking {
        val skipped = CommunicationProfileSetup("profile-a", CommunicationAgeRange.NOT_SPECIFIED,
            OralCommunication.NOT_SPECIFIED)
        manager.saveSetup(skipped)
        assertEquals(skipped, manager.setupForProfile("profile-a").first())
        assertTrue(requireNotNull(manager.setupForProfile("profile-a").first()).setupCompleted)
    }

    @Test fun allSixAgeRangesAndFiveOralOptionsRoundTripWithoutInterpretation() = runBlocking {
        assertEquals(6, CommunicationAgeRange.entries.size)
        assertEquals(5, OralCommunication.entries.size)
        CommunicationAgeRange.entries.forEach { age ->
            OralCommunication.entries.forEach { oral ->
                val chosen = CommunicationProfileSetup("profile-a", age, oral)
                manager.saveSetup(chosen)
                assertEquals(chosen, manager.setupForProfile("profile-a").first())
            }
        }
    }

    @Test fun deletionClearsOnlyTheRemovedProfilesSetupBeforeRemovingTheProfile() = runBlocking {
        manager.saveSetup(answers())
        manager.saveSetup(answers("profile-b"))
        manager.deleteProfile("profile-a")
        assertFalse(profiles.containsKey("profile-a"))
        assertNull(manager.setupForProfile("profile-a").first())
        assertEquals(answers("profile-b"), manager.setupForProfile("profile-b").first())
    }

    @Test fun deletingAnUnconfiguredProfileDoesNotCreateOrRemoveOtherRecords() = runBlocking {
        manager.saveSetup(answers("profile-b"))
        manager.deleteProfile("profile-a")
        assertEquals(answers("profile-b"), manager.setupForProfile("profile-b").first())
        assertEquals(1, dataStore.data.first().asMap().size)
    }

    @Test fun deletedProfileCannotBeSavedAgainAndNoOrphanCanBeCreated() = runBlocking {
        manager.deleteProfile("profile-a")
        expectFailure { manager.saveSetup(answers()) }
        assertNull(manager.setupForProfile("profile-a").first())
        expectFailure { manager.deleteProfile("unknown") }
        assertTrue(profiles.containsKey("profile-b"))
    }

    @Test fun failureRemovingSetupLeavesTheProfileInPlace() = runBlocking {
        val failingStore = object : DataStore<Preferences> {
            override val data = flowOf(emptyPreferences())
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                throw IOException("simulated local write failure")
            }
        }
        val failing = CommunicationProfileSetupManager(CommunicationSetupPreferences(failingStore), registry)
        expectFailure { failing.deleteProfile("profile-a") }
        assertTrue(profiles.containsKey("profile-a"))
    }

    @Test fun interruptedProfileDeletionCannotLeaveContextualDataOrphaned() = runBlocking {
        manager.saveSetup(answers())
        failProfileDelete = true
        expectFailure { manager.deleteProfile("profile-a") }
        assertTrue(profiles.containsKey("profile-a"))
        assertNull(manager.setupForProfile("profile-a").first())
    }

    @Test fun corruptOrUnknownRecordsFailExplicitlyWithoutChangingOtherProfiles() = runBlocking {
        manager.saveSetup(answers("profile-b"))
        listOf("{", """{"schemaVersion":2,"ageRange":"ADULT","oralCommunication":"WORDS_AND_PHRASES","setupCompleted":true}""",
            """{"schemaVersion":1,"ageRange":"INVALID","oralCommunication":"EMERGING_SPEECH","setupCompleted":true}""",
            """{"schemaVersion":1,"ageRange":"ADULT","oralCommunication":"EMERGING_SPEECH","setupCompleted":"true"}""").forEach { invalid ->
            dataStore.edit { it[stringPreferencesKey("profile:profile-a")] = invalid }
            expectFailure { manager.setupForProfile("profile-a").first() }
            assertEquals(answers("profile-b"), manager.setupForProfile("profile-b").first())
        }
    }

    @Test fun onlyContextualFieldsAreStoredAndIncompleteDraftsAreNotSaved() = runBlocking {
        manager.saveSetup(answers())
        val saved = dataStore.data.first().asMap()
        assertEquals(setOf("profile:profile-a"), saved.keys.map { it.name }.toSet())
        val value = saved.values.single().toString()
        assertFalse(value.contains("Mismo nombre"))
        assertFalse(value.contains("profileId"))
        expectFailure { manager.saveSetup(answers("profile-b").copy(setupCompleted = false)) }
        assertNull(manager.setupForProfile("profile-b").first())
    }

    @Test fun profileAnswersAreNotDependenciesOfCatalogOrMessagePresentation() {
        val root = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "app/src/main/assets/communication").isDirectory }
        val files = listOf("domain/communication/CommunicationMessageBuilder.kt",
            "ui/screens/communication/CommunicationHomeScreen.kt",
            "data/communication/LocalCommunicationCatalogRepository.kt")
        files.forEach { path ->
            val source = File(root, "app/src/main/java/com/neuronovaapps/rutinago/$path").readText()
            assertFalse(path, Regex("ageRange|oralCommunication|CommunicationAgeRange|OralCommunication").containsMatchIn(source))
        }
        assertTrue(File(root, "app/src/main/java/com/neuronovaapps/rutinago/data/communication/CommunicationSetupStorage.kt")
            .readText().contains("appContext.noBackupFilesDir"))
    }

    private suspend fun expectFailure(action: suspend () -> Unit) {
        try {
            action()
        } catch (_: Exception) {
            return
        }
        fail("Expected failure")
    }
}
