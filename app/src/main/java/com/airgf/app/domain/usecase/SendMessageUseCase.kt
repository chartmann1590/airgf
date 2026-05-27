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

        val spicyMode = gfConfigRepository.getProfile()?.spicyModeEnabled ?: false
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
        val (cleanText, imagePrompt) = detectImageRequestUseCase(afterEmotion)

        if (imagePrompt != null && imageGenerator.isAvailable()) {
            emit(SendMessageEvent.GeneratingImage(imagePrompt, cleanText))
            try {
                val generatedBitmap = imageGenerator.generate(imagePrompt)
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
            } catch (_: Exception) {
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
}
