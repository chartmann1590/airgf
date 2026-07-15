package com.airgf.app.llm

import android.graphics.Bitmap
import com.google.ai.edge.litertlm.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicBoolean

class LlmSession(
    private val conversation: Conversation,
    private val imageDescriptionService: ImageDescriptionService,
    private val onClosed: (() -> Unit)? = null,
) : AutoCloseable {
    private val isClosed = AtomicBoolean(false)

    fun sendMessage(text: String): Flow<String> =
        conversation.sendMessageAsync(text)
            .map { chunk -> chunk.toString() }
            .flowOn(Dispatchers.IO)

    fun sendMessageWithImage(text: String, imageBitmap: Bitmap?): Flow<String> = flow {
        if (imageBitmap == null) {
            emitAll(sendMessage(text))
            return@flow
        }
        val description = imageDescriptionService.describe(imageBitmap)
        val prompt = buildString {
            if (description != null) {
                append("[The user shared a photo. On-device image analysis: $description]")
            } else {
                append("[The user shared a photo, but image analysis is unavailable on this device. " +
                    "Do not pretend to see details; respond naturally and ask what they want to share about it.]")
            }
            if (text.isNotBlank()) {
                append("\n")
                append(text)
            }
        }
        emitAll(sendMessage(prompt))
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            runCatching {
                conversation.close()
            }
            onClosed?.invoke()
        }
    }
}
