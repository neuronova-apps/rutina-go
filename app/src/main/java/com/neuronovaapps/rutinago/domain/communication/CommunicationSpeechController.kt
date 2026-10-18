package com.neuronovaapps.rutinago.domain.communication

import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CommunicationSpeechRate(val label: String, val factor: Float) {
    SLOW("Lenta", .8f), NORMAL("Normal", 1f), FAST("Rápida", 1.2f);
}

interface CommunicationSpeechPreferencesRepository {
    fun speechRateForProfile(profileId: String): kotlinx.coroutines.flow.Flow<CommunicationSpeechRate>
    suspend fun setSpeechRate(profileId: String, rate: CommunicationSpeechRate)
}

enum class SpeechStatus { INITIALIZING, AVAILABLE, SPEAKING, STOPPED, UNAVAILABLE, ERROR }
enum class SpeechProblem(val explanation: String) {
    NO_ENGINE("No hay un motor de voz disponible. Revisa la síntesis de voz en la Configuración de Android."),
    INITIALIZATION("No se pudo iniciar la voz. Revisa la síntesis de voz de Android e inténtalo de nuevo."),
    NO_SPANISH("No hay una voz española compatible. Revisa los idiomas de síntesis de voz de Android."),
    MISSING_DATA("Faltan datos de voz en español. Puedes revisarlos en la Configuración de Android."),
    PLAYBACK("No se pudo reproducir el mensaje. Revisa el volumen y la salida de audio e inténtalo de nuevo."),
    MUTED("El volumen multimedia está silenciado. Revisa el volumen y la salida de audio."),
    TIMEOUT("El motor de voz no respondió. Puedes reintentar desde Configuración de ComunicaGo."),
    TOO_LONG("El mensaje supera el tamaño admitido por el motor. Puedes pronunciarlo en partes; el mensaje se conserva."),
}
data class SpeechVoice(val languageTag: String, val networkRequired: Boolean, val maxInputLength: Int = 4000)
data class SpeechVoiceCandidate(val id: String, val languageTag: String, val networkRequired: Boolean, val installed: Boolean)
fun preferredSpanishVoices(voices: List<SpeechVoiceCandidate>): List<SpeechVoiceCandidate> = voices
    .filter { it.installed && java.util.Locale.forLanguageTag(it.languageTag).language == "es" }
    .sortedWith(compareBy({ it.networkRequired },
        { java.util.Locale.forLanguageTag(it.languageTag).country != "PE" }, { it.languageTag }, { it.id }))
data class CommunicationSpeechState(
    val status: SpeechStatus = SpeechStatus.INITIALIZING,
    val ready: Boolean = false,
    val active: Boolean = false,
    val lastConfirmed: String? = null,
    val voice: SpeechVoice? = null,
    val problem: SpeechProblem? = null,
)

/** The only platform-dependent part is supplied through this interface. No audio is stored. */
interface CommunicationSpeechEngine {
    interface Listener {
        fun ready(voice: SpeechVoice)
        fun unavailable(problem: SpeechProblem)
        fun started(id: String)
        fun done(id: String)
        fun failed(id: String)
        fun stopped(id: String)
    }
    enum class Request { ACCEPTED, FAILED, MUTED }
    fun initialize(listener: Listener)
    /** Replace this client's previous utterance, never append an unbounded queue. */
    fun speak(text: String, rate: Float, id: String): Request
    fun stop()
    fun shutdown()
}

/** Joins exact stored labels: no IDs, grammar edits, case changes, or inserted vocabulary. */
fun communicationSpeechText(items: List<CommunicationMessageItem>) = items.joinToString(" ") { it.text }

class CommunicationSpeechController(
    private val scope: CoroutineScope,
    private val engineFactory: () -> CommunicationSpeechEngine,
    initialLastConfirmed: String? = null,
    private val onConfirmed: (String) -> Unit = {},
) {
    private val mutable = MutableStateFlow(CommunicationSpeechState(lastConfirmed = initialLastConfirmed))
    val state = mutable.asStateFlow()
    private var engine: CommunicationSpeechEngine? = null
    private var generation = 0L
    private var activeId: String? = null
    private var pendingText: String? = null
    private var started = false
    private var watchdog: Job? = null
    private var closed = false

    @Synchronized fun initialize() {
        if (closed || engine != null) return
        val session = ++generation
        mutable.value = mutable.value.copy(status = SpeechStatus.INITIALIZING, ready = false, problem = null)
        armTimeout(10_000, session, null)
        try {
            engine = engineFactory()
            engine!!.initialize(object : CommunicationSpeechEngine.Listener {
                override fun ready(voice: SpeechVoice) = receive(session) {
                    watchdog?.cancel()
                    mutable.value = mutable.value.copy(status = SpeechStatus.AVAILABLE, ready = true, voice = voice, problem = null)
                }
                override fun unavailable(problem: SpeechProblem) = receive(session) {
                    watchdog?.cancel()
                    mutable.value = mutable.value.copy(status = SpeechStatus.UNAVAILABLE, ready = false, problem = problem)
                }
                override fun started(id: String) = receive(session, id) {
                    started = true
                    mutable.value = mutable.value.copy(status = SpeechStatus.SPEAKING)
                    armTimeout(maxOf(30_000L, (pendingText?.length ?: 0) * 500L).coerceAtMost(1_800_000L), session, id)
                }
                override fun done(id: String) = receive(session, id) {
                    if (!started) { fail(SpeechProblem.PLAYBACK); return@receive }
                    val confirmed = requireNotNull(pendingText)
                    clearRequest()
                    mutable.value = mutable.value.copy(status = SpeechStatus.AVAILABLE, lastConfirmed = confirmed, problem = null)
                    onConfirmed(confirmed)
                }
                override fun failed(id: String) = receive(session, id) { fail(SpeechProblem.PLAYBACK) }
                override fun stopped(id: String) = receive(session, id) {
                    clearRequest()
                    mutable.value = mutable.value.copy(status = SpeechStatus.STOPPED)
                }
            })
        } catch (_: Exception) {
            mutable.value = mutable.value.copy(status = SpeechStatus.UNAVAILABLE, ready = false, problem = SpeechProblem.INITIALIZATION)
            watchdog?.cancel()
        }
    }

    @Synchronized private fun receive(session: Long, id: String? = null, action: () -> Unit) {
        if (!closed && session == generation && (id == null || id == activeId)) action()
    }

    @Synchronized fun speak(items: List<CommunicationMessageItem>, rate: CommunicationSpeechRate) =
        request(communicationSpeechText(items), rate)

    @Synchronized fun repeat(rate: CommunicationSpeechRate) { state.value.lastConfirmed?.let { request(it, rate) } }

    private fun request(text: String, rate: CommunicationSpeechRate) {
        if (closed || !state.value.ready || text.isBlank()) return
        if (text.length > requireNotNull(state.value.voice).maxInputLength) { fail(SpeechProblem.TOO_LONG); return }
        val id = UUID.randomUUID().toString()
        activeId = id
        pendingText = text
        started = false
        mutable.value = mutable.value.copy(active = true, status = SpeechStatus.AVAILABLE, problem = null)
        armTimeout(10_000, generation, id)
        val result = try { engine!!.speak(text, rate.factor, id) } catch (_: Exception) { CommunicationSpeechEngine.Request.FAILED }
        if (activeId != id) return // A synchronous callback already finished this request.
        when (result) {
            CommunicationSpeechEngine.Request.ACCEPTED -> Unit // Acceptance is not playback confirmation.
            CommunicationSpeechEngine.Request.MUTED -> fail(SpeechProblem.MUTED)
            CommunicationSpeechEngine.Request.FAILED -> fail(SpeechProblem.PLAYBACK)
        }
    }

    private fun armTimeout(millis: Long, session: Long, id: String?) {
        watchdog?.cancel()
        watchdog = scope.launch { delay(millis); receive(session, id) {
            activeId = null
            runCatching { engine?.stop() }
            fail(SpeechProblem.TIMEOUT)
            if (id == null) mutable.value = mutable.value.copy(ready = false)
        } }
    }
    private fun clearRequest() {
        watchdog?.cancel()
        activeId = null
        pendingText = null
        started = false
        mutable.value = mutable.value.copy(active = false)
    }
    private fun fail(problem: SpeechProblem) {
        clearRequest()
        runCatching { engine?.stop() }
        mutable.value = mutable.value.copy(status = SpeechStatus.ERROR, problem = problem)
    }
    @Synchronized fun stop() {
        if (closed) return
        if (!state.value.ready && state.value.status == SpeechStatus.INITIALIZING) {
            // Backgrounding during startup must not cancel the initialization watchdog.
            runCatching { engine?.stop() }
            return
        }
        clearRequest() // Ignore a late done/error before asking Android to stop.
        runCatching { engine?.stop() }
        if (state.value.ready) mutable.value = mutable.value.copy(status = SpeechStatus.STOPPED, problem = null)
    }
    @Synchronized fun retry() {
        if (closed) return
        releaseEngine()
        initialize()
    }
    private fun releaseEngine() {
        ++generation
        clearRequest()
        runCatching { engine?.stop() }
        runCatching { engine?.shutdown() }
        engine = null
    }
    @Synchronized fun close() {
        if (closed) return
        closed = true
        releaseEngine()
        mutable.value = mutable.value.copy(ready = false, status = SpeechStatus.STOPPED)
    }
}
