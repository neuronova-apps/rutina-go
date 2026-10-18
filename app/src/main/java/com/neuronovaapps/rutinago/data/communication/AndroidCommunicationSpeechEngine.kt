package com.neuronovaapps.rutinago.data.communication

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.neuronovaapps.rutinago.domain.communication.*
import java.util.Locale

/** Android adapter only; application context, no downloads, audio files, or message logging. */
class AndroidCommunicationSpeechEngine(context: Context) : CommunicationSpeechEngine {
    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var closed = false

    override fun initialize(listener: CommunicationSpeechEngine.Listener) {
        try {
            @Suppress("DEPRECATION")
            val services = context.packageManager.queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0)
            if (services.isEmpty()) { listener.unavailable(SpeechProblem.NO_ENGINE); return }
            tts = TextToSpeech(context) { result -> handler.post {
                if (closed) return@post
                val engine = tts
                if (result != TextToSpeech.SUCCESS || engine == null) {
                    listener.unavailable(SpeechProblem.INITIALIZATION)
                } else configure(engine, listener)
            } }
        } catch (_: Exception) { listener.unavailable(SpeechProblem.INITIALIZATION) }
    }

    private fun configure(engine: TextToSpeech, listener: CommunicationSpeechEngine.Listener) {
        try {
            val listenerResult = engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                private fun deliver(action: () -> Unit) { handler.post { if (!closed) action() } }
                override fun onStart(id: String) = deliver { listener.started(id) }
                override fun onDone(id: String) = deliver { listener.done(id) }
                @Deprecated("Required legacy callback")
                override fun onError(id: String) = deliver { listener.failed(id) }
                override fun onError(id: String, errorCode: Int) = deliver { listener.failed(id) }
                override fun onStop(id: String, interrupted: Boolean) = deliver { listener.stopped(id) }
            })
            if (listenerResult != TextToSpeech.SUCCESS) { listener.unavailable(SpeechProblem.INITIALIZATION); return }
            // Prefer installed local Spanish; es-PE first within that group. Never fall back to English.
            val voices = engine.voices.orEmpty().associateBy { it.name }
            val spanish = preferredSpanishVoices(voices.values.map {
                SpeechVoiceCandidate(it.name, it.locale.toLanguageTag(), it.isNetworkConnectionRequired,
                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty())
            }).map { voices.getValue(it.id) }
            var chosen = spanish.firstOrNull { engine.setVoice(it) == TextToSpeech.SUCCESS }
            if (chosen == null && spanish.isEmpty()) {
                val available = engine.isLanguageAvailable(Locale.forLanguageTag("es-PE"))
                if (available >= TextToSpeech.LANG_AVAILABLE && engine.setLanguage(Locale.forLanguageTag("es-PE")) >= 0) {
                    chosen = engine.voice?.takeIf { it.locale.language == "es" &&
                        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty() }
                }
                if (chosen == null) {
                    listener.unavailable(if (available == TextToSpeech.LANG_MISSING_DATA) SpeechProblem.MISSING_DATA else SpeechProblem.NO_SPANISH)
                    return
                }
            }
            if (chosen == null) { listener.unavailable(SpeechProblem.NO_SPANISH); return }
            engine.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            listener.ready(SpeechVoice(chosen.locale.toLanguageTag(), chosen.isNetworkConnectionRequired,
                TextToSpeech.getMaxSpeechInputLength()))
        } catch (_: Exception) { listener.unavailable(SpeechProblem.INITIALIZATION) }
    }

    override fun speak(text: String, rate: Float, id: String): CommunicationSpeechEngine.Request {
        val engine = tts ?: return CommunicationSpeechEngine.Request.FAILED
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) return CommunicationSpeechEngine.Request.MUTED
        if (closed || engine.setSpeechRate(rate) != TextToSpeech.SUCCESS) return CommunicationSpeechEngine.Request.FAILED
        return if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.SUCCESS)
            CommunicationSpeechEngine.Request.ACCEPTED else CommunicationSpeechEngine.Request.FAILED
    }
    override fun stop() { tts?.stop() }
    override fun shutdown() {
        closed = true
        handler.removeCallbacksAndMessages(null)
        val engine = tts
        tts = null
        try { engine?.stop() } finally { engine?.shutdown() }
    }
}
