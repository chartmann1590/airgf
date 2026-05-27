package com.airgf.app.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun rememberBreathOffsetPx(): Float {
    val density = LocalDensity.current
    val breathDp = 2.dp
    val breathPx = with(density) { breathDp.toPx() }
    val transition = rememberInfiniteTransition(label = "breath")
    val offset by transition.animateFloat(
        initialValue = -breathPx,
        targetValue = breathPx,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathOffset",
    )
    return offset
}

@Composable
fun rememberSwayOffsetPx(): Float {
    val density = LocalDensity.current
    val swayDp = 1.dp
    val swayPx = with(density) { swayDp.toPx() }
    val transition = rememberInfiniteTransition(label = "sway")
    val offset by transition.animateFloat(
        initialValue = -swayPx,
        targetValue = swayPx,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "swayOffset",
    )
    return offset
}

@Composable
fun rememberBlinkFrame(): Int {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(3000, 6000))
            for (i in 0..3) {
                frame = i
                delay(40L)
            }
            frame = 0
        }
    }
    return frame
}

@Composable
fun rememberPulseGlowScale(): Float {
    val transition = rememberInfiniteTransition(label = "pulseGlow")
    val scale by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    return scale
}
