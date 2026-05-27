package com.airgf.app.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.airgf.app.domain.model.Conversation
import com.airgf.app.domain.model.Message
import com.airgf.app.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ExportChatUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    suspend operator fun invoke(): Uri = withContext(Dispatchers.IO) {
        val conversations = chatRepository.getAllConversations()
        val messages = chatRepository.getAllMessages()
        val archive = buildArchive(conversations, messages)

        val exportDirectory = File(context.cacheDir, EXPORT_DIRECTORY).apply { mkdirs() }
        val exportFile = File(exportDirectory, "airgf-chat-${System.currentTimeMillis()}.json")
        exportFile.writeText(json.encodeToString(archive))

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile,
        )
    }

    private fun buildArchive(
        conversations: List<Conversation>,
        messages: List<Message>,
    ): ChatArchive {
        val messagesByConversation = messages.groupBy { it.conversationId }
        return ChatArchive(
            exportedAt = System.currentTimeMillis(),
            conversations = conversations.map { conversation ->
                ExportConversation(
                    id = conversation.id,
                    title = conversation.title,
                    createdAt = conversation.createdAt,
                    lastMessageAt = conversation.lastMessageAt,
                    messages = messagesByConversation[conversation.id].orEmpty().map { message ->
                        ExportMessage(
                            id = message.id,
                            role = if (message.isUser) "user" else "girlfriend",
                            content = message.content,
                            timestamp = message.timestamp,
                            emotion = message.emotion?.name,
                            spicyModeEnabled = message.isSpicyMode,
                            hasImage = message.imagePath != null,
                            imageDescription = message.imageDescription,
                        )
                    },
                )
            },
        )
    }

    @Serializable
    private data class ChatArchive(
        val exportedAt: Long,
        val conversations: List<ExportConversation>,
    )

    @Serializable
    private data class ExportConversation(
        val id: Long,
        val title: String?,
        val createdAt: Long,
        val lastMessageAt: Long,
        val messages: List<ExportMessage>,
    )

    @Serializable
    private data class ExportMessage(
        val id: Long,
        val role: String,
        val content: String,
        val timestamp: Long,
        val emotion: String?,
        val spicyModeEnabled: Boolean,
        val hasImage: Boolean = false,
        val imageDescription: String? = null,
    )

    companion object {
        private const val EXPORT_DIRECTORY = "exports"
    }
}
