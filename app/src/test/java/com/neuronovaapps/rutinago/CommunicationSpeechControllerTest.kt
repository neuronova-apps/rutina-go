package com.neuronovaapps.rutinago

import com.neuronovaapps.rutinago.domain.communication.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CommunicationSpeechControllerTest {
    private class Engine : CommunicationSpeechEngine {
        lateinit var listener: CommunicationSpeechEngine.Listener
        val requests = mutableListOf<Triple<String, Float, String>>()
        var result = CommunicationSpeechEngine.Request.ACCEPTED
        var stops = 0
        var shutdowns = 0
        override fun initialize(listener: CommunicationSpeechEngine.Listener) { this.listener = listener }
        override fun speak(text: String, rate: Float, id: String): CommunicationSpeechEngine.Request {
            requests += Triple(text, rate, id); return result
        }
        override fun stop() { stops++ }
        override fun shutdown() { shutdowns++ }
        fun ready(max: Int = 4000) = listener.ready(SpeechVoice("es-PE", false, max))
        fun finish() { val id = requests.last().third; listener.started(id); listener.done(id) }
    }
    private fun words(vararg text: String) = text.mapIndexed { index, word -> CommunicationMessageItem(index + 1L, "VOC_${index + 1}", word) }
    private fun scenario(block: suspend TestScope.(CommunicationSpeechController, Engine) -> Unit) = runTest {
        val engine = Engine()
        val controller = CommunicationSpeechController(this, { engine })
        controller.initialize()
        try { block(controller, engine) } finally { controller.close() }
    }
    @Test fun emptyMessageIsEmptyText() { assertEquals("", communicationSpeechText(emptyList())) }
    @Test fun oneWordUsesItsLabelOnly() { assertEquals("Agua", communicationSpeechText(words("Agua"))) }
    @Test fun orderAndRepetitionsRemainExact() {
        assertEquals("Yo Quiero Agua Agua", communicationSpeechText(words("Yo", "Quiero", "Agua", "Agua")))
    }
    @Test fun grammarAndCaseAreNotCorrectedAndIdsAreNotSpoken() {
        val items = words("Agua", "yo", "No", "No")
        assertEquals("Agua yo No No", communicationSpeechText(items))
        assertFalse(communicationSpeechText(items).contains("VOC_"))
    }
    @Test fun emptyAndUnreadyMessagesNeverReachEngine() = scenario { c, e ->
        c.speak(words("Agua"), CommunicationSpeechRate.NORMAL)
        e.ready(); c.speak(emptyList(), CommunicationSpeechRate.NORMAL)
        assertTrue(e.requests.isEmpty()); assertFalse(c.state.value.active)
    }
    @Test fun initializationFailureAndMissingEngineAreRecoverable() = scenario { c, e ->
        e.listener.unavailable(SpeechProblem.NO_ENGINE)
        assertEquals(SpeechStatus.UNAVAILABLE, c.state.value.status)
        assertFalse(c.state.value.ready)
        c.speak(words("Agua"), CommunicationSpeechRate.NORMAL); assertTrue(e.requests.isEmpty())
    }
    @Test fun missingSpanishAndMissingDataNeverEnableSpeaking() = scenario { c, e ->
        for (reason in listOf(SpeechProblem.NO_SPANISH, SpeechProblem.MISSING_DATA, SpeechProblem.INITIALIZATION)) {
            e.listener.unavailable(reason)
            assertFalse(c.state.value.ready); assertEquals(reason, c.state.value.problem)
        }
    }
    @Test fun acceptedRequestDoesNotClaimPlaybackOrUpdateRepeat() = scenario { c, e ->
        e.ready(); c.speak(words("Agua"), CommunicationSpeechRate.NORMAL)
        assertEquals(SpeechStatus.AVAILABLE, c.state.value.status)
        assertTrue(c.state.value.active); assertNull(c.state.value.lastConfirmed)
        e.listener.started(e.requests.last().third)
        assertEquals(SpeechStatus.SPEAKING, c.state.value.status); assertNull(c.state.value.lastConfirmed)
    }
    @Test fun onlyStartedAndCompletedRequestBecomesRepeatable() = scenario { c, e ->
        e.ready(); c.speak(words("Quiero", "Agua"), CommunicationSpeechRate.NORMAL); e.finish()
        c.speak(emptyList(), CommunicationSpeechRate.NORMAL)
        c.repeat(CommunicationSpeechRate.SLOW)
        assertEquals("Quiero Agua", e.requests.last().first)
        assertEquals(.8f, e.requests.last().second, 0f)
        assertEquals("Quiero Agua", c.state.value.lastConfirmed)
    }
    @Test fun doneWithoutStartDoesNotOverwriteLastValidMessage() = scenario { c, e ->
        e.ready(); c.speak(words("Agua"), CommunicationSpeechRate.NORMAL); e.finish()
        c.speak(words("Ayuda"), CommunicationSpeechRate.NORMAL)
        e.listener.done(e.requests.last().third)
        assertEquals("Agua", c.state.value.lastConfirmed); assertEquals(SpeechStatus.ERROR, c.state.value.status)
    }
    @Test fun stopDoesNotChangeMessageOrLastConfirmed() = scenario { c, e ->
        val message = words("Yo", "Agua")
        e.ready(); c.speak(message, CommunicationSpeechRate.NORMAL); e.finish()
        c.repeat(CommunicationSpeechRate.NORMAL); val stale = e.requests.last().third
        c.stop(); e.listener.done(stale)
        assertEquals(words("Yo", "Agua"), message)
        assertEquals("Yo Agua", c.state.value.lastConfirmed); assertFalse(c.state.value.active)
    }
    @Test fun consecutiveRequestsReplaceAndIgnoreOldStartDoneAndError() = scenario { c, e ->
        e.ready(); c.speak(words("Agua"), CommunicationSpeechRate.NORMAL); val old = e.requests.last().third
        c.speak(words("Ayuda"), CommunicationSpeechRate.FAST); val current = e.requests.last().third
        assertNotEquals(old, current)
        e.listener.started(old); e.listener.done(old); e.listener.failed(old); e.listener.stopped(old)
        assertTrue(c.state.value.active); assertNull(c.state.value.lastConfirmed)
        e.finish(); assertEquals("Ayuda", c.state.value.lastConfirmed)
    }
    @Test fun playbackFailureRetainsEarlierConfirmationAndBuilderSnapshot() = scenario { c, e ->
        e.ready(); val message = words("Agua"); c.speak(message, CommunicationSpeechRate.NORMAL); e.finish()
        c.speak(words("No"), CommunicationSpeechRate.NORMAL); e.listener.failed(e.requests.last().third)
        assertEquals("Agua", c.state.value.lastConfirmed); assertEquals(words("Agua"), message)
    }
    @Test fun mutedOutputRetainsMessageAndReportsVolume() = scenario { c, e ->
        e.ready(); e.result = CommunicationSpeechEngine.Request.MUTED
        c.speak(words("Agua"), CommunicationSpeechRate.NORMAL)
        assertEquals(SpeechProblem.MUTED, c.state.value.problem); assertFalse(c.state.value.active)
    }
    @Test fun rejectedRequestDoesNotErasePreviousSuccessfulMessage() = scenario { c, e ->
        e.ready(); c.speak(words("Agua"), CommunicationSpeechRate.NORMAL); e.finish()
        e.result = CommunicationSpeechEngine.Request.FAILED
        c.speak(words("No"), CommunicationSpeechRate.NORMAL)
        assertEquals("Agua", c.state.value.lastConfirmed); assertEquals(SpeechProblem.PLAYBACK, c.state.value.problem)
    }
    @Test fun closeStopsOnceShutsDownAndIgnoresObsoleteCallbacks() = scenario { c, e ->
        e.ready(); c.speak(words("Agua"), CommunicationSpeechRate.NORMAL); val id = e.requests.last().third
        c.close(); c.close(); e.listener.started(id); e.listener.done(id); e.ready()
        assertEquals(1, e.shutdowns); assertFalse(c.state.value.ready); assertNull(c.state.value.lastConfirmed)
        c.repeat(CommunicationSpeechRate.NORMAL); c.speak(words("No"), CommunicationSpeechRate.NORMAL)
        assertEquals(1, e.requests.size)
    }
    @Test fun retryIgnoresInitializationAndUtterancesFromPreviousEngine() = runTest {
        val a = Engine(); val b = Engine(); val queue = ArrayDeque(listOf(a, b))
        val c = CommunicationSpeechController(this, { queue.removeFirst() })
        try {
            c.initialize(); a.ready(); c.speak(words("Agua"), CommunicationSpeechRate.NORMAL)
            c.retry(); a.ready(); a.finish()
            assertEquals(SpeechStatus.INITIALIZING, c.state.value.status); assertNull(c.state.value.lastConfirmed)
            b.ready(); c.speak(words("No"), CommunicationSpeechRate.NORMAL); b.finish()
            assertEquals("No", c.state.value.lastConfirmed); assertEquals(1, a.shutdowns)
        } finally { c.close() }
    }
    @Test fun initializationTimeoutDoesNotSpeakQueuedText() = scenario { c, e ->
        advanceTimeBy(10_001); runCurrent()
        assertEquals(SpeechProblem.TIMEOUT, c.state.value.problem)
        e.ready(); assertTrue(e.requests.isEmpty())
    }
    @Test fun backgroundDuringInitializationKeepsTheTimeoutAndNeverQueuesSpeech() = scenario { c, e ->
        c.stop()
        advanceTimeBy(10_001); runCurrent()
        assertEquals(SpeechProblem.TIMEOUT, c.state.value.problem)
        e.ready()
        assertTrue(e.requests.isEmpty())
    }
    @Test fun startTimeoutStopsAndIgnoresLateCompletion() = scenario { c, e ->
        e.ready(); c.speak(words("Agua"), CommunicationSpeechRate.NORMAL); val id = e.requests.last().third
        advanceTimeBy(10_001); runCurrent(); e.listener.started(id); e.listener.done(id)
        assertEquals(SpeechProblem.TIMEOUT, c.state.value.problem); assertNull(c.state.value.lastConfirmed)
    }
    @Test fun hungPlaybackIsStoppedWithoutChangingLastConfirmed() = scenario { c, e ->
        e.ready(); c.speak(words("Agua"), CommunicationSpeechRate.NORMAL); e.listener.started(e.requests.last().third)
        advanceTimeBy(30_001); runCurrent()
        assertEquals(SpeechProblem.TIMEOUT, c.state.value.problem); assertFalse(c.state.value.active)
    }
    @Test fun oversizedTextIsRejectedWholeWithoutTruncationOrReplacingRepeat() = scenario { c, e ->
        e.ready(5); c.speak(words("Agua"), CommunicationSpeechRate.NORMAL); e.finish()
        c.speak(words("Quiero", "Agua"), CommunicationSpeechRate.NORMAL)
        assertEquals(1, e.requests.size); assertEquals("Agua", c.state.value.lastConfirmed)
        assertEquals(SpeechProblem.TOO_LONG, c.state.value.problem)
    }
    @Test fun profileControllersNeverShareLastSpokenText() = scenario { a, engineA ->
        engineA.ready(); a.speak(words("Agua"), CommunicationSpeechRate.NORMAL); engineA.finish(); a.close()
        val engineB = Engine(); val b = CommunicationSpeechController(this, { engineB })
        try { b.initialize(); engineB.ready(); b.repeat(CommunicationSpeechRate.NORMAL)
            assertNull(b.state.value.lastConfirmed); assertTrue(engineB.requests.isEmpty())
        } finally { b.close() }
    }
    @Test fun ratesAreExplicitAndNormalIsOne() {
        assertEquals(listOf(.8f, 1f, 1.2f), CommunicationSpeechRate.entries.map { it.factor })
    }
    @Test fun engineFactoryFailureIsContained() = runTest {
        val c = CommunicationSpeechController(this, { error("synthetic failure") })
        try { c.initialize(); assertEquals(SpeechProblem.INITIALIZATION, c.state.value.problem) }
        finally { c.close() }
    }
    @Test fun spanishVoiceSelectionPrefersInstalledLocalPeruvianThenOtherSpanish() {
        val voices = listOf(SpeechVoiceCandidate("en", "en-US", false, true),
            SpeechVoiceCandidate("pe-network", "es-PE", true, true), SpeechVoiceCandidate("es", "es-ES", false, true),
            SpeechVoiceCandidate("pe", "es-PE", false, true), SpeechVoiceCandidate("missing", "es-PE", false, false))
        assertEquals(listOf("pe", "es", "pe-network"), preferredSpanishVoices(voices).map { it.id })
        assertEquals(listOf("es"), preferredSpanishVoices(voices.filter { it.id == "es" || it.id == "en" }).map { it.id })
        assertTrue(preferredSpanishVoices(voices.filter { it.id in listOf("en", "missing") }).isEmpty())
    }
}
