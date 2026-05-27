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

@Singleton
class LlmEngine @Inject constructor(
    @ApplicationContext private val context: Context,
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
                        backend = Backend.GPU(),
                        cacheDir = context.cacheDir.path,
                    ),
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        cacheDir = context.cacheDir.path,
                    ),
                )

                var lastError: Exception? = null
                for (config in backendConfigs) {
                    try {
                        val newEngine = Engine(config)
                        newEngine.initialize()
                        engine = newEngine
                        _state.value = LlmState.Ready
                        return@withContext
                    } catch (e: Exception) {
                        lastError = e
                    }
                }

                _state.value = LlmState.Error(
                    lastError?.message ?: "Failed to load model",
                )
            } catch (e: Exception) {
                _state.value = LlmState.Error(e.message ?: "Failed to load model")
            }
        }
    }

    private var activeSession: LlmSession? = null

    fun createSession(systemPrompt: String, history: List<DomainMessage>): LlmSession {
        val activeEngine = engine ?: throw IllegalStateException("Engine not initialized")
        if (_state.value !is LlmState.Ready) {
            throw IllegalStateException("Engine is not ready")
        }

        activeSession?.close()
        activeSession = null

        val initialMessages = history.map { message ->
            val content = if (message.imagePath != null && message.imageDescription != null) {
                "(shared an image: ${message.imageDescription})"
            } else if (message.imagePath != null) {
                "(shared an image)"
            } else {
                message.content
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
        val session = LlmSession(conversation)
        activeSession = session
        return session
    }

    fun release() {
        releaseInternal()
        _state.value = LlmState.Uninitialized
    }

    private fun releaseInternal() {
        activeSession?.close()
        activeSession = null
        engine?.close()
        engine = null
    }
}
