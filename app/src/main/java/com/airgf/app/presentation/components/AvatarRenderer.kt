package com.airgf.app.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.airgf.app.animation.AvatarExpressionDriver
import com.airgf.app.animation.AvatarFacePose
import com.airgf.app.animation.BlendShapeLipSync
import com.airgf.app.data.model.AvatarModelSource
import com.airgf.app.data.model.DownloadState
import com.airgf.app.data.repository.AvatarAssetRepository
import com.airgf.app.domain.model.EmotionState
import com.airgf.app.domain.model.VisualTemplate
import com.airgf.app.presentation.theme.OnSurface
import com.airgf.app.presentation.theme.OnSurfaceVariant
import com.airgf.app.presentation.theme.Primary
import com.airgf.app.tts.LipSyncBridge
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberModelInstance
import kotlinx.coroutines.launch

@Composable
fun AvatarRenderer(
    template: VisualTemplate,
    emotion: EmotionState,
    mouthShape: LipSyncBridge.MouthShape,
    assetRepository: AvatarAssetRepository,
    modifier: Modifier = Modifier,
    environmentAssetPath: String? = null,
) {
    val scope = rememberCoroutineScope()
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val lipSync = remember { BlendShapeLipSync() }
    val expressionDriver = remember { AvatarExpressionDriver() }
    val facePose = remember(template, emotion, mouthShape) {
        expressionDriver.poseFor(emotion) + lipSync.poseFor(mouthShape)
    }

    var downloadState by remember(template) { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var availabilityVersion by remember(template) { mutableIntStateOf(0) }
    val availability = remember(template, availabilityVersion) {
        assetRepository.resolveAvailability(template)
    }
    val sourcePath = when (val source = availability.source) {
        is AvatarModelSource.BundledAsset -> source.assetPath
        is AvatarModelSource.DownloadedFile -> source.absolutePath
        is AvatarModelSource.ProceduralFallback -> null
    }
    val modelInstance = sourcePath?.let { rememberModelInstance(modelLoader, it) }
    val envModelInstance = if (environmentAssetPath != null) {
        rememberModelInstance(modelLoader, environmentAssetPath)
    } else {
        null
    }

    val idleTransition = rememberInfiniteTransition(label = "avatarIdle")
    val idleLift by idleTransition.animateFloat(
        initialValue = -0.02f,
        targetValue = 0.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "idleLift",
    )

    val backdrop = template.fallbackPalette

    val bgBrush = if (envModelInstance != null) {
        Brush.verticalGradient(listOf(Color.Black, Color.Black))
    } else {
        Brush.verticalGradient(
            listOf(
                Color(backdrop.backdropStartColor),
                Color(backdrop.backdropEndColor),
            ),
        )
    }

    Box(
        modifier = modifier
            .background(bgBrush)
            .clip(RoundedCornerShape(24.dp)),
    ) {
        if (modelInstance != null) {
            SceneView(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
            ) {
                if (envModelInstance != null) {
                    ModelNode(
                        modelInstance = envModelInstance,
                        scaleToUnits = 8f,
                        centerOrigin = Position(0f, 0f, 0f),
                    )
                }
                ModelNode(
                    modelInstance = modelInstance,
                    scaleToUnits = if (envModelInstance != null) 1.9f else 2.2f,
                    centerOrigin = if (envModelInstance != null) {
                        Position(0f, 0f, 1.5f)
                    } else {
                        null
                    },
                    autoAnimate = true,
                )
            }
        } else {
            FallbackAvatarStage(
                template = template,
                facePose = facePose,
                idleLift = idleLift,
            )
        }

        AvatarStatusOverlay(
            template = template,
            downloadState = downloadState,
            statusText = availability.statusMessage,
            showDownloadAction = availability.canDownload && downloadState !is DownloadState.Progress,
            onDownload = {
                scope.launch {
                    assetRepository.downloadModel(template).collect { state ->
                        downloadState = state
                        if (state is DownloadState.Complete) {
                            availabilityVersion++
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun FallbackAvatarStage(
    template: VisualTemplate,
    facePose: AvatarFacePose,
    idleLift: Float,
) {
    val tilt by animateFloatAsState(
        targetValue = facePose.headTiltDegrees,
        animationSpec = tween(350),
        label = "fallbackTilt",
    )
    val scale by animateFloatAsState(
        targetValue = 1f + (facePose.jawOpen * 0.03f) + (facePose.speakingBob * 0.02f),
        animationSpec = tween(120),
        label = "fallbackScale",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = 0.2f + (facePose.mouthSmile.coerceAtLeast(0f) * 0.2f) + (facePose.eyeWide * 0.1f),
        animationSpec = tween(250),
        label = "fallbackGlow",
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .scale(1f + facePose.speakingBob * 0.08f)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(template.fallbackPalette.accentColor).copy(alpha = glowAlpha)),
        )
        ProceduralAvatarPreview(
            template = template,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .graphicsLayer {
                    rotationZ = tilt
                    translationY = idleLift * 120f
                }
                .scale(scale)
                .clip(RoundedCornerShape(28.dp)),
        )
    }
}

@Composable
private fun BoxScope.AvatarStatusOverlay(
    template: VisualTemplate,
    downloadState: DownloadState,
    statusText: String?,
    showDownloadAction: Boolean,
    onDownload: () -> Unit,
) {
    val message = when (downloadState) {
        DownloadState.Idle -> statusText
        is DownloadState.Progress -> "Downloading ${template.displayName} ${(downloadState.percent * 100).toInt()}%"
        is DownloadState.Complete -> "Downloaded ${template.displayName}. The full-detail avatar is ready."
        is DownloadState.Error -> downloadState.message
    } ?: return

    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
        color = Color.Black.copy(alpha = 0.48f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurface,
            )
            when (downloadState) {
                is DownloadState.Progress -> {
                    CircularProgressIndicator(
                        progress = { downloadState.percent },
                        color = Primary,
                    )
                }

                is DownloadState.Error, DownloadState.Idle -> {
                    if (showDownloadAction) {
                        Button(onClick = onDownload) {
                            Text(text = "Download Full Avatar")
                        }
                    } else {
                        Text(
                            text = "Rendering the built-in 3D fallback until a GLB is available.",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant,
                        )
                    }
                }

                is DownloadState.Complete -> {
                    Text(
                        text = "Re-open this screen anytime to view the downloaded model.",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                    )
                }
            }
        }
    }
}
