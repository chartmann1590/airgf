package com.airgf.app.presentation.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import com.airgf.app.domain.model.VisualTemplate
import com.airgf.app.presentation.theme.OnSurface

@Composable
fun CharacterThumbnail(
    template: VisualTemplate,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val assetPath = template.thumbnailAssetPath
    val imageBitmap = remember(template) {
        runCatching {
            context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = template.displayName,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        ProceduralAvatarPreview(
            template = template,
            modifier = modifier,
        )
    }
}

@Composable
internal fun ProceduralAvatarPreview(
    template: VisualTemplate,
    modifier: Modifier = Modifier,
) {
    val palette = template.fallbackPalette
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(palette.backdropStartColor),
                        Color(palette.backdropEndColor),
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.72f)
                .size(84.dp)
                .clip(CircleShape)
                .background(Color(palette.hairColor).copy(alpha = 0.95f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.68f)
                .fillMaxSize(0.62f)
                .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp))
                .background(Color(palette.skinColor)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.9f)
                .fillMaxSize(0.36f)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color(palette.accentColor).copy(alpha = 0.2f))
                .border(
                    width = 1.dp,
                    color = Color(palette.accentColor).copy(alpha = 0.45f),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                ),
        )
        Text(
            text = template.displayName.first().uppercase(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = OnSurface,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
