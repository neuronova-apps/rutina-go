package com.neuronovaapps.rutinago

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.neuronovaapps.rutinago.data.preferences.UserPreferences
import com.neuronovaapps.rutinago.data.preferences.UserPreferencesRepository
import com.neuronovaapps.rutinago.domain.communication.CommunicationAgeRange
import com.neuronovaapps.rutinago.domain.communication.OralCommunication
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CommunicationProfileSetupUiTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()
    private lateinit var repository: UserPreferencesRepository
    private lateinit var original: UserPreferences
    private val firstId = "setup-ui-${UUID.randomUUID()}"
    private val secondId = "setup-ui-${UUID.randomUUID()}"

    @Before fun prepareProfiles() = runBlocking {
        repository = UserPreferencesRepository(composeRule.activity)
        original = repository.preferences.first()
        repository.createProfile("Perfil configuración", firstId)
        repository.createProfile("Otro perfil configuración", secondId)
        repository.selectProfile(firstId)
    }

    @After fun cleanOnlyTestProfiles() = runBlocking {
        repository.deleteProfile(firstId)
        assertNull(repository.setupForProfile(firstId).first())
        repository.deleteProfile(secondId)
        assertNull(repository.setupForProfile(secondId).first())
        original.activeProfile?.id?.let { id ->
            if (repository.preferences.first().profiles.any { it.id == id }) repository.selectProfile(id)
        }
    }

    @Test fun firstEntryOptionalAnswersEditingAndRecreationPreserveTheMessage() {
        openUnconfigured()
        composeRule.onNodeWithTag("communication-setup-save").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag("communication-age-YEARS_9_TO_12").assertIsNotSelected()
        saveAnswers("NOT_SPECIFIED", "NOT_SPECIFIED")
        composeRule.awaitCommunicationCatalog()
        composeRule.scrollCommunicationTo("core-VOC_006")
        composeRule.onNodeWithTag("communication-core-VOC_006").performClick()
        composeRule.scrollCommunicationTo("VOC_015")
        composeRule.onNodeWithTag("communication-word-VOC_015").performClick()
        openEditor()
        composeRule.onNodeWithTag("communication-age-NOT_SPECIFIED").performScrollTo().assertIsSelected()
        choose("YEARS_9_TO_12", "EMERGING_SPEECH")
        systemBack()
        composeRule.awaitCommunicationCatalog()
        assertMessage()
        runBlocking { assertEquals(CommunicationAgeRange.NOT_SPECIFIED,
            repository.setupForProfile(firstId).first()?.ageRange) }
        openEditor()
        saveAnswers("YEARS_9_TO_12", "EMERGING_SPEECH")
        composeRule.awaitCommunicationCatalog()
        assertMessage()
        openEditor()
        composeRule.onNodeWithTag("communication-age-YEARS_9_TO_12").performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag("communication-oral-EMERGING_SPEECH").performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag("communication-setup-back").performScrollTo().performClick()
        composeRule.awaitCommunicationCatalog()
        assertMessage()
        composeRule.activityRule.scenario.recreate()
        composeRule.awaitCommunicationCatalog()
        assertMessage()
        assertAllVocabularyAvailable()
        composeRule.scrollCommunicationTo("back")
        composeRule.onNodeWithTag("communication-back-to-modules").performClick()
        composeRule.onNodeWithTag("module-selector").assertIsDisplayed()
        composeRule.onNodeWithTag("module-communication").performClick()
        composeRule.awaitCommunicationCatalog()
        composeRule.onNodeWithTag("communication-profile-setup").assertDoesNotExist()
        assertMessage()
    }

    @Test fun unconfiguredProfileCanReturnToSelectorUsingBothBackActions() {
        openUnconfigured()
        composeRule.onNodeWithTag("communication-setup-back").performScrollTo().performClick()
        composeRule.onNodeWithTag("module-selector").assertIsDisplayed()
        composeRule.onNodeWithTag("module-communication").performClick()
        awaitSetup()
        systemBack()
        composeRule.onNodeWithTag("module-selector").assertIsDisplayed()
        runBlocking { assertNull(repository.setupForProfile(firstId).first()) }
    }

    @Test fun profilesKeepIndependentAnswersAndDeletionLeavesNoOrphan() {
        openUnconfigured()
        saveAnswers("YEARS_9_TO_12", "EMERGING_SPEECH")
        composeRule.awaitCommunicationCatalog()
        composeRule.scrollCommunicationTo("back")
        composeRule.onNodeWithTag("communication-back-to-modules").performClick()
        runBlocking { repository.selectProfile(secondId) }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Perfil activo: Otro perfil configuración").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("module-communication").performClick()
        awaitSetup()
        saveAnswers("YEARS_3_TO_5", "NO_CURRENT_SPEECH")
        composeRule.awaitCommunicationCatalog()
        assertAllVocabularyAvailable()
        runBlocking {
            assertEquals(CommunicationAgeRange.YEARS_9_TO_12, repository.setupForProfile(firstId).first()?.ageRange)
            assertEquals(OralCommunication.EMERGING_SPEECH, repository.setupForProfile(firstId).first()?.oralCommunication)
            assertEquals(CommunicationAgeRange.YEARS_3_TO_5, repository.setupForProfile(secondId).first()?.ageRange)
            val file = java.io.File(composeRule.activity.noBackupFilesDir,
                com.neuronovaapps.rutinago.data.communication.CommunicationSetupStorage.FILE_NAME)
            assertTrue(file.isFile)
            repository.renameProfile(firstId, "Renombrado")
            assertEquals(CommunicationAgeRange.YEARS_9_TO_12, repository.setupForProfile(firstId).first()?.ageRange)
        }
    }

    private fun openUnconfigured() {
        composeRule.onNodeWithText("Comenzar").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Perfil activo: Perfil configuración").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("module-communication").performClick()
        awaitSetup()
    }
    private fun awaitSetup() = composeRule.waitUntil(10_000) {
        composeRule.onAllNodesWithTag("communication-profile-setup").fetchSemanticsNodes().isNotEmpty()
    }
    private fun choose(age: String, oral: String) {
        composeRule.onNodeWithTag("communication-age-$age").performScrollTo().performClick()
        composeRule.onNodeWithTag("communication-oral-$oral").performScrollTo().performClick()
    }
    private fun saveAnswers(age: String, oral: String) {
        choose(age, oral)
        composeRule.onNodeWithTag("communication-setup-save").performScrollTo().assertIsEnabled().performClick()
    }
    private fun openEditor() {
        composeRule.scrollCommunicationTo("personalize")
        composeRule.onNodeWithTag("communication-personalize").performClick()
        awaitSetup()
    }
    private fun assertMessage() {
        composeRule.scrollCommunicationTo("message")
        composeRule.onNodeWithTag("communication-message-item-1").assertTextEquals("Quiero")
        composeRule.onNodeWithTag("communication-message-item-2").assertTextEquals("Agua")
    }
    private fun assertAllVocabularyAvailable() {
        composeRule.scrollCommunicationTo("count")
        composeRule.onNodeWithText("132 palabras").assertIsDisplayed()
        listOf("HOME", "ROUTINES", "CREATE", "PROGRESS", "SETTINGS").forEach {
            composeRule.onNodeWithTag("destination-$it").assertDoesNotExist()
        }
    }
    private fun systemBack() {
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }
}
