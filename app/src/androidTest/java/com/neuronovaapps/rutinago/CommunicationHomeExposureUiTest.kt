package com.neuronovaapps.rutinago

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.neuronovaapps.rutinago.data.communication.LocalCommunicationCatalogRepository
import com.neuronovaapps.rutinago.data.preferences.UserPreferences
import com.neuronovaapps.rutinago.data.preferences.UserPreferencesRepository
import com.neuronovaapps.rutinago.data.preferences.TextScaleMode
import com.neuronovaapps.rutinago.domain.communication.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Exercises the real MainActivity -> module shell -> profile entry, with legacy profile records. */
class CommunicationHomeExposureUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var repository: UserPreferencesRepository
    private lateinit var original: UserPreferences
    private val profile = "home-exposure-${UUID.randomUUID()}"
    @Before fun prepareLegacyProfile() = runBlocking {
        repository = UserPreferencesRepository(rule.activity)
        original = repository.preferences.first()
        val catalog = LocalCommunicationCatalogRepository {
            rule.activity.assets.open(LocalCommunicationCatalogRepository.ASSET_PATH).bufferedReader().use { it.readText() }
        }
        repository.createProfile("Perfil anterior a Inicio personalizado", profile)
        repository.saveSetup(CommunicationProfileSetup(profile, CommunicationAgeRange.YEARS_9_TO_12, OralCommunication.EMERGING_SPEECH))
        repository.saveVocabulary(profile, catalog.vocabulary.map { it.vocabId }.toSet())
        repository.selectProfile(profile)
        // Exactly the pre-3.4.4.1 shape: setup and available vocabulary, no Home record.
        assertNull(repository.homeVocabularyForProfile(profile).first())
    }
    @After fun cleanup(): Unit = runBlocking {
        repository.deleteProfile(profile)
        original.activeProfile?.id?.let { repository.selectProfile(it) }
        repository.setTextScaleMode(original.textScaleMode)
        repository.setHighContrast(original.highContrast)
    }
    private fun openCommunication() {
        rule.onNodeWithText("Comenzar").performClick()
        rule.waitUntil(15_000) { rule.onAllNodesWithTag("module-selector").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("module-communication").performClick()
        rule.awaitCommunicationCatalog()
    }
    private fun capture(name: String) {
        rule.waitForIdle()
        val folder = File(rule.activity.getExternalFilesDir(null), "home-exposure-evidence").apply { mkdirs() }
        File(folder, "$name.txt").writeText(rule.onRoot(useUnmergedTree = true).printToString())
        // Capture the actual display without PixelCopy's short renderer timeout on an emulator.
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            File(folder, "$name.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally { bitmap.recycle() }
    }
    private fun awaitAssignment(value: Boolean) {
        rule.waitUntil(15_000) { runBlocking { repository.homeVocabularyForProfile(profile).first()?.let { ("VOC_015" in it) == value } == true } }
    }
    @Test fun organizationIsVisibleAtEntryWithoutSearchingOrScrolling() {
        runBlocking { repository.setPresentationMode(profile, CommunicationPresentationMode.GUIDED) }
        openCommunication()
        capture("entry")
        try {
            rule.onNodeWithTag("communication-organize-home").assertIsDisplayed().assertHasClickAction()
        } finally {
            // A second capture distinguishes a missing callback from a control below the fold.
            rule.onNodeWithTag("communication-home").performScrollToKey("organize-home")
            rule.onNodeWithTag("communication-organize-home").assertIsDisplayed()
            capture("entry-after-scroll")
        }
        rule.onNodeWithTag("communication-destination-MY_VOCABULARY").performClick()
        capture("vocabulary-entry")
        rule.onNodeWithTag("vocabulary-organize-home").assertIsDisplayed().performClick()
        rule.onNodeWithTag("home-vocabulary-organization").assertIsDisplayed()
    }
    @Test fun organizationIsVisibleAtEntryInFullView() {
        runBlocking { repository.setPresentationMode(profile, CommunicationPresentationMode.FULL) }
        openCommunication()
        capture("entry-full")
        rule.onNodeWithTag("communication-organize-home").assertIsDisplayed().assertHasClickAction()
        rule.onNodeWithTag("communication-destination-MY_VOCABULARY").performClick()
        capture("vocabulary-entry-full")
        rule.onNodeWithTag("vocabulary-organize-home").assertIsDisplayed().performClick()
        rule.onNodeWithTag("home-vocabulary-organization").assertIsDisplayed()
        rule.onNodeWithTag("home-vocabulary-organization").performScrollToKey("filter")
        rule.onNodeWithTag("home-organization-available").performScrollTo().assertIsSelected()
    }
    @Test fun legacyProfileCanAddUseRemoveReAddAndRestoreThroughTheRealApp() {
        openCommunication()
        rule.onNodeWithTag("communication-home").performScrollToKey("organize-home")
        rule.onNodeWithTag("communication-organize-home").performClick()
        val organizer = rule.onNodeWithTag("home-vocabulary-organization")
        organizer.performScrollToKey("search")
        rule.onNodeWithTag("home-organization-search").performTextReplacement("VOC_015")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        organizer.performScrollToKey("filter")
        rule.onNodeWithTag("home-organization-available").performScrollTo().assertIsSelected()
        organizer.performScrollToKey("VOC_015")
        rule.onNodeWithTag("home-assignment-VOC_015").performScrollTo().performClick()
        awaitAssignment(true)
        organizer.performScrollToKey("back")
        rule.onNodeWithTag("home-organization-back").performClick()
        rule.onNodeWithTag("communication-destination-HOME").assertIsSelected()
        rule.onNodeWithTag("communication-home").performScrollToKey("core-VOC_015")
        rule.onNodeWithTag("communication-core-VOC_015").assertIsDisplayed().performClick()
        rule.scrollCommunicationTo("message")
        rule.onNodeWithTag("communication-message-item-1").assertTextEquals("Agua")
        rule.onNodeWithTag("communication-home").performScrollToKey("organize-home")
        rule.onNodeWithTag("communication-organize-home").performClick()
        organizer.performScrollToKey("VOC_015")
        rule.onNodeWithTag("home-assignment-VOC_015").performScrollTo().performClick()
        awaitAssignment(false)
        organizer.performScrollToKey("back")
        rule.onNodeWithTag("home-organization-back").performClick()
        rule.onNodeWithTag("communication-core-VOC_015").assertDoesNotExist()
        rule.selectCommunicationCategory("CAT_003")
        rule.onNodeWithTag("communication-category-grid").performScrollToKey("VOC_015")
        rule.onNodeWithTag("communication-word-VOC_015").assertIsDisplayed()
        rule.onNodeWithTag("home-assignment-VOC_015").performScrollTo().performClick()
        awaitAssignment(true)
        rule.onNodeWithTag("communication-compact-message").assertTextEquals("Mensaje: Agua")
        rule.activityRule.scenario.recreate()
        rule.awaitCommunicationCatalog()
        rule.onNodeWithTag("communication-destination-HOME").performClick()
        rule.onNodeWithTag("communication-home").performScrollToKey("core-VOC_015")
        rule.onNodeWithTag("communication-core-VOC_015").assertIsDisplayed()
        rule.scrollCommunicationTo("message")
        rule.onNodeWithTag("communication-message-item-1").assertTextEquals("Agua")
        capture("legacy-profile-cycle-complete")
    }
    @Test fun organizationIsVisibleWithVeryLargeTextAndHighContrast() {
        runBlocking {
            repository.setTextScaleMode(TextScaleMode.VERY_LARGE)
            repository.setHighContrast(true)
        }
        openCommunication()
        capture("entry-large-contrast")
        rule.onNodeWithTag("communication-organize-home").assertIsDisplayed().performClick()
        rule.onNodeWithTag("home-vocabulary-organization").assertIsDisplayed()
        rule.onNodeWithTag("home-vocabulary-organization").performScrollToKey("back")
        rule.onNodeWithTag("home-organization-back").performClick()
        rule.onNodeWithTag("communication-destination-MY_VOCABULARY").performClick()
        capture("vocabulary-entry-large-contrast")
        rule.onNodeWithTag("vocabulary-organize-home").assertIsDisplayed().performClick()
        rule.onNodeWithTag("home-vocabulary-organization").assertIsDisplayed()
    }
}
