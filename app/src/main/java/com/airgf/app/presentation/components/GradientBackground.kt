package com.airgf.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.airgf.app.presentation.theme.BackgroundGradientBottom
import com.airgf.app.presentation.theme.BackgroundGradientTop
import com.airgf.app.presentation.theme.PrimaryContainer

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    showRadialGlow: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BackgroundGradientTop, BackgroundGradientBottom),
                ),
            )
            .statusBarsPadding(),
    ) {
        if (showRadialGlow) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PrimaryContainer.copy(alpha = 0.15f),
                                Color.Transparent,
                            ),
                            center = Offset(0.5f, 0.2f),
                            radius = 800f,
                        ),
                    ),
            )
        }
        content()
    }
}
