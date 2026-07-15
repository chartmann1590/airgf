package com.airgf.app.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import com.airgf.app.domain.model.CompanionPresentation
import com.airgf.app.domain.model.VisualTemplate
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
    private var currentVoiceOption: VoiceOption = VoiceOption.SOFT
    private var presentation: CompanionPresentation = CompanionPresentation.FEMININE
    private var preferredLocaleTags: List<String> = listOf(Locale.US.toLanguageTag())
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
        currentVoiceOption = voiceOption
        if (!_isReady.value) {
            pendingVoiceOption = voiceOption
            return
        }
        applyVoice(voiceOption)
    }

    fun setPresentation(value: CompanionPresentation) {
        presentation = value
        if (_isReady.value) applyVoice(currentVoiceOption)
    }

    fun setAvatar(template: VisualTemplate) {
        preferredLocaleTags = template.preferredVoiceLocaleTags
        if (_isReady.value) applyVoice(currentVoiceOption)
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
            ?.filter { voice -> !voice.isNetworkConnectionRequired }
            .orEmpty()

        val selected = selectVoice(voices, voiceOption)
            ?: voices.firstOrNull { it.locale.language == Locale.US.language }
            ?: voices.firstOrNull()
        selected?.let {
            engine.language = it.locale
            engine.voice = it
            Log.i(
                TAG,
                "Selected offline voice=${it.name} locale=${it.locale.toLanguageTag()} quality=${it.quality}",
            )
        }

        val presentationPitch = when (presentation) {
            CompanionPresentation.FEMININE -> 1.03f
            CompanionPresentation.MASCULINE -> 0.96f
            CompanionPresentation.NEUTRAL -> 1.0f
        }
        val stylePitch = when (voiceOption) {
            VoiceOption.SOFT -> 1.01f
            VoiceOption.ENERGETIC -> 1.04f
            VoiceOption.MATURE -> 0.98f
            VoiceOption.BREATHY -> 0.99f
        }
        engine.setPitch((presentationPitch * stylePitch).coerceIn(0.9f, 1.1f))
        engine.setSpeechRate(speechSpeed)
    }

    private fun selectVoice(voices: List<Voice>, option: VoiceOption): Voice? {
        if (voices.isEmpty()) return null

        return voices.maxByOrNull { voice -> scoreVoice(voice, option) }
    }

    private fun scoreVoice(voice: Voice, option: VoiceOption): Int {
        val name = buildString {
            append(voice.name.lowercase())
            append(' ')
            append(voice.features.joinToString(" ").lowercase())
        }
        var score = voice.quality * 2

        val localeIndex = preferredLocaleTags.indexOfFirst { preferred ->
            voice.locale.toLanguageTag().equals(preferred, ignoreCase = true)
        }
        score += when (localeIndex) {
            0 -> 1_000
            1 -> 700
            else -> if (voice.locale.language == "en") 250 else -500
        }
        score += (500 - voice.latency).coerceAtLeast(0)

        val soundsFeminine = name.contains("female") || name.contains("-f") || name.endsWith("f")
        val soundsMasculine = name.contains("male") || name.contains("-m") || name.endsWith("m")
        score += when (presentation) {
            CompanionPresentation.FEMININE -> if (soundsFeminine) 40 else if (soundsMasculine) -40 else 0
            CompanionPresentation.MASCULINE -> if (soundsMasculine) 40 else if (soundsFeminine) -40 else 0
            CompanionPresentation.NEUTRAL -> 0
        }

        when (option) {
            VoiceOption.SOFT -> {
                if (soundsFeminine) score += 20
                if (name.contains("soft") || name.contains("gentle")) score += 15
            }
            VoiceOption.ENERGETIC -> {
                if (name.contains("bright") || name.contains("b")) score += 15
            }
            VoiceOption.MATURE -> {
                if (name.contains("warm") || name.contains("d") || name.contains("m")) score += 15
                if (soundsMasculine) score += 10
            }
            VoiceOption.BREATHY -> {
                if (name.contains("soft") || name.contains("calm")) score += 15
            }
        }

        return score
    }

    companion object {
        private const val TAG = "AirGfTts"
        private const val PREVIEW_TEXT = "Hi, I'm so happy to meet you!"
        const val MIN_SPEECH_RATE = 0.7f
        const val MAX_SPEECH_RATE = 1.3f
    }
}
