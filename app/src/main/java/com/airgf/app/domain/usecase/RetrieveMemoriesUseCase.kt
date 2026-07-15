package com.airgf.app.domain.usecase

import com.airgf.app.domain.model.CompanionMemory
import com.airgf.app.domain.repository.MemoryRepository
import javax.inject.Inject

class RetrieveMemoriesUseCase @Inject constructor(
    private val repository: MemoryRepository,
) {
    suspend operator fun invoke(query: String, limit: Int = 8): List<CompanionMemory> {
        val terms = query.lowercase().split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }.toSet()
        return repository.getApproved()
            .map { memory: CompanionMemory ->
                val memoryTerms = memory.content.lowercase().split(Regex("[^a-z0-9]+"))
                val overlap = memoryTerms.count(terms::contains)
                val score = overlap * 4f + (if (memory.pinned) 10f else 0f) + memory.confidence
                memory to score
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
}
