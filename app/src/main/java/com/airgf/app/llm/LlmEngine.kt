package com.airgf.app.llm

import android.content.Context
import com.airgf.app.domain.model.Message as DomainMessage
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Singleton
class LlmEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageDescriptionService: ImageDescriptionService,
) {
    private var engine: Engine? = null

    private val _state = MutableStateFlow<LlmState>(LlmState.Uninitialized)
    val state: StateFlow<LlmState> = _state.asStateFlow()

    sealed class LlmState {
        data object Uninitialized : LlmState()
        data object Loading : LlmState()
        data object Ready : LlmState()
        data class Error(val message: String) : LlmState()
    }

    suspend fun initialize(modelPath: String) {
        if (_state.value is LlmState.Ready && engine != null) return

        _state.value = LlmState.Loading
        withContext(Dispatchers.IO) {
            try {
                releaseInternal()

                val backendConfigs = listOf(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        cacheDir = context.cacheDir.path,
                    ),
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.GPU(),
                        cacheDir = context.cacheDir.path,
                    ),
                )

                var lastError: Throwable? = null
                for (config in backendConfigs) {
                    try {
                        val newEngine = Engine(config)
                        newEngine.initialize()
                        engine = newEngine
                        _state.value = LlmState.Ready
                        return@withContext
                    } catch (e: Throwable) {
                        lastError = e
                    }
                }

                _state.value = LlmState.Error(
                    lastError?.message ?: "Failed to load model",
                )
            } catch (e: Throwable) {
                _state.value = LlmState.Error(e.message ?: "Failed to load model")
            }
        }
    }

    private val activeSessions = ConcurrentHashMap<String, LlmSession>()

    fun createSession(systemPrompt: String, history: List<DomainMessage>): LlmSession {
        val activeEngine = engine ?: throw IllegalStateException("Engine not initialized")
        if (_state.value !is LlmState.Ready) {
            throw IllegalStateException("Engine is not ready")
        }

        val initialMessages = history.map { message ->
            val content = when {
                message.imagePath != null && message.isUser -> {
                    val desc = message.imageDescription ?: "a photo"
                    val text = message.content.ifBlank { "" }
                    "[shared an image: $desc]${if (text.isNotBlank()) "\n$text" else ""}"
                }
                message.imagePath != null && !message.isUser && message.imageDescription != null -> {
                    "${message.content}\n(sent an image: ${message.imageDescription})"
                }
                else -> message.content
            }
            if (message.isUser) {
                Message.user(content)
            } else {
                Message.model(content)
            }
        }

        val conversation = activeEngine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(systemPrompt),
                initialMessages = initialMessages,
                samplerConfig = SamplerConfig(
                    topK = 40,
                    topP = 0.95,
                    temperature = 0.85,
                ),
            ),
        )
        val sessionId = UUID.randomUUID().toString()
        val session = LlmSession(conversation, imageDescriptionService) { activeSessions.remove(sessionId) }
        activeSessions[sessionId] = session
        return session
    }

    fun release() {
        releaseInternal()
        _state.value = LlmState.Uninitialized
    }

    private fun releaseInternal() {
        activeSessions.values.toList().forEach { it.close() }
        activeSessions.clear()
        engine?.close()
        engine = null
    }
}
