package com.airgf.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airgf.app.core.di.AvatarAssetRepositoryEntryPoint
import com.airgf.app.data.model.AvatarModelSource
import com.airgf.app.domain.model.VisualTemplate
import dagger.hilt.android.EntryPointAccessors
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

@Composable
fun AvatarModelPreview(
    template: VisualTemplate,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    scaleToUnits: Float = 1.6f,
) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val assetRepository = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AvatarAssetRepositoryEntryPoint::class.java,
        ).avatarAssetRepository()
    }
    val availability = remember(template, assetRepository) {
        assetRepository.resolveAvailability(template)
    }
    val sourcePath = when (val source = availability.source) {
        is AvatarModelSource.BundledAsset -> source.assetPath
        is AvatarModelSource.DownloadedFile -> source.absolutePath
        is AvatarModelSource.ProceduralFallback -> null
    }
    val modelInstance = sourcePath?.let { rememberModelInstance(modelLoader, it) }
    val palette = template.fallbackPalette
    val backgroundBrush = remember(template) {
        Brush.verticalGradient(
            listOf(
                Color(palette.backdropStartColor),
                Color(palette.backdropEndColor),
            ),
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundBrush),
    ) {
        if (modelInstance != null) {
            SceneView(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
            ) {
                ModelNode(
                    modelInstance = modelInstance,
                    scaleToUnits = scaleToUnits,
                    autoAnimate = false,
                )
            }
        } else {
            ProceduralAvatarPreview(
                template = template,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
