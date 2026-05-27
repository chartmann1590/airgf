package com.airgf.app.animation

import com.airgf.app.domain.model.EmotionState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarExpressionDriver @Inject constructor() {
    fun poseFor(emotion: EmotionState): AvatarFacePose = when (emotion) {
        EmotionState.NEUTRAL -> AvatarFacePose()
        EmotionState.HAPPY -> AvatarFacePose(
            mouthSmile = 0.7f,
            cheekSquint = 0.28f,
        )
        EmotionState.SAD -> AvatarFacePose(
            mouthFrown = 0.45f,
            browInnerUp = 0.38f,
        )
        EmotionState.FLIRTY -> AvatarFacePose(
            mouthSmile = 0.38f,
            eyeSquint = 0.25f,
            cheekSquint = 0.18f,
            headTiltDegrees = 5f,
        )
        EmotionState.THINKING -> AvatarFacePose(
            browInnerUp = 0.58f,
            headTiltDegrees = -4f,
        )
        EmotionState.SURPRISED -> AvatarFacePose(
            jawOpen = 0.2f,
            eyeWide = 0.76f,
            browInnerUp = 0.62f,
            headTiltDegrees = -1.5f,
        )
        EmotionState.LAUGHING -> AvatarFacePose(
            mouthSmile = 1f,
            jawOpen = 0.32f,
            cheekSquint = 0.46f,
        )
        EmotionState.SHY -> AvatarFacePose(
            mouthSmile = 0.18f,
            eyeSquint = 0.4f,
            headTiltDegrees = -7f,
        )
    }
}
