package com.airgf.app.presentation.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgf.app.animation.CharacterAnimationController
import com.airgf.app.data.repository.AvatarAssetRepository
import com.airgf.app.domain.model.EmotionState
import com.airgf.app.domain.model.GfProfile
import com.airgf.app.domain.model.UserProfile
import com.airgf.app.domain.model.VoiceOption
import com.airgf.app.domain.repository.ChatRepository
import com.airgf.app.domain.repository.GfConfigRepository
import com.airgf.app.domain.repository.ModelRepository
import com.airgf.app.domain.repository.UserRepository
import com.airgf.app.domain.usecase.BuildSystemPromptUseCase
import com.airgf.app.domain.usecase.SendMessageEvent
import com.airgf.app.domain.usecase.SendMessageUseCase
import com.airgf.app.domain.usecase.RetrieveMemoriesUseCase
import com.airgf.app.llm.LlmEngine
import com.airgf.app.llm.LlmSession
import com.airgf.app.presentation.components.WaveformStyle
import com.airgf.app.safety.ContentSafetyPolicy
import com.airgf.app.speech.SpeechRecognitionError
import com.airgf.app.speech.SpeechRecognizerManager
import com.airgf.app.tts.LipSyncBridge
import com.airgf.app.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CallPhase {
    Idle,
    Starting,
    Listening,
    Processing,
    Speaking,
    Muted,
    Ended,
    Error,
}

data class CallUiState(
    val gfProfile: GfProfile? = null,
    val userProfile: UserProfile? = null,
    val phase: CallPhase = CallPhase.Idle,
    val callActive: Boolean = false,
    val micMuted: Boolean = false,
    val speakerEnabled: Boolean = true,
    val elapsedSeconds: Long = 0,
    val lastUserTranscript: String? = null,
    val gfSpeechText: String? = null,
    val error: String? = null,
    val permissionDenied: Boolean = false,
    val emotion: EmotionState = EmotionState.NEUTRAL,
    val mouthShape: LipSyncBridge.MouthShape = LipSyncBridge.MouthShape.CLOSED,
    val waveformStyle: WaveformStyle = WaveformStyle.SOFT,
    val llmState: LlmEngine.LlmState = LlmEngine.LlmState.Uninitialized,
    val avatarAction: CharacterAnimationController.AvatarAction = CharacterAnimationController.AvatarAction.IDLE,
)

@HiltViewModel
class CallViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val gfConfigRepository: GfConfigRepository,
    private val userRepository: UserRepository,
    private val modelRepository: ModelRepository,
    private val llmEngine: LlmEngine,
    private val buildSystemPromptUseCase: BuildSystemPromptUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val ttsManager: TtsManager,
    private val lipSyncBridge: LipSyncBridge,
    private val animationController: CharacterAnimationController,
    private val speechRecognizerManager: SpeechRecognizerManager,
    val avatarAssetRepository: AvatarAssetRepository,
    private val retrieveMemoriesUseCase: RetrieveMemoriesUseCase,
    private val contentSafetyPolicy: ContentSafetyPolicy,
) : ViewModel() {

    private val _state = MutableStateFlow(CallUiState())
    val state: StateFlow<CallUiState> = _state.asStateFlow()

    private var conversationId: Long = 0
    private var session: LlmSession? = null
    private var callTimerJob: Job? = null
    private var generationJob: Job? = null
    private var listenRetryJob: Job? = null
    private var visemeJob: Job? = null

    init {
        viewModelScope.launch {
            conversationId = chatRepository.getOrCreateConversation()
        }
        viewModelScope.launch {
            combine(
                gfConfigRepository.getProfileFlow(),
                userRepository.getProfileFlow(),
            ) { gfProfile, userProfile -> gfProfile to userProfile }
                .collect { (gfProfile, userProfile) ->
                    _state.update {
                        it.copy(
                            gfProfile = gfProfile,
                            userProfile = userProfile,
                            waveformStyle = gfProfile?.voiceOption.toWaveformStyle(),
                        )
                    }
                    gfProfile?.let { profile ->
                        ttsManager.setPresentation(profile.presentation)
                        ttsManager.setAvatar(profile.visualTemplate)
                        ttsManager.setVoice(profile.voiceOption)
                    }
                }
        }
        viewModelScope.launch {
            userRepository.ttsEnabledFlow().collect { enabled ->
                _state.update { it.copy(speakerEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userRepository.speechSpeedFlow().collect(ttsManager::setSpeechSpeed)
        }
        viewModelScope.launch {
            llmEngine.state.collect { llmState ->
                _state.update { it.copy(llmState = llmState) }
            }
        }
        viewModelScope.launch {
            animationController.currentEmotion.collect { emotion ->
                _state.update { it.copy(emotion = emotion) }
            }
        }
        viewModelScope.launch {
            animationController.currentMouthShape.collect { shape ->
                _state.update { it.copy(mouthShape = shape) }
            }
        }
        viewModelScope.launch {
            animationController.currentAction.collect { action ->
                _state.update { it.copy(avatarAction = action) }
            }
        }
        ttsManager.ensureInitialized()
    }

    fun startCall(hasAudioPermission: Boolean) {
        if (!hasAudioPermission) {
            _state.update {
                it.copy(
                    permissionDenied = true,
                    error = "Microphone permission is required for calls.",
                    phase = CallPhase.Error,
                )
            }
            return
        }
        if (_state.value.callActive) return

        _state.update {
            it.copy(
                callActive = true,
                permissionDenied = false,
                phase = CallPhase.Starting,
                elapsedSeconds = 0,
                error = null,
            )
        }
        startTimer()

        viewModelScope.launch {
            if (conversationId == 0L) {
                conversationId = chatRepository.getOrCreateConversation()
            }
            if (!ensureLlmReady()) return@launch
            recreateSession()
            generateIntroThenListen()
        }
    }

    fun onPermissionDenied() {
        _state.update {
            it.copy(
                permissionDenied = true,
                phase = CallPhase.Error,
                error = "Microphone permission is required for calls.",
            )
        }
    }

    fun retryListening() {
        if (!_state.value.callActive || _state.value.micMuted) return
        startListening()
    }

    fun toggleMute() {
        val muted = !_state.value.micMuted
        _state.update {
            it.copy(
                micMuted = muted,
                phase = if (muted) CallPhase.Muted else CallPhase.Listening,
                error = null,
            )
        }
        if (muted) {
            speechRecognizerManager.cancelListening()
        } else if (_state.value.callActive && _state.value.gfSpeechText == null) {
            startListening()
        }
    }

    fun toggleSpeaker() {
        val enabled = !_state.value.speakerEnabled
        viewModelScope.launch {
            userRepository.setTtsEnabled(enabled)
            if (!enabled) {
                ttsManager.stop()
                animationController.setMouthShape(LipSyncBridge.MouthShape.CLOSED)
                if (_state.value.callActive && !_state.value.micMuted) startListening()
            }
        }
    }

    fun interruptReply() {
        ttsManager.stop()
        animationController.setMouthShape(LipSyncBridge.MouthShape.CLOSED)
        _state.update { it.copy(gfSpeechText = null) }
        if (_state.value.callActive && !_state.value.micMuted) startListening()
    }

    fun endCall() {
        _state.update { it.copy(callActive = false, phase = CallPhase.Ended) }
        listenRetryJob?.cancel()
        generationJob?.cancel()
        callTimerJob?.cancel()
        speechRecognizerManager.cancelListening()
        ttsManager.stop()
        animationController.setMouthShape(LipSyncBridge.MouthShape.CLOSED)
        runCatching { session?.close() }
        session = null
    }

    private suspend fun ensureLlmReady(): Boolean {
        val current = llmEngine.state.value
        if (current is LlmEngine.LlmState.Ready) return true

        val modelPath = modelRepository.getModelPath()
        if (modelPath == null) {
            _state.update {
                it.copy(
                    phase = CallPhase.Error,
                    error = "Model not downloaded. Please complete model download in setup.",
                )
            }
            return false
        }

        llmEngine.initialize(modelPath)
        val ready = llmEngine.state.value is LlmEngine.LlmState.Ready
        if (!ready) {
            _state.update {
                it.copy(
                    phase = CallPhase.Error,
                    error = "AI model is not ready yet.",
                )
            }
        }
        return ready
    }

    private suspend fun recreateSession() {
        runCatching { session?.close() }
        val memories = retrieveMemoriesUseCase("")
        val systemPrompt = buildSystemPromptUseCase(memories)
        val history = chatRepository.getRecentMessages(HISTORY_LIMIT).sortedBy { it.timestamp }
        session = llmEngine.createSession(systemPrompt, history)
    }

    private suspend fun generateIntroThenListen() {
        val activeSession = session ?: return
        val draft = buildString {
            activeSession.sendMessage(
                "Open this voice call with a fresh, natural one-sentence greeting. " +
                    "Do not reuse a stock greeting and do not mention this instruction.",
            ).collect { append(it) }
        }
        if (contentSafetyPolicy.classify(draft).allowed) {
            speakText(draft, listenAfter = true)
        } else {
            startListening()
        }
    }

    private fun startListening() {
        if (!_state.value.callActive || _state.value.micMuted) return
        listenRetryJob?.cancel()
        ttsManager.stop()
        animationController.setMouthShape(LipSyncBridge.MouthShape.CLOSED)
        _state.update { it.copy(phase = CallPhase.Listening, error = null, gfSpeechText = null) }
        speechRecognizerManager.startListening(
            onReady = {
                _state.update { state ->
                    if (state.callActive && !state.micMuted) state.copy(phase = CallPhase.Listening) else state
                }
            },
            onBeginning = {
                _state.update { state ->
                    if (state.callActive) state.copy(phase = CallPhase.Listening) else state
                }
            },
            onEnd = {
                _state.update { state ->
                    if (state.callActive && !state.micMuted) state.copy(phase = CallPhase.Processing) else state
                }
            },
            onResult = { text ->
                viewModelScope.launch { handleUserSpeech(text) }
            },
            onError = { error ->
                handleRecognitionError(error)
            },
        )
    }

    private suspend fun handleUserSpeech(text: String) {
        if (!_state.value.callActive) return
        _state.update {
            it.copy(
                lastUserTranscript = text,
                phase = CallPhase.Processing,
                error = null,
            )
        }

        val activeSession = session ?: run {
            recreateSession()
            session
        } ?: run {
            _state.update { it.copy(phase = CallPhase.Error, error = "Chat session not ready.") }
            return
        }

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            var latestText = ""
            sendMessageUseCase(activeSession, conversationId, text).collect { event ->
                when (event) {
                    is SendMessageEvent.Streaming -> latestText = event.text
                    is SendMessageEvent.GeneratingImage -> latestText = event.textSoFar
                    is SendMessageEvent.Complete -> {
                        animationController.setEmotion(event.message.emotion ?: EmotionState.NEUTRAL)
                        speakText(event.message.content, listenAfter = true)
                    }
                    is SendMessageEvent.Error -> {
                        _state.update {
                            it.copy(
                                phase = CallPhase.Error,
                                error = event.message,
                                gfSpeechText = latestText.ifBlank { null },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleRecognitionError(error: SpeechRecognitionError) {
        if (!_state.value.callActive) return
        _state.update {
            it.copy(
                phase = if (error.recoverable) CallPhase.Listening else CallPhase.Error,
                error = error.message,
            )
        }
        if (error.recoverable && !_state.value.micMuted) {
            listenRetryJob?.cancel()
            listenRetryJob = viewModelScope.launch {
                delay(900)
                startListening()
            }
        }
    }

    private fun speakText(text: String, listenAfter: Boolean) {
        val clean = text.trim()
        if (clean.isEmpty()) {
            if (listenAfter) startListening()
            return
        }
        _state.update {
            it.copy(
                phase = if (it.speakerEnabled) CallPhase.Speaking else CallPhase.Listening,
                gfSpeechText = clean,
                error = null,
            )
        }
        if (!_state.value.speakerEnabled) {
            if (listenAfter) startListening()
            return
        }
        speechRecognizerManager.cancelListening()
        ttsManager.speak(
            text = clean,
            onWordStart = { word ->
                visemeJob?.cancel()
                visemeJob = viewModelScope.launch {
                    lipSyncBridge.animateWord(word, animationController::setMouthShape)
                }
            },
            onDone = {
                viewModelScope.launch {
                    animationController.setMouthShape(LipSyncBridge.MouthShape.CLOSED)
                    _state.update { it.copy(gfSpeechText = null) }
                    if (listenAfter && _state.value.callActive && !_state.value.micMuted) {
                        startListening()
                    }
                }
            },
        )
    }

    private fun startTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (_state.value.callActive) {
                delay(1000)
                _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    override fun onCleared() {
        endCall()
        visemeJob?.cancel()
        super.onCleared()
    }

    private fun VoiceOption?.toWaveformStyle(): WaveformStyle = when (this) {
        VoiceOption.SOFT -> WaveformStyle.SOFT
        VoiceOption.ENERGETIC -> WaveformStyle.ENERGETIC
        VoiceOption.MATURE -> WaveformStyle.MATURE
        VoiceOption.BREATHY -> WaveformStyle.BREATHY
        null -> WaveformStyle.SOFT
    }

    companion object {
        private const val HISTORY_LIMIT = 20
    }
}
