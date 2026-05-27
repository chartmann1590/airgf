package com.airgf.app.animation

data class AvatarFacePose(
    val jawOpen: Float = 0f,
    val visemeSil: Float = 0f,
    val visemeAa: Float = 0f,
    val visemeE: Float = 0f,
    val visemeO: Float = 0f,
    val visemeU: Float = 0f,
    val visemeFf: Float = 0f,
    val visemePp: Float = 0f,
    val visemeTh: Float = 0f,
    val mouthOpen: Float = 0f,
    val mouthSmile: Float = 0f,
    val mouthFrown: Float = 0f,
    val mouthPucker: Float = 0f,
    val mouthPress: Float = 0f,
    val browInnerUp: Float = 0f,
    val browDown: Float = 0f,
    val eyeBlink: Float = 0f,
    val eyeWide: Float = 0f,
    val eyeSquint: Float = 0f,
    val cheekSquint: Float = 0f,
    val headTiltDegrees: Float = 0f,
    val speakingBob: Float = 0f,
) {
    operator fun plus(other: AvatarFacePose): AvatarFacePose = AvatarFacePose(
        jawOpen = (jawOpen + other.jawOpen).coerceIn(0f, 1f),
        visemeSil = (visemeSil + other.visemeSil).coerceIn(0f, 1f),
        visemeAa = (visemeAa + other.visemeAa).coerceIn(0f, 1f),
        visemeE = (visemeE + other.visemeE).coerceIn(0f, 1f),
        visemeO = (visemeO + other.visemeO).coerceIn(0f, 1f),
        visemeU = (visemeU + other.visemeU).coerceIn(0f, 1f),
        visemeFf = (visemeFf + other.visemeFf).coerceIn(0f, 1f),
        visemePp = (visemePp + other.visemePp).coerceIn(0f, 1f),
        visemeTh = (visemeTh + other.visemeTh).coerceIn(0f, 1f),
        mouthOpen = (mouthOpen + other.mouthOpen).coerceIn(0f, 1f),
        mouthSmile = (mouthSmile + other.mouthSmile).coerceIn(-1f, 1f),
        mouthFrown = (mouthFrown + other.mouthFrown).coerceIn(0f, 1f),
        mouthPucker = (mouthPucker + other.mouthPucker).coerceIn(0f, 1f),
        mouthPress = (mouthPress + other.mouthPress).coerceIn(0f, 1f),
        browInnerUp = (browInnerUp + other.browInnerUp).coerceIn(0f, 1f),
        browDown = (browDown + other.browDown).coerceIn(0f, 1f),
        eyeBlink = (eyeBlink + other.eyeBlink).coerceIn(0f, 1f),
        eyeWide = (eyeWide + other.eyeWide).coerceIn(0f, 1f),
        eyeSquint = (eyeSquint + other.eyeSquint).coerceIn(0f, 1f),
        cheekSquint = (cheekSquint + other.cheekSquint).coerceIn(0f, 1f),
        headTiltDegrees = (headTiltDegrees + other.headTiltDegrees).coerceIn(-12f, 12f),
        speakingBob = (speakingBob + other.speakingBob).coerceIn(0f, 1f),
    )

    fun toMorphWeights(blendShapeOrder: List<String>): FloatArray =
        blendShapeOrder.map { name ->
            when (name.lowercase()) {
                "jawopen" -> jawOpen
                "viseme_sil" -> visemeSil
                "viseme_aa" -> visemeAa
                "viseme_e" -> visemeE
                "viseme_o" -> visemeO
                "viseme_u" -> visemeU
                "viseme_ff" -> visemeFf
                "viseme_pp" -> visemePp
                "viseme_th" -> visemeTh
                "mouthopen" -> mouthOpen
                "mouthsmile" -> mouthSmile.coerceAtLeast(0f)
                "mouthfrown" -> mouthFrown
                "mouthpucker" -> mouthPucker
                "mouthpress" -> mouthPress
                "browinnerup" -> browInnerUp
                "browdownleft", "browdownright" -> browDown
                "eyeblinkleft", "eyeblinkright" -> eyeBlink
                "eyewideleft", "eyewideright" -> eyeWide
                "eyesquintleft", "eyesquintright" -> eyeSquint
                "cheeksquintleft", "cheeksquintright" -> cheekSquint
                else -> 0f
            }
        }.toFloatArray()
}
