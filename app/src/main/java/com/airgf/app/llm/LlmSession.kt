package com.airgf.app.llm

import android.graphics.Bitmap
import com.airgf.app.core.util.BitmapDescriber
import com.google.ai.edge.litertlm.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicBoolean

class LlmSession(
    private val conversation: Conversation,
) : AutoCloseable {
    private val isClosed = AtomicBoolean(false)

    fun sendMessage(text: String): Flow<String> =
        conversation.sendMessageAsync(text)
            .map { chunk -> chunk.toString() }
            .flowOn(Dispatchers.IO)

    fun sendMessageWithImage(text: String, imageBitmap: Bitmap?): Flow<String> {
        if (imageBitmap == null) return sendMessage(text)
        val description = runCatching { BitmapDescriber.describe(imageBitmap) }
            .getOrDefault("The user shared a photo.")
        val prompt = buildString {
            append("[The user shared a photo with you. $description]")
            if (text.isNotBlank()) {
                append("\n")
                append(text)
            }
        }
        return sendMessage(prompt)
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            runCatching {
                conversation.close()
            }
        }
    }
}
