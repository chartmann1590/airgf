package com.airgf.app.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.airgf.app.presentation.theme.Primary
import com.airgf.app.presentation.theme.Secondary
import kotlin.random.Random

data class Particle(
    val x: Float,
    val size: Float,
    val duration: Int,
    val delay: Int,
    val useSecondary: Boolean,
)

@Composable
fun FloatingParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 18,
) {
    val particles = remember {
        List(particleCount) {
            Particle(
                x = Random.nextFloat(),
                size = Random.nextFloat() * 30f + 10f,
                duration = Random.nextInt(8000, 15000),
                delay = Random.nextInt(0, 5000),
                useSecondary = Random.nextBoolean(),
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "progress",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEachIndexed { index, particle ->
            val particleProgress = ((progress * particle.duration + particle.delay + index * 500) % particle.duration) / particle.duration.toFloat()
            val y = size.height * (1f - particleProgress)
            val x = particle.x * size.width
            val alpha = when {
                particleProgress < 0.1f -> particleProgress * 10f
                particleProgress > 0.9f -> (1f - particleProgress) * 10f
                else -> 0.6f
            }
            val color = if (particle.useSecondary) Secondary else Primary
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha * 0.4f), Color.Transparent),
                ),
                radius = particle.size,
                center = Offset(x, y),
            )
        }
    }
}
