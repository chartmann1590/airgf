package com.airgf.app.presentation.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgf.app.core.util.ImageStorageUtil
import com.airgf.app.domain.model.GfProfile
import com.airgf.app.domain.model.Message
import com.airgf.app.domain.model.UserProfile
import com.airgf.app.domain.repository.ChatRepository
import com.airgf.app.domain.repository.GfConfigRepository
import com.airgf.app.domain.repository.ModelRepository
import com.airgf.app.domain.repository.UserRepository
import com.airgf.app.animation.CharacterAnimationController
import com.airgf.app.domain.model.EmotionState
import com.airgf.app.domain.usecase.BuildSystemPromptUseCase
import com.airgf.app.domain.usecase.SendMessageEvent
import com.airgf.app.domain.usecase.SendMessageUseCase
import com.airgf.app.imagegen.ImageGenerator
import com.airgf.app.llm.LlmEngine
import com.airgf.app.llm.LlmSession
import com.airgf.app.tts.LipSyncBridge
import com.airgf.app.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val streamingText: String = "",
    val isGenerating: Boolean = false,
    val isGeneratingImage: Boolean = false,
    val inputText: String = "",
    val gfProfile: GfProfile? = null,
    val userProfile: UserProfile? = null,
    val llmState: LlmEngine.LlmState = LlmEngine.LlmState.Uninitialized,
    val isInitializing: Boolean = false,
    val error: String? = null,
    val ttsEnabled: Boolean = true,
    val isSpeaking: Boolean = false,
    val pendingImageUri: Uri? = null,
    val pendingImageBitmap: Bitmap? = null,
    val pendingImagePath: String? = null,
    val showAttachmentOptions: Boolean = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
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
    private val imageStorageUtil: ImageStorageUtil,
    private val imageGenerator: ImageGenerator,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var conversationId: Long = 0
    private var session: LlmSession? = null
    private var sendJob: Job? = null
    private var hasObservedProfileState = false

    init {
        viewModelScope.launch {
            _state.update { it.copy(isInitializing = true) }
            try {
                conversationId = chatRepository.getOrCreateConversation()

                launch {
                    chatRepository.getMessagesFlow(conversationId).collect { messages ->
                        _state.update { current -> current.copy(messages = messages) }
                    }
                }

                launch {
                    combine(
                        gfConfigRepository.getProfileFlow(),
                        userRepository.getProfileFlow(),
                    ) { gfProfile, userProfile ->
                        gfProfile to userProfile
                    }.collect { (gfProfile, userProfile) ->
                        val previous = _state.value
                        _state.update {
                            it.copy(
                                gfProfile = gfProfile,
                                userProfile = userProfile,
                            )
                        }
                        gfProfile?.voiceOption?.let(ttsManager::setVoice)
                        val shouldRefreshSession = hasObservedProfileState &&
                            (previous.gfProfile != gfProfile || previous.userProfile != userProfile)
                        hasObservedProfileState = true
                        if (shouldRefreshSession) {
                            recreateSession()
                        }
                    }
                }

                launch {
                    llmEngine.state.collect { llmState ->
                        _state.update { it.copy(llmState = llmState) }
                    }
                }

                launch {
                    userRepository.ttsEnabledFlow().collect { enabled ->
                        _state.update { it.copy(ttsEnabled = enabled) }
                    }
                }

                launch {
                    userRepository.speechSpeedFlow().collect { speed ->
                        ttsManager.setSpeechSpeed(speed)
                    }
                }

                launch {
                    ttsManager.isSpeaking.collect { speaking ->
                        _state.update { it.copy(isSpeaking = speaking) }
                    }
                }

                ttsManager.ensureInitialized()
                initializeLlm()
                launch { imageGenerator.ensureInitialized() }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Failed to load chat",
                        isInitializing = false,
                    )
                }
            }
        }
    }

    fun onInputChange(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            val saved = imageStorageUtil.savePickedImageFromUri(uri) ?: return@launch
            val bitmap = imageStorageUtil.loadImageBitmap(saved.path)
            _state.update {
                it.copy(
                    pendingImageUri = uri,
                    pendingImageBitmap = bitmap,
                    pendingImagePath = saved.path,
                    showAttachmentOptions = false,
                )
            }
        }
    }

    fun onCameraCaptured(uri: Uri) {
        viewModelScope.launch {
            val saved = imageStorageUtil.savePickedImageFromUri(uri) ?: return@launch
            val bitmap = imageStorageUtil.loadImageBitmap(saved.path)
            _state.update {
                it.copy(
                    pendingImageUri = uri,
                    pendingImageBitmap = bitmap,
                    pendingImagePath = saved.path,
                    showAttachmentOptions = false,
                )
            }
        }
    }

    fun clearPendingImage() {
        _state.update {
            it.copy(pendingImageUri = null, pendingImageBitmap = null, pendingImagePath = null)
        }
    }

    fun toggleAttachmentOptions() {
        _state.update { it.copy(showAttachmentOptions = !it.showAttachmentOptions) }
    }

    fun dismissAttachmentOptions() {
        _state.update { it.copy(showAttachmentOptions = false) }
    }

    fun sendMessage() {
        val current = _state.value
        val text = current.inputText.trim()
        if ((text.isEmpty() && current.pendingImageUri == null) || current.isGenerating) return
        if (current.llmState !is LlmEngine.LlmState.Ready) {
            _state.update { it.copy(error = "AI model is not ready yet") }
            return
        }

        val pendingBitmap = current.pendingImageBitmap
        val pendingPath = current.pendingImagePath

        _state.update {
            it.copy(
                inputText = "",
                isGenerating = true,
                streamingText = "",
                error = null,
                pendingImageUri = null,
                pendingImageBitmap = null,
                pendingImagePath = null,
            )
        }

        ttsManager.stop()

        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            if (session == null) {
                recreateSession()
            }
            val activeSession = session ?: run {
                _state.update { it.copy(error = "Chat session not ready", isGenerating = false) }
                return@launch
            }
            val messageText = if (text.isEmpty() && pendingPath != null) "Look at this" else text
            sendMessageUseCase(
                session = activeSession,
                conversationId = conversationId,
                text = messageText,
                imagePath = pendingPath,
                imageBitmap = pendingBitmap,
            ).collect { event ->
                when (event) {
                    is SendMessageEvent.Streaming -> {
                        _state.update { it.copy(streamingText = event.text) }
                    }
                    is SendMessageEvent.GeneratingImage -> {
                        _state.update { it.copy(isGeneratingImage = true, streamingText = event.textSoFar) }
                    }
                    is SendMessageEvent.Complete -> {
                        animationController.setEmotion(
                            event.message.emotion ?: EmotionState.NEUTRAL,
                        )
                        _state.update {
                            it.copy(isGenerating = false, isGeneratingImage = false, streamingText = "")
                        }
                        if (_state.value.ttsEnabled) {
                            speakMessage(event.message.content)
                        }
                    }
                    is SendMessageEvent.Error -> {
                        _state.update {
                            it.copy(
                                isGenerating = false,
                                isGeneratingImage = false,
                                streamingText = "",
                                error = event.message,
                            )
                        }
                    }
                }
            }
        }
    }

    fun requestImage() {
        val current = _state.value
        if (current.isGenerating) return
        onInputChange("Send me a picture of yourself")
        sendMessage()
    }

    fun toggleSpicyMode() {
        val gf = _state.value.gfProfile ?: return
        viewModelScope.launch {
            val updated = gf.copy(spicyModeEnabled = !gf.spicyModeEnabled)
            gfConfigRepository.saveProfile(updated)
            _state.update { it.copy(gfProfile = updated) }
            recreateSession()
        }
    }

    fun toggleTtsEnabled() {
        viewModelScope.launch {
            val enabled = !_state.value.ttsEnabled
            userRepository.setTtsEnabled(enabled)
            if (!enabled) {
                ttsManager.stop()
                animationController.setMouthShape(LipSyncBridge.MouthShape.CLOSED)
            }
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
        animationController.setMouthShape(LipSyncBridge.MouthShape.CLOSED)
    }

    fun retryLlmInit() {
        viewModelScope.launch { initializeLlm(force = true) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun initializeLlm(force: Boolean = false) {
        val llmState = llmEngine.state.value
        if (!force && (llmState is LlmEngine.LlmState.Ready || llmState is LlmEngine.LlmState.Loading)) {
            if (llmState is LlmEngine.LlmState.Ready && session == null) {
                recreateSession()
            }
            _state.update { it.copy(isInitializing = false) }
            return
        }

        val modelPath = modelRepository.getModelPath()
        if (modelPath == null) {
            _state.update {
                it.copy(
                    isInitializing = false,
                    error = "Model not downloaded. Please complete model download in setup.",
                )
            }
            return
        }

        _state.update { it.copy(isInitializing = true, error = null) }
        llmEngine.initialize(modelPath)
        _state.update { it.copy(isInitializing = false) }

        if (llmEngine.state.value is LlmEngine.LlmState.Ready) {
            recreateSession()
        }
    }

    private suspend fun recreateSession() {
        sendJob?.cancel()
        runCatching { session?.close() }
        session = null

        if (llmEngine.state.value !is LlmEngine.LlmState.Ready) return

        try {
            val systemPrompt = buildSystemPromptUseCase()
            val history = chatRepository.getRecentMessages(HISTORY_LIMIT)
                .sortedBy { it.timestamp }
            session = llmEngine.createSession(systemPrompt, history)
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message ?: "Failed to create chat session") }
        }
    }

    override fun onCleared() {
        sendJob?.cancel()
        ttsManager.stop()
        runCatching { session?.close() }
        session = null
        super.onCleared()
    }

    private fun speakMessage(text: String) {
        ttsManager.speak(
            text = text,
            onWordStart = { word ->
                animationController.setMouthShape(lipSyncBridge.getVisemeForWord(word))
            },
            onDone = {
                animationController.setMouthShape(LipSyncBridge.MouthShape.CLOSED)
            },
        )
    }

    companion object {
        private const val HISTORY_LIMIT = 20
    }
}
