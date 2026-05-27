package com.airgf.app.tts

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LipSyncBridge @Inject constructor() {

    enum class MouthShape(val frameIndex: Int) {
        CLOSED(0),
        OPEN_A(1),
        NARROW_E(2),
        ROUND_O(3),
        WIDE_W(4),
        TEETH_F(5),
        LIPS_M(6),
        TONGUE_L(7),
    }

    private val phonemeMap = mapOf(
        'a' to MouthShape.OPEN_A,
        'e' to MouthShape.NARROW_E,
        'i' to MouthShape.NARROW_E,
        'o' to MouthShape.ROUND_O,
        'u' to MouthShape.ROUND_O,
        'w' to MouthShape.WIDE_W,
        'f' to MouthShape.TEETH_F,
        'v' to MouthShape.TEETH_F,
        'm' to MouthShape.LIPS_M,
        'b' to MouthShape.LIPS_M,
        'p' to MouthShape.LIPS_M,
        'l' to MouthShape.TONGUE_L,
        't' to MouthShape.TONGUE_L,
        'd' to MouthShape.TONGUE_L,
        'n' to MouthShape.TONGUE_L,
    )

    fun getVisemeForWord(word: String): MouthShape {
        val c = word.lowercase().firstOrNull() ?: return MouthShape.CLOSED
        return phonemeMap[c] ?: MouthShape.OPEN_A
    }
}
