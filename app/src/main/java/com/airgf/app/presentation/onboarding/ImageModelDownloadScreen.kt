package com.airgf.app.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airgf.app.core.util.FileUtil
import com.airgf.app.imagegen.ImageGenConstants
import com.airgf.app.presentation.components.DownloadProgressRing
import com.airgf.app.presentation.components.FloatingParticles
import com.airgf.app.presentation.components.GlassPanel
import com.airgf.app.presentation.components.GlowButton
import com.airgf.app.presentation.components.GradientBackground
import com.airgf.app.presentation.components.OnboardingHeader
import com.airgf.app.presentation.theme.OnSurface
import com.airgf.app.presentation.theme.OnSurfaceVariant
import com.airgf.app.presentation.theme.Outline
import com.airgf.app.presentation.theme.Primary

@Composable
fun ImageModelDownloadScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageModelDownloadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.startDownload()
    }

    GradientBackground(modifier = modifier) {
        FloatingParticles()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OnboardingHeader(
                step = OnboardingStep.IMAGE_MODEL_DOWNLOAD,
                onBack = null,
                showStepLabel = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (state.phase) {
                    DownloadPhase.Complete -> "Ready"
                    DownloadPhase.Error -> "Download Failed"
                    else -> "Image Generation"
                },
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = OnSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            DownloadProgressRing(
                percent = state.percent,
                pulseWhenComplete = state.phase == DownloadPhase.Complete,
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (state.phase) {
                DownloadPhase.Error -> {
                    ImageErrorContent(
                        errorType = state.errorType,
                        errorMessage = state.errorMessage,
                        onRetry = viewModel::retry,
                        onSkip = onContinue,
                    )
                }
                DownloadPhase.Complete -> {
                    ImageCompleteContent(onContinue = onContinue)
                }
                else -> {
                    ImageDownloadingContent(
                        bytesDownloaded = state.bytesDownloaded,
                        totalBytes = state.totalBytes,
                        speedBytesPerSec = state.speedBytesPerSec,
                        etaSeconds = state.etaSeconds,
                        tip = viewModel.currentTip(),
                        onSkip = onContinue,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageDownloadingContent(
    bytesDownloaded: Long,
    totalBytes: Long,
    speedBytesPerSec: Long,
    etaSeconds: Long?,
    tip: String,
    onSkip: () -> Unit,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Downloading ${ImageGenConstants.MODEL_DISPLAY_NAME}...",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = OnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            RowWithIcon(
                icon = { Icon(Icons.Default.Download, contentDescription = null, tint = Primary) },
                text = buildString {
                    append(FileUtil.formatFileSize(bytesDownloaded))
                    append(" / ")
                    append(FileUtil.formatFileSize(totalBytes))
                    if (speedBytesPerSec > 0) {
                        append(" - ")
                        append(FileUtil.formatFileSize(speedBytesPerSec))
                        append("/s")
                    }
                },
            )
            if (etaSeconds != null && etaSeconds > 0 && speedBytesPerSec > 0) {
                RowWithIcon(
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = Outline) },
                    text = formatImageEta(etaSeconds),
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedButton(
        onClick = onSkip,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "Skip for now")
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = tip,
        style = MaterialTheme.typography.bodySmall,
        color = OnSurfaceVariant,
        fontStyle = FontStyle.Italic,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    )
}

@Composable
private fun ImageCompleteContent(onContinue: () -> Unit) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${ImageGenConstants.MODEL_DISPLAY_NAME} is ready",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = OnSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "She can now create and share images with you.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    GlowButton(
        text = "Continue",
        onClick = onContinue,
        icon = Icons.AutoMirrored.Filled.ArrowForward,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ImageErrorContent(
    errorType: DownloadErrorType?,
    errorMessage: String?,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
) {
    val message = when (errorType) {
        DownloadErrorType.NoNetwork -> "No internet connection. Connect to download the image model."
        DownloadErrorType.InsufficientStorage -> "Not enough storage. At least 2 GB of free space is required."
        DownloadErrorType.DownloadFailed -> errorMessage ?: "Download failed. Please try again."
        null -> "Something went wrong. Please try again."
    }

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(20.dp),
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    GlowButton(
        text = "Retry",
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
        onClick = onSkip,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "Skip for now")
    }
}

@Composable
private fun RowWithIcon(
    icon: @Composable () -> Unit,
    text: String,
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
        )
    }
}

private fun formatImageEta(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return when {
        minutes > 0 -> "~$minutes min ${secs}s remaining"
        else -> "~$secs seconds remaining"
    }
}
