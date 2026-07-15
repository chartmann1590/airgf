package com.airgf.app.data.repository

import com.airgf.app.data.local.db.dao.CompanionMemoryDao
import com.airgf.app.data.local.db.entity.CompanionMemoryEntity
import com.airgf.app.domain.model.CompanionMemory
import com.airgf.app.domain.model.MemoryCategory
import com.airgf.app.domain.model.MemoryState
import com.airgf.app.domain.repository.MemoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class MemoryRepositoryImpl @Inject constructor(
    private val dao: CompanionMemoryDao,
) : MemoryRepository {
    override fun observeMemories(): Flow<List<CompanionMemory>> =
        dao.observeAll().map { rows -> rows.map { row -> row.toDomain() } }

    override suspend fun getApproved(): List<CompanionMemory> = dao.getApproved().map { it.toDomain() }

    override suspend fun suggest(memory: CompanionMemory): Long {
        val normalized = normalize(memory.content)
        if (normalized.isBlank() || dao.findDuplicate(normalized) != null) return -1
        return dao.insert(memory.toEntity(normalized))
    }

    override suspend fun setState(id: Long, state: MemoryState) {
        dao.updateState(id, state.name, System.currentTimeMillis())
    }

    override suspend fun deleteAll() = dao.deleteAll()

    private fun normalize(value: String): String = value.lowercase()
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun CompanionMemoryEntity.toDomain() = CompanionMemory(
        id = id,
        category = runCatching { MemoryCategory.valueOf(category) }.getOrDefault(MemoryCategory.EVENT),
        content = content,
        sourceMessageId = sourceMessageId,
        confidence = confidence,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastAccessedAt = lastAccessedAt,
        state = runCatching { MemoryState.valueOf(state) }.getOrDefault(MemoryState.SUGGESTED),
        pinned = pinned,
        sensitive = sensitive,
    )

    private fun CompanionMemory.toEntity(normalized: String) = CompanionMemoryEntity(
        id = id,
        category = category.name,
        content = content,
        normalizedContent = normalized,
        sourceMessageId = sourceMessageId,
        confidence = confidence,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastAccessedAt = lastAccessedAt,
        state = state.name,
        pinned = pinned,
        sensitive = sensitive,
    )
}
