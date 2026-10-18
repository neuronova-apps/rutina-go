package com.neuronovaapps.rutinago

import androidx.test.platform.app.InstrumentationRegistry
import com.neuronovaapps.rutinago.data.communication.AndroidCommunicationSpeechEngine
import com.neuronovaapps.rutinago.domain.communication.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Real installed Android engine discovery/init; deliberately makes no claim about audible output. */
class CommunicationAndroidSpeechEngineTest {
    @Test fun localAndroidVoiceConfirmsSpeakRepeatAndStopThroughRealCallbacks() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val controller = CommunicationSpeechController(scope, { AndroidCommunicationSpeechEngine(context) })
        val report = File(context.getExternalFilesDir(null), "phase35-native-playback.txt")
        try {
            withContext(Dispatchers.Main) { controller.initialize() }
            val initialized = withTimeout(20_000) { controller.state.first {
                it.status in listOf(SpeechStatus.AVAILABLE, SpeechStatus.UNAVAILABLE, SpeechStatus.ERROR)
            } }
            report.writeText("PENDIENTE idioma=${initialized.voice?.languageTag} motivo=${initialized.problem?.name}; audibilidad no validada")
            org.junit.Assume.assumeTrue("Se necesita una voz local española para esta prueba",
                initialized.ready && initialized.voice?.networkRequired == false)
            val items = listOf(CommunicationMessageItem(1, "VOC_001", "Yo"),
                CommunicationMessageItem(2, "VOC_006", "Quiero"), CommunicationMessageItem(3, "VOC_015", "Agua"))
            withContext(Dispatchers.Main) {
                controller.speak(items, CommunicationSpeechRate.NORMAL)
                assertTrue(controller.state.value.active)
            }
            val spoken = withTimeout(20_000) { controller.state.first { !it.active } }
            assertEquals("Yo Quiero Agua", spoken.lastConfirmed)
            assertEquals(SpeechStatus.AVAILABLE, spoken.status)
            withContext(Dispatchers.Main) {
                controller.repeat(CommunicationSpeechRate.SLOW)
                assertTrue(controller.state.value.active)
            }
            val repeated = withTimeout(20_000) { controller.state.first { !it.active } }
            assertEquals(SpeechStatus.AVAILABLE, repeated.status)
            assertEquals("Yo Quiero Agua", repeated.lastConfirmed)
            withContext(Dispatchers.Main) {
                controller.speak(items, CommunicationSpeechRate.FAST)
                assertTrue(controller.state.value.active)
                controller.stop()
            }
            assertFalse(controller.state.value.active)
            assertEquals("Yo Quiero Agua", controller.state.value.lastConfirmed)
            report.writeText("CALLBACKS_REALES_CONFIRMADOS idioma=${initialized.voice?.languageTag}; Hablar y Repetir completados; Detener verificado; audibilidad no validada")
        } finally { withContext(NonCancellable + Dispatchers.Main) { controller.close(); scope.cancel() } }
    }
    @Test fun installedAndroidEngineReportsSpanishAvailabilityOrAnExplicitFailure() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val terminal = CompletableDeferred<String>()
        val engine = withContext(Dispatchers.Main) { AndroidCommunicationSpeechEngine(context).also { adapter ->
            adapter.initialize(object : CommunicationSpeechEngine.Listener {
                override fun ready(voice: SpeechVoice) {
                    if (!voice.languageTag.startsWith("es")) terminal.completeExceptionally(AssertionError("Non-Spanish voice"))
                    else terminal.complete("DISPONIBLE idioma=${voice.languageTag} requiere_red=${voice.networkRequired}; audibilidad no validada")
                }
                override fun unavailable(problem: SpeechProblem) {
                    assertTrue(problem in listOf(SpeechProblem.NO_ENGINE, SpeechProblem.INITIALIZATION, SpeechProblem.NO_SPANISH, SpeechProblem.MISSING_DATA))
                    terminal.complete("NO_DISPONIBLE motivo=${problem.name}; audibilidad no validada")
                }
                override fun started(id: String) = Unit
                override fun done(id: String) = Unit
                override fun failed(id: String) = Unit
                override fun stopped(id: String) = Unit
            })
        } }
        try {
            val result = withTimeout(20_000) { terminal.await() }
            File(context.getExternalFilesDir(null), "phase35-real-engine.txt").writeText(result)
            assertTrue(result.isNotBlank())
        } finally { withContext(Dispatchers.Main) { engine.shutdown() } }
    }
}
