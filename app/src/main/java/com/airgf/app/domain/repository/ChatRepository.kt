package com.airgf.app.domain.repository

import com.airgf.app.domain.model.Conversation
import com.airgf.app.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessagesFlow(conversationId: Long): Flow<List<Message>>
    suspend fun getRecentMessages(limit: Int): List<Message>
    suspend fun insertMessage(message: Message): Long
    suspend fun getOrCreateConversation(): Long
    suspend fun deleteAllMessages()
    suspend fun getAllConversations(): List<Conversation>
    suspend fun getAllMessages(): List<Message>
}
