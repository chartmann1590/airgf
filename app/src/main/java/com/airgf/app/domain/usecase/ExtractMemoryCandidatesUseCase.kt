package com.airgf.app.domain.usecase

import com.airgf.app.domain.model.CompanionMemory
import com.airgf.app.domain.model.MemoryCategory
import javax.inject.Inject

class ExtractMemoryCandidatesUseCase @Inject constructor() {
    data class ModelExtraction(
        val cleanResponse: String,
        val candidates: List<CompanionMemory>,
    )

    operator fun invoke(text: String, sourceMessageId: Long, now: Long = System.currentTimeMillis()): List<CompanionMemory> {
        val clean = text.trim().replace(Regex("\\s+"), " ")
        if (clean.length !in 8..300 || sensitiveTerms.any { clean.contains(it, ignoreCase = true) }) return emptyList()

        return patterns.mapNotNull { rule ->
            val match = rule.regex.find(clean) ?: return@mapNotNull null
            val detail = match.groupValues.getOrNull(1)?.trim(' ', '.', '!', '?') ?: return@mapNotNull null
            if (detail.length !in 2..120) return@mapNotNull null
            CompanionMemory(
                category = rule.category,
                content = rule.render(detail),
                sourceMessageId = sourceMessageId,
                confidence = 0.82f,
                createdAt = now,
                updatedAt = now,
            )
        }.distinctBy { it.content.lowercase() }.take(2)
    }

    fun fromModelResponse(
        response: String,
        sourceMessageId: Long,
        now: Long = System.currentTimeMillis(),
    ): ModelExtraction {
        val candidates = modelMemoryPattern.findAll(response).mapNotNull { match ->
            val category = runCatching { MemoryCategory.valueOf(match.groupValues[1].uppercase()) }.getOrNull()
                ?: return@mapNotNull null
            val content = match.groupValues[2].trim().replace(Regex("\\s+"), " ")
            if (content.length !in 4..160 || sensitiveTerms.any { content.contains(it, ignoreCase = true) }) {
                return@mapNotNull null
            }
            CompanionMemory(
                category = category,
                content = content,
                sourceMessageId = sourceMessageId,
                confidence = 0.75f,
                createdAt = now,
                updatedAt = now,
            )
        }.distinctBy { it.content.lowercase() }.take(2).toList()

        return ModelExtraction(
            cleanResponse = response.replace(modelMemoryPattern, "")
                .replace(Regex("\\n{3,}"), "\n\n")
                .trim(),
            candidates = candidates,
        )
    }

    private data class Rule(
        val regex: Regex,
        val category: MemoryCategory,
        val render: (String) -> String,
    )

    private val patterns = listOf(
        Rule(Regex("(?:i (?:really )?(?:like|love|enjoy))\\s+(.+)", RegexOption.IGNORE_CASE), MemoryCategory.PREFERENCE) { "They enjoy $it" },
        Rule(Regex("(?:i (?:do not|don't) like|i hate)\\s+(.+)", RegexOption.IGNORE_CASE), MemoryCategory.BOUNDARY) { "They dislike $it" },
        Rule(Regex("my (?:friend|partner|mom|mother|dad|father|sister|brother|pet)(?:'s)? name is\\s+(.+)", RegexOption.IGNORE_CASE), MemoryCategory.PERSON) { "Someone important to them is $it" },
        Rule(Regex("(?:i want to|my goal is to|i'm trying to)\\s+(.+)", RegexOption.IGNORE_CASE), MemoryCategory.GOAL) { "They want to $it" },
        Rule(Regex("remember (?:that )?(.+)", RegexOption.IGNORE_CASE), MemoryCategory.EVENT) { it },
    )

    private val sensitiveTerms = listOf(
        "password", "passcode", "credit card", "bank account", "social security", "medical diagnosis",
        "exact address", "private key", "api key", "sexual", "nude",
    )

    private val modelMemoryPattern = Regex(
        "\\[MEMORY:(PREFERENCE|PERSON|EVENT|BOUNDARY|GOAL|RELATIONSHIP):([^]\\r\\n]+)]",
        RegexOption.IGNORE_CASE,
    )
}
