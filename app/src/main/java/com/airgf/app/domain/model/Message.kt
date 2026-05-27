package com.airgf.app.domain.model

data class Message(
    val id: Long = 0,
    val conversationId: Long,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long,
    val emotion: EmotionState? = null,
    val isSpicyMode: Boolean = false,
    val imagePath: String? = null,
    val imageDescription: String? = null,
)
