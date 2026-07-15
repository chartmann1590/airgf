package com.airgf.app.domain.usecase

import com.airgf.app.domain.model.MemoryCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryAndRepetitionUseCaseTest {
    @Test fun `extracts reviewable preference without sensitive content`() {
        val memories = ExtractMemoryCandidatesUseCase()("I really love hiking in the mountains", 42, 1000)
        assertEquals(1, memories.size)
        assertEquals(MemoryCategory.PREFERENCE, memories.single().category)
        assertEquals("They enjoy hiking in the mountains", memories.single().content)
    }

    @Test fun `does not learn credentials`() {
        assertTrue(ExtractMemoryCandidatesUseCase()("Remember that my password is secret123", 42).isEmpty())
    }

    @Test fun `extracts and removes model proposed memories`() {
        val extraction = ExtractMemoryCandidatesUseCase().fromModelResponse(
            "That sounds fun.\n[MEMORY:PREFERENCE:They enjoy trail running]\n[EMOTION:HAPPY]",
            sourceMessageId = 9,
        )

        assertEquals(1, extraction.candidates.size)
        assertTrue(extraction.cleanResponse.contains("That sounds fun"))
        assertFalse(extraction.cleanResponse.contains("MEMORY:"))
    }

    @Test fun `detects repeated response structure`() {
        val score = ResponseSimilarityUseCase()(
            "I am glad you told me about your day and I want to hear more",
            listOf("I am glad you told me about your day and I want to hear more"),
        )
        assertTrue(score > 0.9f)
    }
}
