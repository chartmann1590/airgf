package com.airgf.app.domain.usecase

import javax.inject.Inject

class ResponseSimilarityUseCase @Inject constructor() {
    operator fun invoke(candidate: String, previous: List<String>): Float {
        val candidateTokens = shingles(candidate)
        if (candidateTokens.isEmpty()) return 0f
        return previous.maxOfOrNull { old ->
            val oldTokens = shingles(old)
            val union = candidateTokens union oldTokens
            if (union.isEmpty()) 0f else (candidateTokens intersect oldTokens).size.toFloat() / union.size
        } ?: 0f
    }

    private fun shingles(text: String): Set<String> {
        val words = text.lowercase().replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size < 3) return words.toSet()
        return words.windowed(3).map { it.joinToString(" ") }.toSet()
    }
}
