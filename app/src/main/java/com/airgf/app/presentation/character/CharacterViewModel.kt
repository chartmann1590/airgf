package com.airgf.app.presentation.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgf.app.ads.ConsentManager
import com.airgf.app.animation.CharacterAnimationController
import com.airgf.app.data.repository.AvatarAssetRepository
import com.airgf.app.domain.model.EmotionState
import com.airgf.app.domain.model.GfProfile
import com.airgf.app.domain.model.VoiceOption
import com.airgf.app.domain.repository.ChatRepository
import com.airgf.app.domain.repository.GfConfigRepository
import com.airgf.app.domain.repository.SubscriptionRepository
import com.airgf.app.domain.repository.UserRepository
import com.airgf.app.domain.usecase.BuildSystemPromptUseCase
import com.airgf.app.domain.usecase.DetectEmotionUseCase
import com.airgf.app.llm.LlmEngine
import com.airgf.app.presentation.components.WaveformStyle
import com.airgf.app.safety.ContentSafetyPolicy
import com.airgf.app.tts.LipSyncBridge
import com.airgf.app.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CharacterUiState(
    val gfProfile: GfProfile? = null,
    val emotion: EmotionState = EmotionState.NEUTRAL,
    val mouthShape: LipSyncBridge.MouthShape = LipSyncBridge.MouthShape.CLOSED,
    val isSpeaking: Boolean = false,
    val speechBubbleText: String? = null,
    val waveformStyle: WaveformStyle = WaveformStyle.SOFT,
    val action: CharacterAnimationController.AvatarAction = CharacterAnimationController.AvatarAction.IDLE,
    val isSubscribed: Boolean = false,
)

@HiltViewModel
class CharacterViewModel @Inject constructor(
    private val gfConfigRepository: GfConfigRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val animationController: CharacterAnimationController,
    private val ttsManager: TtsManager,
    private val lipSyncBridge: LipSyncBridge,
    private val llmEngine: LlmEngine,
    private val buildSystemPromptUseCase: BuildSystemPromptUseCase,
    private val detectEmotionUseCase: DetectEmotionUseCase,
    private val contentSafetyPolicy: ContentSafetyPolicy,
    val avatarAssetRepository: AvatarAssetRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val consentManager: ConsentManager,
) : ViewModel() {

    val canRequestAds: Boolean get() = consentManager.canRequestAds

    private val _state = MutableStateFlow(CharacterUiState())
    val state: StateFlow<CharacterUiState> = _state.asStateFlow()

    private var talkJob: Job? = null
    private var bubbleDismissJob: Job? = null
    private var ambientActionJob: Job? = null
    private var visemeJob: Job? = null

    private val greetingPrompts = listOf(
        "Say a short sweet greeting to your partner.",
        "Give your partner a compliment.",
        "Tell your partner something flirty.",
        "Ask your partner how their day is going.",
        "Say something cute and playful.",
    )

    init {
        viewModelScope.launch {
            gfConfigRepository.getProfileFlow().collect { profile ->
                _state.update {
                    it.copy(
                        gfProfile = profile,
                        waveformStyle = profile?.voiceOption.toWaveformStyle(),
                    )
                }
                profile?.let {
                    ttsManager.setPresentation(it.presentation)
                    ttsManager.setAvatar(it.visualTemplate)
                    ttsManager.setVoice(it.voiceOption)
                }
            }
        }

        viewModelScope.launch {
            val latestGfEmotion = chatRepository.getRecentMessages(50)
                .filter { !it.isUser }
                .maxByOrNull { it.timestamp }
                ?.emotion
            if (latestGfEmotion != null) {
                animationController.setEmotion(latestGfEmotion)
            }
        }

        viewModelScope.launch {
            subscriptionRepository.isSubscribedFlow().collect { subscribed ->
                _state.update { it.copy(isSubscribed = subscribed) }
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
                _state.update { it.copy(action = action) }
            }
        }

        viewModelScope.launch {
            ttsManager.isSpeaking.collect { speaking ->
                _state.update { it.copy(isSpeaking = speaking) }
                if (!speaking) {
                    scheduleBubbleDismiss()
                }
            }
        }

        ttsManager.ensureInitialized()
        ambientActionJob = viewModelScope.launch {
            while (true) {
                delay(9000)
                if (!_state.value.isSpeaking) {
                    animationController.requestAction(CharacterAnimationController.AvatarAction.WALKING)
                    delay(6200)
                    animationController.requestAction(CharacterAnimationController.AvatarAction.WAVING)
                    delay(2200)
                    animationController.requestAction(CharacterAnimationController.AvatarAction.IDLE)
                }
            }
        }
    }

    fun tapToTalk() {
        if (_state.value.isSpeaking) {
            stopSpeaking()
            return
        }
        if (llmEngine.state.value !is LlmEngine.LlmState.Ready) {
            _state.update { it.copy(speechBubbleText = "AI model is still loading.") }
            return
        }

        talkJob?.cancel()
        talkJob = viewModelScope.launch {
            val prompt = greetingPrompts.random()
            try {
                val systemPrompt = buildSystemPromptUseCase()
                val session = llmEngine.createSession(systemPrompt, emptyList())
                val fullResponse = StringBuilder()
                try {
                    fullResponse.append(generateSafeResponse(session, prompt))
                } finally {
                    session.close()
                }

                val (cleanText, emotion) = detectEmotionUseCase(fullResponse.toString())
                animationController.setEmotion(emotion)
                speakText(cleanText)
            } catch (_: Exception) {
                _state.update { it.copy(speechBubbleText = "AI response unavailable.") }
            }
        }
    }

    fun sendQuickReaction(message: String) {
        if (_state.value.isSpeaking) stopSpeaking()

        talkJob?.cancel()
        talkJob = viewModelScope.launch {
            if (llmEngine.state.value !is LlmEngine.LlmState.Ready) {
                _state.update { it.copy(speechBubbleText = "AI model is still loading.") }
                return@launch
            }
            try {
                val systemPrompt = buildSystemPromptUseCase()
                val session = llmEngine.createSession(systemPrompt, emptyList())
                val fullResponse = StringBuilder()
                try {
                    fullResponse.append(
                        generateSafeResponse(
                            session,
                            "Your partner just said: \"$message\". Respond warmly and briefly.",
                        ),
                    )
                } finally {
                    session.close()
                }

                val (cleanText, emotion) = detectEmotionUseCase(fullResponse.toString())
                animationController.setEmotion(emotion)
                speakText(cleanText)
            } catch (_: Exception) {
                _state.update { it.copy(speechBubbleText = "AI response unavailable.") }
            }
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
        animationController.setMouthShape(LipSyncBridge.MouthShape.CLOSED)
        _state.update { it.copy(speechBubbleText = null) }
    }

    fun dance() {
        viewModelScope.launch {
            animationController.requestAction(CharacterAnimationController.AvatarAction.DANCING)
            delay(7000)
            if (!_state.value.isSpeaking) animationController.requestAction(CharacterAnimationController.AvatarAction.IDLE)
        }
    }

    private fun speakText(text: String) {
        bubbleDismissJob?.cancel()
        _state.update { it.copy(speechBubbleText = text) }
        ttsManager.speak(
            text = text,
            onWordStart = { word ->
                visemeJob?.cancel()
                visemeJob = viewModelScope.launch {
                    lipSyncBridge.animateWord(word, animationController::setMouthShape)
                }
            },
            onDone = {
                animationController.setMouthShape(LipSyncBridge.MouthShape.CLOSED)
            },
        )
    }

    private suspend fun generateSafeResponse(
        session: com.airgf.app.llm.LlmSession,
        prompt: String,
    ): String {
        val draft = buildString { session.sendMessage(prompt).collect { append(it) } }
        val decision = contentSafetyPolicy.classify(draft)
        if (decision.allowed) return draft

        val retry = buildString {
            session.sendMessage(
                "Regenerate that response without ${decision.category.name} content. " +
                    "Stay warm, brief, and do not mention this instruction.",
            ).collect { append(it) }
        }
        check(contentSafetyPolicy.classify(retry).allowed) { "Model produced unsafe output" }
        return retry
    }

    private fun scheduleBubbleDismiss() {
        bubbleDismissJob?.cancel()
        bubbleDismissJob = viewModelScope.launch {
            delay(3000)
            _state.update { it.copy(speechBubbleText = null) }
        }
    }

    override fun onCleared() {
        talkJob?.cancel()
        bubbleDismissJob?.cancel()
        ambientActionJob?.cancel()
        visemeJob?.cancel()
        ttsManager.stop()
        super.onCleared()
    }

    private fun VoiceOption?.toWaveformStyle(): WaveformStyle = when (this) {
        VoiceOption.SOFT -> WaveformStyle.SOFT
        VoiceOption.ENERGETIC -> WaveformStyle.ENERGETIC
        VoiceOption.MATURE -> WaveformStyle.MATURE
        VoiceOption.BREATHY -> WaveformStyle.BREATHY
        null -> WaveformStyle.SOFT
    }
}
