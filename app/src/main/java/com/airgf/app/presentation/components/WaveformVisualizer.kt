package com.airgf.app.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airgf.app.presentation.theme.Primary
import com.airgf.app.presentation.theme.Secondary
import kotlin.math.sin

enum class WaveformStyle {
    SOFT,
    ENERGETIC,
    MATURE,
    BREATHY,
}

@Composable
fun WaveformVisualizer(
    style: WaveformStyle,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (style) {
                    WaveformStyle.SOFT -> 2000
                    WaveformStyle.ENERGETIC -> 600
                    WaveformStyle.MATURE -> 1500
                    WaveformStyle.BREATHY -> 2500
                },
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    val barCount = 12
    val color = when (style) {
        WaveformStyle.SOFT -> Primary.copy(alpha = 0.7f)
        WaveformStyle.ENERGETIC -> Secondary
        WaveformStyle.MATURE -> Primary.copy(alpha = 0.5f)
        WaveformStyle.BREATHY -> Primary.copy(alpha = 0.4f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp),
    ) {
        if (!isActive) return@Canvas
        val barWidth = size.width / (barCount * 2f)
        val maxHeight = size.height
        for (i in 0 until barCount) {
            val amplitude = when (style) {
                WaveformStyle.SOFT -> 0.3f + 0.2f * sin(Math.toRadians((phase + i * 30).toDouble())).toFloat()
                WaveformStyle.ENERGETIC -> 0.2f + 0.6f * sin(Math.toRadians((phase * 2 + i * 45).toDouble())).toFloat()
                WaveformStyle.MATURE -> 0.15f + 0.15f * sin(Math.toRadians((phase + i * 20).toDouble())).toFloat()
                WaveformStyle.BREATHY -> 0.1f + 0.25f * sin(Math.toRadians((phase * 0.5 + i * 25).toDouble())).toFloat()
            }
            val barHeight = maxHeight * amplitude
            val x = i * (barWidth * 2) + barWidth / 2
            val y = (maxHeight - barHeight) / 2
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
            )
        }
    }
}
