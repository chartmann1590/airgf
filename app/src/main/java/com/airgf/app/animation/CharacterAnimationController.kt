package com.airgf.app.animation

import com.airgf.app.domain.model.EmotionState
import com.airgf.app.tts.LipSyncBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterAnimationController @Inject constructor() {

    private val _currentEmotion = MutableStateFlow(EmotionState.NEUTRAL)
    val currentEmotion: StateFlow<EmotionState> = _currentEmotion.asStateFlow()

    private val _currentMouthShape = MutableStateFlow(LipSyncBridge.MouthShape.CLOSED)
    val currentMouthShape: StateFlow<LipSyncBridge.MouthShape> = _currentMouthShape.asStateFlow()

    fun setEmotion(emotion: EmotionState) {
        _currentEmotion.value = emotion
    }

    fun setMouthShape(shape: LipSyncBridge.MouthShape) {
        _currentMouthShape.value = shape
    }
}
