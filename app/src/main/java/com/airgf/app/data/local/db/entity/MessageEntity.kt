package com.airgf.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String,
    val content: String,
    val timestamp: Long,
    val emotionTag: String?,
    val isSpicyMode: Boolean,
    val imagePath: String? = null,
    val imageDescription: String? = null,
)
