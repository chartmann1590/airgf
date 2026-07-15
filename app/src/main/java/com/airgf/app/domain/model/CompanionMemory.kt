package com.airgf.app.domain.model

data class CompanionMemory(
    val id: Long = 0,
    val category: MemoryCategory,
    val content: String,
    val sourceMessageId: Long?,
    val confidence: Float,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAccessedAt: Long? = null,
    val state: MemoryState = MemoryState.SUGGESTED,
    val pinned: Boolean = false,
    val sensitive: Boolean = false,
)

enum class MemoryCategory { PREFERENCE, PERSON, EVENT, BOUNDARY, GOAL, RELATIONSHIP }
enum class MemoryState { SUGGESTED, APPROVED, DELETED }
