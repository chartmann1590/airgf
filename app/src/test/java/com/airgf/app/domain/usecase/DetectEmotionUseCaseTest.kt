package com.airgf.app.domain.usecase

import com.airgf.app.domain.model.EmotionState
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectEmotionUseCaseTest {
    private val useCase = DetectEmotionUseCase()

    @Test
    fun `extracts emotion tag and trims reply`() {
        val (text, emotion) = useCase("Hey there. [EMOTION:FLIRTY]")

        assertEquals("Hey there.", text)
        assertEquals(EmotionState.FLIRTY, emotion)
    }

    @Test
    fun `falls back to neutral when tag is missing or invalid`() {
        val (plainText, plainEmotion) = useCase("Just chatting with you")
        val (invalidText, invalidEmotion) = useCase("Hmm... [EMOTION:NOT_REAL]")

        assertEquals("Just chatting with you", plainText)
        assertEquals(EmotionState.NEUTRAL, plainEmotion)
        assertEquals("Hmm...", invalidText)
        assertEquals(EmotionState.NEUTRAL, invalidEmotion)
    }
}
