package com.airgf.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import com.airgf.app.domain.model.VisualTemplate
import com.airgf.app.presentation.theme.Background
import com.airgf.app.presentation.theme.OnSurface
import com.airgf.app.presentation.theme.Primary
import com.airgf.app.presentation.theme.SurfaceContainer

@Composable
fun TemplateCard(
    template: VisualTemplate,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
    ) {
        CharacterThumbnail(
            template = template,
            modifier = Modifier.fillMaxSize(),
        )
        if (template.isOnDemand) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(50),
                color = Primary.copy(alpha = 0.18f),
            ) {
                Text(
                    text = template.deliveryMode.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Background.copy(alpha = 0.8f)),
                        startY = 100f,
                    ),
                ),
        )
        Text(
            text = template.displayName,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = OnSurface,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp),
        )
    }
}
