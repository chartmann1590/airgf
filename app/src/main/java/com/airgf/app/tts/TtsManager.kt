package com.airgf.app.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.airgf.app.domain.model.VoiceOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var tts: TextToSpeech? = null
    private var initStarted = false
    private var pendingVoiceOption: VoiceOption? = null
    private var speechSpeed = 1.0f

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    fun ensureInitialized() {
        if (initStarted) return
        initStarted = true

        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                _isReady.value = false
                return@TextToSpeech
            }
            tts?.language = Locale.US
            _isReady.value = true
            pendingVoiceOption?.let { option ->
                applyVoice(option)
                pendingVoiceOption = null
            }
        }
    }

    fun setVoice(voiceOption: VoiceOption) {
        if (!_isReady.value) {
            pendingVoiceOption = voiceOption
            return
        }
        applyVoice(voiceOption)
    }

    fun setSpeechSpeed(speed: Float) {
        speechSpeed = speed.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
        tts?.setSpeechRate(speechSpeed)
    }

    fun speak(
        text: String,
        onWordStart: (word: String) -> Unit,
        onDone: () -> Unit = {},
    ) {
        ensureInitialized()
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val engine = tts ?: return
        val utteranceId = UUID.randomUUID().toString()
        val listener = TtsTimingListener(
            text = trimmed,
            utteranceId = utteranceId,
            onStart = { _isSpeaking.value = true },
            onWordStart = onWordStart,
            onDone = {
                _isSpeaking.value = false
                onDone()
            },
            onError = {
                _isSpeaking.value = false
                onDone()
            },
        )
        engine.setOnUtteranceProgressListener(listener)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        engine.speak(trimmed, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun previewVoice(voiceOption: VoiceOption, sampleText: String = PREVIEW_TEXT) {
        setVoice(voiceOption)
        speak(text = sampleText, onWordStart = {})
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        initStarted = false
        _isReady.value = false
        _isSpeaking.value = false
        pendingVoiceOption = null
    }

    private fun applyVoice(voiceOption: VoiceOption) {
        val engine = tts ?: return
        val voices = engine.voices
            ?.filter { voice ->
                voice.locale.language == "en" && !voice.isNetworkConnectionRequired
            }
            .orEmpty()

        val selected = selectVoice(voices, voiceOption) ?: voices.firstOrNull()
        selected?.let { engine.voice = it }

        when (voiceOption) {
            VoiceOption.SOFT -> engine.setPitch(1.05f)
            VoiceOption.ENERGETIC -> engine.setPitch(1.15f)
            VoiceOption.MATURE -> engine.setPitch(0.95f)
            VoiceOption.BREATHY -> engine.setPitch(0.85f)
        }
        engine.setSpeechRate(speechSpeed)
    }

    private fun selectVoice(voices: List<Voice>, option: VoiceOption): Voice? {
        if (voices.isEmpty()) return null

        return voices.maxByOrNull { voice -> scoreVoice(voice, option) }
    }

    private fun scoreVoice(voice: Voice, option: VoiceOption): Int {
        val name = voice.name.lowercase()
        var score = voice.quality

        when (option) {
            VoiceOption.SOFT -> {
                if (name.contains("female") || name.contains("-f") || name.endsWith("f")) score += 20
                if (name.contains("soft") || name.contains("gentle")) score += 15
            }
            VoiceOption.ENERGETIC -> {
                if (name.contains("bright") || name.contains("b")) score += 15
                score += voice.latency
            }
            VoiceOption.MATURE -> {
                if (name.contains("warm") || name.contains("d") || name.contains("m")) score += 15
                if (name.contains("male") || name.contains("-m") || name.endsWith("m")) score += 10
            }
            VoiceOption.BREATHY -> {
                score += (500 - voice.latency).coerceAtLeast(0)
            }
        }

        return score
    }

    companion object {
        private const val PREVIEW_TEXT = "Hi, I'm so happy to meet you!"
        const val MIN_SPEECH_RATE = 0.7f
        const val MAX_SPEECH_RATE = 1.3f
    }
}
