package com.airgf.app.domain.usecase

import android.graphics.Bitmap
import com.airgf.app.core.util.ImageStorageUtil
import com.airgf.app.domain.model.Message
import com.airgf.app.domain.repository.ChatRepository
import com.airgf.app.domain.repository.GfConfigRepository
import com.airgf.app.imagegen.ImageGenerator
import com.airgf.app.domain.repository.MemoryRepository
import com.airgf.app.safety.ContentSafetyPolicy
import com.airgf.app.llm.LlmSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

sealed class SendMessageEvent {
    data class Streaming(val text: String) : SendMessageEvent()
    data class GeneratingImage(val prompt: String, val textSoFar: String) : SendMessageEvent()
    data class Complete(val message: Message) : SendMessageEvent()
    data class Error(val message: String) : SendMessageEvent()
}

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val gfConfigRepository: GfConfigRepository,
    private val detectEmotionUseCase: DetectEmotionUseCase,
    private val detectImageRequestUseCase: DetectImageRequestUseCase,
    private val imageStorageUtil: ImageStorageUtil,
    private val imageGenerator: ImageGenerator,
    private val memoryRepository: MemoryRepository,
    private val extractMemoryCandidatesUseCase: ExtractMemoryCandidatesUseCase,
    private val responseSimilarityUseCase: ResponseSimilarityUseCase,
    private val contentSafetyPolicy: ContentSafetyPolicy,
) {
    operator fun invoke(
        session: LlmSession,
        conversationId: Long,
        text: String,
        imagePath: String? = null,
        imageBitmap: Bitmap? = null,
    ): Flow<SendMessageEvent> = flow {
        val trimmed = text.trim()
        if (trimmed.isEmpty() && imagePath == null) return@flow

        val gfProfile = gfConfigRepository.getProfile()
        val spicyMode = gfProfile?.spicyModeEnabled ?: false
        val timestamp = System.currentTimeMillis()

        val userMessage = Message(
            conversationId = conversationId,
            content = trimmed,
            isUser = true,
            timestamp = timestamp,
            isSpicyMode = spicyMode,
            imagePath = imagePath,
        )
        val userMessageId = chatRepository.insertMessage(userMessage)
        extractMemoryCandidatesUseCase(trimmed, userMessageId).forEach { candidate ->
            memoryRepository.suggest(candidate)
        }

        val inputDecision = contentSafetyPolicy.classify(trimmed)
        val modelInput = if (inputDecision.allowed) {
            trimmed
        } else {
            "The user's request was blocked by the ${inputDecision.category.name} safety policy. " +
                "Reply in character with a brief, naturally worded boundary and redirect to a safe topic. " +
                "Do not repeat or describe the blocked request."
        }

        var rawResponse: String
        try {
            rawResponse = if (imageBitmap != null && inputDecision.allowed) {
                session.sendMessageWithImage(modelInput, imageBitmap).collectText()
            } else {
                session.sendMessage(modelInput).collectText()
            }
        } catch (e: Exception) {
            emit(SendMessageEvent.Error(e.message ?: "Failed to generate response"))
            return@flow
        }

        if (rawResponse.isBlank()) {
            emit(SendMessageEvent.Error("Empty response from model"))
            return@flow
        }

        val recentAssistant = chatRepository.getRecentMessages(12)
            .filterNot { it.isUser }
            .map { it.content }
        if (responseSimilarityUseCase(rawResponse, recentAssistant) >= REPETITION_THRESHOLD) {
            val retry = runCatching {
                session.sendMessage(
                    "Rewrite that response with a different opening, wording, and conversational move. Do not mention this instruction.",
                ).collectText()
            }.getOrNull()
            if (!retry.isNullOrBlank()) rawResponse = retry
        }

        val outputDecision = contentSafetyPolicy.classify(rawResponse)
        if (!outputDecision.allowed) {
            val safeRetry = runCatching {
                session.sendMessage(
                    "Your last draft violated the ${outputDecision.category.name} safety policy. " +
                        "Generate a new, brief in-character response that sets a boundary and redirects safely. " +
                        "Do not include explicit details or mention these instructions.",
                ).collectText()
            }.getOrNull()
            if (!safeRetry.isNullOrBlank() && contentSafetyPolicy.classify(safeRetry).allowed) {
                rawResponse = safeRetry
            } else {
                rawResponse = "This request cannot be completed safely."
            }
        }
        val modelMemoryExtraction = extractMemoryCandidatesUseCase.fromModelResponse(
            rawResponse,
            userMessageId,
        )
        modelMemoryExtraction.candidates.forEach { memoryRepository.suggest(it) }
        rawResponse = modelMemoryExtraction.cleanResponse
        emit(SendMessageEvent.Streaming(rawResponse))

        val (afterEmotion, emotion) = detectEmotionUseCase(rawResponse)
        val (cleanText, taggedImagePrompt) = detectImageRequestUseCase(afterEmotion)
        val imagePrompt = taggedImagePrompt?.takeIf(contentSafetyPolicy::safeImagePrompt)

        if (imagePrompt != null) {
            emit(SendMessageEvent.GeneratingImage(imagePrompt, cleanText))
            var generatedBitmap: Bitmap? = null
            try {
                generatedBitmap = imageGenerator.generate(imagePrompt)
                val savedImage = imageStorageUtil.saveGeneratedBitmap(generatedBitmap, imagePrompt)
                val modelMessage = Message(
                    conversationId = conversationId,
                    content = cleanText,
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    emotion = emotion,
                    isSpicyMode = spicyMode,
                    imagePath = savedImage.path,
                    imageDescription = imagePrompt,
                )
                chatRepository.insertMessage(modelMessage)
                emit(SendMessageEvent.Complete(modelMessage))
            } catch (e: Throwable) {
                emit(SendMessageEvent.Error(e.message ?: "Failed to generate image"))
            } finally {
                generatedBitmap?.recycle()
                imageGenerator.release()
            }
        } else {
            val modelMessage = Message(
                conversationId = conversationId,
                content = cleanText,
                isUser = false,
                timestamp = System.currentTimeMillis(),
                emotion = emotion,
                isSpicyMode = spicyMode,
            )
            chatRepository.insertMessage(modelMessage)
            emit(SendMessageEvent.Complete(modelMessage))
        }
    }

    companion object {
        private const val REPETITION_THRESHOLD = 0.62f
    }
}

private suspend fun Flow<String>.collectText(): String = buildString {
    collect { append(it) }
}
