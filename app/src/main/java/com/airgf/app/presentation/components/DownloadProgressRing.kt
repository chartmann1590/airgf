package com.airgf.app.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airgf.app.presentation.theme.OnSurface
import com.airgf.app.presentation.theme.OnSurfaceVariant
import com.airgf.app.presentation.theme.Primary
import com.airgf.app.presentation.theme.SecondaryContainer
import com.airgf.app.presentation.theme.SurfaceVariant

@Composable
fun DownloadProgressRing(
    percent: Float,
    modifier: Modifier = Modifier,
    pulseWhenComplete: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val clampedPercent = percent.coerceIn(0f, 100f)
    val sweepAngle = (clampedPercent / 100f) * 360f
    val scale = if (pulseWhenComplete) pulseScale else 1f

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthBg = 4.dp.toPx()
            val strokeWidthProgress = 6.dp.toPx()
            val diameter = size.minDimension - strokeWidthProgress
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Primary.copy(alpha = 0.2f),
                        Primary.copy(alpha = 0f),
                    ),
                ),
                radius = size.minDimension / 2f,
            )

            drawArc(
                color = SurfaceVariant,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthBg, cap = StrokeCap.Round),
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(Primary, SecondaryContainer, Primary),
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthProgress, cap = StrokeCap.Round),
            )
        }

        Row(
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "${clampedPercent.toInt()}",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                color = OnSurface,
            )
            Text(
                text = "%",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = OnSurfaceVariant,
                modifier = Modifier.align(Alignment.Bottom),
            )
        }
    }
}
