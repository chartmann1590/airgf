package com.airgf.app.animation

import com.airgf.app.tts.LipSyncBridge
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlendShapeLipSync @Inject constructor() {
    fun poseFor(shape: LipSyncBridge.MouthShape): AvatarFacePose = when (shape) {
        LipSyncBridge.MouthShape.CLOSED -> AvatarFacePose(
            visemeSil = 1f,
            mouthPress = 0.05f,
        )
        LipSyncBridge.MouthShape.OPEN_A -> AvatarFacePose(
            jawOpen = 0.72f,
            visemeAa = 1f,
            mouthOpen = 0.9f,
            speakingBob = 1f,
        )
        LipSyncBridge.MouthShape.NARROW_E -> AvatarFacePose(
            jawOpen = 0.32f,
            visemeE = 1f,
            mouthSmile = 0.2f,
            speakingBob = 0.45f,
        )
        LipSyncBridge.MouthShape.ROUND_O -> AvatarFacePose(
            jawOpen = 0.48f,
            visemeO = 1f,
            mouthPucker = 0.75f,
            speakingBob = 0.65f,
        )
        LipSyncBridge.MouthShape.WIDE_W -> AvatarFacePose(
            jawOpen = 0.36f,
            visemeU = 0.85f,
            mouthPucker = 0.45f,
            speakingBob = 0.4f,
        )
        LipSyncBridge.MouthShape.TEETH_F -> AvatarFacePose(
            jawOpen = 0.2f,
            visemeFf = 1f,
            mouthPress = 0.2f,
            speakingBob = 0.3f,
        )
        LipSyncBridge.MouthShape.LIPS_M -> AvatarFacePose(
            visemePp = 1f,
            mouthPress = 0.8f,
            speakingBob = 0.12f,
        )
        LipSyncBridge.MouthShape.TONGUE_L -> AvatarFacePose(
            jawOpen = 0.3f,
            visemeTh = 1f,
            mouthOpen = 0.35f,
            speakingBob = 0.28f,
        )
    }
}
