package com.neuronovaapps.rutinago

import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import com.neuronovaapps.rutinago.data.communication.LocalCommunicationCatalogRepository
import com.neuronovaapps.rutinago.data.preferences.*
import com.neuronovaapps.rutinago.domain.communication.*
import com.neuronovaapps.rutinago.ui.screens.communication.CommunicationProfileEntryScreen
import com.neuronovaapps.rutinago.ui.theme.RutinaGoTheme
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Actual profile entry + real DataStore + injected engine. This suite does not validate audible audio. */
class CommunicationSpeechUiTest {
    @get:Rule val rule = createComposeRule()
    private val a = "speech-a-${UUID.randomUUID()}"
    private val b = "speech-b-${UUID.randomUUID()}"
    private var profile by mutableStateOf(a)
    private var shown by mutableStateOf(true)
    private lateinit var repository: UserPreferencesRepository
    private lateinit var original: UserPreferences
    private val engines = mutableListOf<FakeSpeechEngine>()
    private var failure: SpeechProblem? = null
    @Before fun prepare() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = UserPreferencesRepository(context)
        original = repository.preferences.first()
        val catalog = LocalCommunicationCatalogRepository { context.assets.open(LocalCommunicationCatalogRepository.ASSET_PATH).bufferedReader().use { it.readText() } }
        for (id in listOf(a, b)) {
            repository.createProfile(if (id == a) "Voz A" else "Voz B", id)
            repository.saveSetup(CommunicationProfileSetup(id, CommunicationAgeRange.YEARS_9_TO_12, OralCommunication.NO_CURRENT_SPEECH))
            repository.saveVocabulary(id, catalog.vocabulary.map { it.vocabId }.toSet())
        }
    }
    @After fun cleanup(): Unit = runBlocking {
        rule.runOnIdle { shown = false }; rule.waitForIdle()
        repository.deleteProfile(a); repository.deleteProfile(b)
        original.activeProfile?.id?.let { repository.selectProfile(it) }
    }
    @Composable private fun App() {
        RutinaGoTheme {
            if (shown) CommunicationProfileEntryScreen(profile, if (profile == a) "Voz A" else "Voz B",
                speechEngineFactory = { FakeSpeechEngine(failure).also { engines += it } }, onBackToModules = { shown = false })
            else androidx.compose.material3.Text("Selector de módulos", androidx.compose.ui.Modifier.testTag("speech-selector"))
        }
    }
    private fun open() { rule.setContent { App() }; rule.awaitCommunicationCatalog() }
    private fun message() { rule.scrollCommunicationTo("message"); rule.onNodeWithTag("communication-speak").performScrollTo() }
    private fun buildMessage() {
        for (id in listOf("VOC_001", "VOC_006")) {
            rule.onNodeWithTag("communication-home").performScrollToKey("core-$id")
            rule.onNodeWithTag("communication-core-$id").performClick()
        }
        rule.selectCommunicationCategory("CAT_003")
        rule.onNodeWithTag("communication-category-grid").performScrollToKey("VOC_015")
        rule.onNodeWithTag("communication-word-VOC_015").performClick()
        rule.onNodeWithTag("communication-view-full-message").performClick()
        message()
    }
    @Test fun speakRepeatAndStopAreIndependentFromMessageEditing() {
        open(); message()
        rule.onNodeWithTag("communication-speak").assertIsNotEnabled()
        rule.onNodeWithTag("communication-speech-stop").assertIsNotEnabled()
        buildMessage()
        rule.captureSpeechEvidence("message-ready")
        assertTrue(engines.single().requests.isEmpty()) // No automatic speech per card.
        rule.onNodeWithTag("communication-speak").assertIsEnabled().performClick()
        val engine = engines.single()
        assertEquals("Yo Quiero Agua", engine.requests.last().text)
        rule.onNodeWithTag("communication-speech-repeat").assertDoesNotExist()
        rule.runOnIdle { engine.finish() }
        rule.onNodeWithTag("communication-clear").performScrollTo().performClick()
        rule.onNodeWithTag("communication-speak").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("communication-speech-repeat").performScrollTo().performClick()
        assertEquals("Yo Quiero Agua", engine.requests.last().text)
        rule.onNodeWithTag("communication-speech-stop").performScrollTo().assertIsEnabled().performClick()
        rule.onNodeWithTag("communication-speech-stop").assertIsNotEnabled()
        rule.onNodeWithText("Tu mensaje aparecerá aquí").assertExists()
        rule.onNodeWithTag("communication-undo-clear").performScrollTo().performClick()
        rule.onNodeWithTag("communication-message-item-1").assertTextEquals("Yo")
    }
    @Test fun consecutiveRequestsAndProfileChangeIgnoreOldCallbacks() {
        open(); buildMessage()
        val previous = engines.single()
        rule.onNodeWithTag("communication-speak").performClick()
        rule.onNodeWithTag("communication-speak").performClick()
        assertNotEquals(previous.requests[0].id, previous.requests[1].id)
        rule.runOnIdle { previous.finish(previous.requests[0].id) }
        rule.onNodeWithTag("communication-speech-repeat").assertDoesNotExist()
        rule.runOnIdle { profile = b }
        rule.awaitCommunicationCatalog()
        message()
        assertEquals(1, previous.shutdowns)
        rule.runOnIdle { previous.finish() }
        rule.onNodeWithTag("communication-speech-repeat").assertDoesNotExist()
        rule.onNodeWithTag("communication-speak").assertIsNotEnabled()
        assertTrue(engines.last().requests.isEmpty())
    }
    @Test fun speedAndLastConfirmedSurviveRestorationWithoutAutomaticPlayback() {
        val restorer = StateRestorationTester(rule)
        restorer.setContent { App() }; rule.awaitCommunicationCatalog(); buildMessage()
        rule.onNodeWithTag("communication-speak").performClick()
        rule.runOnIdle { engines.single().finish() }
        rule.onNodeWithTag("communication-destination-SETTINGS").performClick()
        rule.onNodeWithTag("communication-voice-rate-SLOW").performScrollTo().performClick()
        rule.waitUntil(10_000) { runBlocking { repository.speechRateForProfile(a).first() == CommunicationSpeechRate.SLOW } }
        rule.captureSpeechEvidence("speed-slow")
        restorer.emulateSavedInstanceStateRestore()
        rule.awaitCommunicationCatalog()
        rule.onNodeWithTag("communication-voice-rate-SLOW").performScrollTo().assertIsSelected()
        assertEquals(1, engines.first().shutdowns)
        assertTrue(engines.last().requests.isEmpty())
        message(); rule.onNodeWithTag("communication-speech-repeat").performScrollTo().performClick()
        assertEquals(.8f, engines.last().requests.last().rate, 0f)
        assertEquals("Yo Quiero Agua", engines.last().requests.last().text)
        rule.onNodeWithTag("communication-destination-SETTINGS").performClick()
        rule.onNodeWithTag("communication-back-to-modules").performScrollTo().performClick()
        rule.onNodeWithTag("speech-selector").assertIsDisplayed()
        assertEquals(1, engines.last().shutdowns)
    }
    @Test fun unavailableSpanishDisablesVoiceAndPreservesAllCommunicationTabs() {
        failure = SpeechProblem.MISSING_DATA
        open(); buildMessage()
        rule.onNodeWithTag("communication-speak").assertIsNotEnabled()
        rule.onNodeWithTag("communication-speech-status").assertTextEquals("Faltan datos de voz en español. Puedes revisarlos en la Configuración de Android.")
        CommunicationDestination.entries.forEach { rule.onNodeWithTag("communication-destination-${it.name}").assertExists() }
        rule.onNodeWithTag("communication-destination-SETTINGS").performClick()
        rule.onNodeWithTag("communication-speech-retry").performScrollTo().assertIsEnabled()
        rule.onNodeWithTag("communication-voice-rate-NORMAL").performScrollTo().assertIsSelected()
        message(); rule.onNodeWithTag("communication-message-item-3").assertTextEquals("Agua")
    }
    @Test fun ageOrOralAnswersAndPresentationNeverDisableVoiceOrCreateAnotherEngine() {
        open(); buildMessage()
        for (age in CommunicationAgeRange.entries) for (oral in OralCommunication.entries) {
            runBlocking { repository.saveSetup(CommunicationProfileSetup(a, age, oral)) }
            message(); rule.onNodeWithTag("communication-speak").assertIsEnabled()
        }
        for (mode in CommunicationPresentationMode.entries) {
            runBlocking { repository.setPresentationMode(a, mode) }
            message(); rule.onNodeWithTag("communication-speak").assertIsEnabled()
        }
        assertEquals(1, engines.size)
    }
}

internal class FakeSpeechEngine(private val problem: SpeechProblem? = null) : CommunicationSpeechEngine {
    data class Request(val text: String, val rate: Float, val id: String)
    lateinit var listener: CommunicationSpeechEngine.Listener
    val requests = mutableListOf<Request>()
    var stops = 0
    var shutdowns = 0
    override fun initialize(listener: CommunicationSpeechEngine.Listener) {
        this.listener = listener
        if (problem == null) listener.ready(SpeechVoice("es-PE", false)) else listener.unavailable(problem)
    }
    override fun speak(text: String, rate: Float, id: String): CommunicationSpeechEngine.Request {
        requests += Request(text, rate, id); return CommunicationSpeechEngine.Request.ACCEPTED
    }
    override fun stop() { stops++ }
    override fun shutdown() { shutdowns++ }
    fun finish(id: String = requests.last().id) { listener.started(id); listener.done(id) }
}

internal fun androidx.compose.ui.test.junit4.ComposeContentTestRule.captureSpeechEvidence(name: String) {
    waitForIdle()
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.waitForIdleSync()
    android.os.SystemClock.sleep(200)
    val folder = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "tts-evidence").apply { mkdirs() }
    java.io.File(folder, "$name.txt").writeText(onRoot(useUnmergedTree = true).printToString())
    val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
    try { java.io.File(folder, "$name.png").outputStream().use {
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
    } } finally { bitmap.recycle() }
}
