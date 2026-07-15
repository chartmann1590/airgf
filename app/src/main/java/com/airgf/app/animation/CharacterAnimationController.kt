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

    enum class AvatarAction { IDLE, WALKING, TALKING, WAVING, DANCING, REACTING }

    private val _currentEmotion = MutableStateFlow(EmotionState.NEUTRAL)
    val currentEmotion: StateFlow<EmotionState> = _currentEmotion.asStateFlow()

    private val _currentMouthShape = MutableStateFlow(LipSyncBridge.MouthShape.CLOSED)
    val currentMouthShape: StateFlow<LipSyncBridge.MouthShape> = _currentMouthShape.asStateFlow()

    private val _currentAction = MutableStateFlow(AvatarAction.IDLE)
    val currentAction: StateFlow<AvatarAction> = _currentAction.asStateFlow()

    fun setEmotion(emotion: EmotionState) {
        _currentEmotion.value = emotion
        if (_currentAction.value != AvatarAction.TALKING) _currentAction.value = AvatarAction.REACTING
    }

    fun setMouthShape(shape: LipSyncBridge.MouthShape) {
        _currentMouthShape.value = shape
        _currentAction.value = if (shape == LipSyncBridge.MouthShape.CLOSED) AvatarAction.IDLE else AvatarAction.TALKING
    }

    fun requestAction(action: AvatarAction) {
        _currentAction.value = action
    }
}
