package com.airgf.app.data.repository

import com.airgf.app.data.local.db.dao.ConversationDao
import com.airgf.app.data.local.db.dao.MessageDao
import com.airgf.app.data.local.db.entity.ConversationEntity
import com.airgf.app.data.local.db.entity.MessageEntity
import com.airgf.app.domain.model.Conversation
import com.airgf.app.domain.model.EmotionState
import com.airgf.app.domain.model.Message
import com.airgf.app.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
) : ChatRepository {

    override fun getMessagesFlow(conversationId: Long): Flow<List<Message>> =
        messageDao.getMessagesFlow(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getRecentMessages(limit: Int): List<Message> =
        messageDao.getRecentMessages(limit).map { it.toDomain() }

    override suspend fun insertMessage(message: Message): Long {
        val messageId = messageDao.insert(message.toEntity())
        conversationDao.updateLastMessageTime(message.conversationId, message.timestamp)
        return messageId
    }

    override suspend fun getOrCreateConversation(): Long {
        val existing = conversationDao.getLatestConversation()
        if (existing != null) return existing.id

        val now = System.currentTimeMillis()
        return conversationDao.insert(
            ConversationEntity(
                title = null,
                createdAt = now,
                lastMessageAt = now,
            ),
        )
    }

    override suspend fun deleteAllMessages() {
        messageDao.deleteAll()
        conversationDao.deleteAll()
    }

    override suspend fun getAllConversations(): List<Conversation> =
        conversationDao.getAll().map { it.toDomain() }

    override suspend fun getAllMessages(): List<Message> =
        messageDao.getAllMessages().map { it.toDomain() }

    private fun ConversationEntity.toDomain(): Conversation = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        lastMessageAt = lastMessageAt,
    )

    private fun MessageEntity.toDomain(): Message = Message(
        id = id,
        conversationId = conversationId,
        content = content,
        isUser = role == ROLE_USER,
        timestamp = timestamp,
        emotion = emotionTag?.let { tag ->
            runCatching { EmotionState.valueOf(tag) }.getOrNull()
        },
        isSpicyMode = isSpicyMode,
        imagePath = imagePath,
        imageDescription = imageDescription,
    )

    private fun Message.toEntity(): MessageEntity = MessageEntity(
        id = id,
        conversationId = conversationId,
        role = if (isUser) ROLE_USER else ROLE_MODEL,
        content = content,
        timestamp = timestamp,
        emotionTag = emotion?.name,
        isSpicyMode = isSpicyMode,
        imagePath = imagePath,
        imageDescription = imageDescription,
    )

    companion object {
        private const val ROLE_USER = "user"
        private const val ROLE_MODEL = "model"
    }
}
