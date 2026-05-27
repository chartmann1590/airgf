package com.airgf.app.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.airgf.app.domain.model.Message
import com.airgf.app.domain.repository.ChatRepository
import com.airgf.app.domain.repository.GfConfigRepository
import com.airgf.app.domain.repository.ModelRepository
import com.airgf.app.domain.repository.UserRepository
import com.airgf.app.domain.usecase.BuildSystemPromptUseCase
import com.airgf.app.domain.usecase.DetectEmotionUseCase
import com.airgf.app.llm.LlmEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

@HiltWorker
class ProactiveMessageWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val chatRepository: ChatRepository,
    private val gfConfigRepository: GfConfigRepository,
    private val userRepository: UserRepository,
    private val modelRepository: ModelRepository,
    private val buildSystemPromptUseCase: BuildSystemPromptUseCase,
    private val detectEmotionUseCase: DetectEmotionUseCase,
    private val llmEngine: LlmEngine,
    private val notificationManager: GfNotificationManager,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!userRepository.isOnboardingComplete() || !userRepository.areProactiveMessagesEnabled()) {
                return Result.success()
            }

            val gfProfile = gfConfigRepository.getProfile() ?: return Result.success()
            if (userRepository.getProfile() == null) return Result.success()

            val modelPath = modelRepository.getModelPath() ?: return Result.success()
            if (llmEngine.state.value !is LlmEngine.LlmState.Ready) {
                llmEngine.initialize(modelPath)
            }
            if (llmEngine.state.value !is LlmEngine.LlmState.Ready) {
                return Result.success()
            }

            val systemPrompt = buildString {
                append(buildSystemPromptUseCase())
                appendLine()
                appendLine()
                append("CONTEXT: ${timeContextPrompt()} Generate a short proactive message in 1-2 sentences.")
            }

            val responseBuilder = StringBuilder()
            val session = llmEngine.createSession(systemPrompt, emptyList())
            try {
                session.sendMessage("Generate a proactive message.").collect { chunk ->
                    responseBuilder.append(chunk)
                }
            } finally {
                session.close()
            }

            val rawResponse = responseBuilder.toString().trim()
            if (rawResponse.isBlank()) return Result.success()

            val (cleanText, emotion) = detectEmotionUseCase(rawResponse)
            if (cleanText.isBlank()) return Result.success()

            val conversationId = chatRepository.getOrCreateConversation()
            val proactiveMessage = Message(
                conversationId = conversationId,
                content = cleanText,
                isUser = false,
                timestamp = System.currentTimeMillis(),
                emotion = emotion,
                isSpicyMode = gfProfile.spicyModeEnabled,
            )

            chatRepository.insertMessage(proactiveMessage)
            notificationManager.showGfMessage(gfProfile.name, cleanText)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun timeContextPrompt(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..9 -> "It's morning. Send a sweet good morning message."
            in 12..13 -> "It's lunchtime. Check in on your partner."
            in 17..19 -> "It's evening. Ask about their day."
            in 21..23 -> "It's nighttime. Send a cozy good night message."
            else -> "You're thinking about your partner. Send a sweet random message."
        }
    }
}
