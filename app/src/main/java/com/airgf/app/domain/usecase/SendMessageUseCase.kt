package com.airgf.app.domain.usecase

import android.graphics.Bitmap
import com.airgf.app.core.util.ImageStorageUtil
import com.airgf.app.domain.model.Message
import com.airgf.app.domain.repository.ChatRepository
import com.airgf.app.domain.repository.GfConfigRepository
import com.airgf.app.imagegen.ImageGenerator
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
        chatRepository.insertMessage(userMessage)

        val explicitImagePrompt = gfProfile?.let { buildDirectImagePrompt(trimmed, it.name) }

        val responseBuilder = StringBuilder()
        try {
            if (imageBitmap != null) {
                session.sendMessageWithImage(trimmed, imageBitmap).collect { chunk ->
                    responseBuilder.append(chunk)
                    emit(SendMessageEvent.Streaming(responseBuilder.toString()))
                }
            } else {
                session.sendMessage(trimmed).collect { chunk ->
                    responseBuilder.append(chunk)
                    emit(SendMessageEvent.Streaming(responseBuilder.toString()))
                }
            }
        } catch (e: Exception) {
            emit(SendMessageEvent.Error(e.message ?: "Failed to generate response"))
            return@flow
        }

        val rawResponse = responseBuilder.toString()
        if (rawResponse.isBlank()) {
            emit(SendMessageEvent.Error("Empty response from model"))
            return@flow
        }

        val (afterEmotion, emotion) = detectEmotionUseCase(rawResponse)
        val (cleanText, taggedImagePrompt) = detectImageRequestUseCase(afterEmotion)
        val imagePrompt = taggedImagePrompt ?: explicitImagePrompt

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

    private fun buildDirectImagePrompt(text: String, gfName: String): String? {
        val normalized = text.lowercase()
        val asksForImage = listOf(
            "send me a picture",
            "send me a pic",
            "send pic",
            "send a pic",
            "send picture",
            "send photo",
            "send me a photo",
            "send me selfie",
            "send me a selfie",
            "send selfie",
            "show me a picture",
            "show me a pic",
            "show me a photo",
            "show me yourself",
            "picture of yourself",
            "photo of yourself",
            "pic of yourself",
            "selfie",
        ).any(normalized::contains)

        if (!asksForImage) return null

        val isSelfie = listOf("selfie", "yourself", "you").any(normalized::contains)
        val subject = if (isSelfie) {
            "$gfName, an adult woman, warm natural selfie"
        } else {
            "$gfName, an adult woman, personal photo"
        }
        return "$subject, expressive eyes, soft flattering light, tasteful outfit, realistic portrait, high detail"
    }
}
