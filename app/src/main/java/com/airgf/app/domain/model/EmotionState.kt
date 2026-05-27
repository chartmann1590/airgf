package com.airgf.app.domain.model

enum class EmotionState(val spriteRow: Int) {
    NEUTRAL(0),
    HAPPY(1),
    SAD(2),
    FLIRTY(3),
    THINKING(4),
    SURPRISED(5),
    LAUGHING(6),
    SHY(7),
    ;

    val assetSuffix: String
        get() = name.lowercase()

    val label: String
        get() = when (this) {
            NEUTRAL -> "neutral"
            HAPPY -> "happy"
            SAD -> "sad"
            FLIRTY -> "flirty"
            THINKING -> "thoughtful"
            SURPRISED -> "surprised"
            LAUGHING -> "joyful"
            SHY -> "shy"
        }

    val emoji: String
        get() = when (this) {
            NEUTRAL -> "🙂"
            HAPPY -> "😊"
            SAD -> "😢"
            FLIRTY -> "😏"
            THINKING -> "🤔"
            SURPRISED -> "😮"
            LAUGHING -> "😄"
            SHY -> "😳"
        }
}
