package com.airgf.app.presentation.call

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.airgf.app.llm.LlmEngine
import com.airgf.app.presentation.components.AvatarRenderer
import com.airgf.app.presentation.components.GfAvatar
import com.airgf.app.presentation.components.GlassPanel
import com.airgf.app.presentation.components.GradientBackground
import com.airgf.app.presentation.components.WaveformVisualizer
import com.airgf.app.presentation.theme.Error
import com.airgf.app.presentation.theme.OnSurface
import com.airgf.app.presentation.theme.OnSurfaceVariant
import com.airgf.app.presentation.theme.Primary
import com.airgf.app.presentation.theme.PrimaryContainer
import com.airgf.app.presentation.theme.Secondary

@Composable
fun CallScreen(
    navController: NavController,
    viewModel: CallViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.startCall(hasAudioPermission = true)
        } else {
            viewModel.onPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.startCall(hasAudioPermission = true)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    BackHandler {
        viewModel.endCall()
        navController.popBackStack()
    }

    GradientBackground(showRadialGlow = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CallHeader(uiState = uiState)

            Spacer(modifier = Modifier.height(14.dp))

            CallAvatarStage(
                uiState = uiState,
                avatarAssetRepository = viewModel.avatarAssetRepository,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Spacer(modifier = Modifier.height(14.dp))

            CallStatusPanel(
                uiState = uiState,
                onRetry = viewModel::retryListening,
            )

            Spacer(modifier = Modifier.height(16.dp))

            CallControls(
                uiState = uiState,
                onToggleMute = viewModel::toggleMute,
                onInterrupt = viewModel::interruptReply,
                onToggleSpeaker = viewModel::toggleSpeaker,
                onEnd = {
                    viewModel.endCall()
                    navController.popBackStack()
                },
            )
        }
    }
}

@Composable
private fun CallHeader(uiState: CallUiState) {
    val gfName = uiState.gfProfile?.name ?: "Your GF"
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GfAvatar(
            gfName = gfName,
            visualTemplate = uiState.gfProfile?.visualTemplate,
            prefer3dPreview = true,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = gfName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = OnSurface,
            )
            Text(
                text = "${uiState.phase.label()} - ${formatDuration(uiState.elapsedSeconds)}",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CallAvatarStage(
    uiState: CallUiState,
    avatarAssetRepository: com.airgf.app.data.repository.AvatarAssetRepository,
    modifier: Modifier = Modifier,
) {
    val template = uiState.gfProfile?.visualTemplate
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .border(
                width = if (uiState.phase == CallPhase.Speaking || uiState.phase == CallPhase.Listening) 2.dp else 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        PrimaryContainer.copy(alpha = 0.65f),
                        Secondary.copy(alpha = 0.3f),
                        Color.Transparent,
                    ),
                ),
                shape = RoundedCornerShape(28.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (template != null) {
            AvatarRenderer(
                template = template,
                emotion = uiState.emotion,
                mouthShape = uiState.mouthShape,
                assetRepository = avatarAssetRepository,
                action = uiState.avatarAction,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp)),
            )
        } else {
            Text(
                text = uiState.gfProfile?.name ?: "Amoura",
                style = MaterialTheme.typography.headlineMedium,
                color = Primary,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f)),
                    ),
                ),
        )

        if (uiState.phase == CallPhase.Speaking || uiState.phase == CallPhase.Listening) {
            WaveformVisualizer(
                style = uiState.waveformStyle,
                isActive = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
                    .fillMaxWidth(0.55f)
                    .height(34.dp),
            )
        }

        uiState.gfSpeechText?.let { text ->
            GlassPanel(
                cornerRadius = 20.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 18.dp, end = 18.dp, bottom = 64.dp),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun CallStatusPanel(
    uiState: CallUiState,
    onRetry: () -> Unit,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState.phase == CallPhase.Starting || uiState.llmState is LlmEngine.LlmState.Loading) {
                CircularProgressIndicator(
                    color = Primary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = uiState.statusText(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (uiState.phase == CallPhase.Error) Error else OnSurface,
                textAlign = TextAlign.Center,
            )
            uiState.lastUserTranscript?.let { transcript ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"$transcript\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (uiState.phase == CallPhase.Error && !uiState.permissionDenied) {
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, Primary.copy(alpha = 0.6f), CircleShape),
                ) {
                    Icon(Icons.Outlined.Mic, contentDescription = "Retry listening", tint = Primary)
                }
            }
        }
    }
}

@Composable
private fun CallControls(
    uiState: CallUiState,
    onToggleMute: () -> Unit,
    onInterrupt: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEnd: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CallControlButton(
            onClick = onToggleMute,
            background = if (uiState.micMuted) PrimaryContainer else Color.White.copy(alpha = 0.12f),
        ) {
            Icon(
                imageVector = if (uiState.micMuted) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                contentDescription = if (uiState.micMuted) "Unmute" else "Mute",
                tint = OnSurface,
            )
        }
        CallControlButton(
            onClick = onInterrupt,
            background = Color.White.copy(alpha = 0.12f),
        ) {
            Icon(Icons.Outlined.Stop, contentDescription = "Stop speaking", tint = OnSurface)
        }
        CallControlButton(
            onClick = onEnd,
            size = 68.dp,
            background = Color(0xFFE53935),
        ) {
            Icon(Icons.Filled.CallEnd, contentDescription = "End call", tint = Color.White)
        }
        CallControlButton(
            onClick = onToggleSpeaker,
            background = if (uiState.speakerEnabled) Color.White.copy(alpha = 0.12f) else PrimaryContainer,
        ) {
            Icon(
                imageVector = if (uiState.speakerEnabled) {
                    Icons.AutoMirrored.Outlined.VolumeUp
                } else {
                    Icons.AutoMirrored.Outlined.VolumeOff
                },
                contentDescription = if (uiState.speakerEnabled) "Disable speaker" else "Enable speaker",
                tint = OnSurface,
            )
        }
    }
}

@Composable
private fun CallControlButton(
    onClick: () -> Unit,
    background: Color,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
    ) {
        content()
    }
}

private fun CallPhase.label(): String =
    when (this) {
        CallPhase.Idle -> "Ready"
        CallPhase.Starting -> "Calling"
        CallPhase.Listening -> "Listening"
        CallPhase.Processing -> "Thinking"
        CallPhase.Speaking -> "Speaking"
        CallPhase.Muted -> "Muted"
        CallPhase.Ended -> "Ended"
        CallPhase.Error -> "Needs attention"
    }

private fun CallUiState.statusText(): String =
    error ?: when (phase) {
        CallPhase.Idle -> "Starting call..."
        CallPhase.Starting -> "Connecting to your companion..."
        CallPhase.Listening -> "Listening..."
        CallPhase.Processing -> "Thinking..."
        CallPhase.Speaking -> "Speaking..."
        CallPhase.Muted -> "Microphone muted"
        CallPhase.Ended -> "Call ended"
        CallPhase.Error -> "Something went wrong."
    }

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}
