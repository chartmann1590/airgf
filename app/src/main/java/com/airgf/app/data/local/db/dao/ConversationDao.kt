package com.airgf.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.airgf.app.data.local.db.entity.ConversationEntity

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC LIMIT 1")
    suspend fun getLatestConversation(): ConversationEntity?

    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Query("SELECT * FROM conversations ORDER BY createdAt ASC")
    suspend fun getAll(): List<ConversationEntity>

    @Query("UPDATE conversations SET lastMessageAt = :timestamp WHERE id = :id")
    suspend fun updateLastMessageTime(id: Long, timestamp: Long)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}
