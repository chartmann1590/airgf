package com.airgf.app.domain.usecase

import com.airgf.app.domain.model.EmotionState
import javax.inject.Inject

class DetectEmotionUseCase @Inject constructor() {
    private val emotionRegex = Regex("\\[EMOTION:(\\w+)]")

    operator fun invoke(rawResponse: String): Pair<String, EmotionState> {
        val match = emotionRegex.find(rawResponse)
        val emotion = match?.groupValues?.get(1)?.let { tag ->
            runCatching { EmotionState.valueOf(tag) }.getOrDefault(EmotionState.NEUTRAL)
        } ?: EmotionState.NEUTRAL
        val cleanText = rawResponse.replace(emotionRegex, "").trim()
        return cleanText to emotion
    }
}
