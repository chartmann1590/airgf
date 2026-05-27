package com.airgf.app.animation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.airgf.app.domain.model.EmotionState

@Composable
fun rememberExpressionAlpha(targetEmotion: EmotionState): Float {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "expressionAlpha",
    )
    return alpha
}
