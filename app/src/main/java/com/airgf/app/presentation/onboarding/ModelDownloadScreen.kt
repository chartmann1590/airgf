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
import com.airgf.app.llm.ModelConstants
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
fun ModelDownloadScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModelDownloadViewModel = hiltViewModel(),
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
                step = OnboardingStep.MODEL_DOWNLOAD,
                onBack = null,
                showStepLabel = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (state.phase) {
                    DownloadPhase.Complete -> "Ready"
                    DownloadPhase.Error -> "Download Failed"
                    else -> "Initializing"
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
                    ErrorContent(
                        errorType = state.errorType,
                        errorMessage = state.errorMessage,
                        onRetry = viewModel::retry,
                    )
                }
                DownloadPhase.Complete -> {
                    CompleteContent(onContinue = onContinue)
                }
                else -> {
                    DownloadingContent(
                        bytesDownloaded = state.bytesDownloaded,
                        totalBytes = state.totalBytes,
                        speedBytesPerSec = state.speedBytesPerSec,
                        etaSeconds = state.etaSeconds,
                        tip = viewModel.currentTip(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadingContent(
    bytesDownloaded: Long,
    totalBytes: Long,
    speedBytesPerSec: Long,
    etaSeconds: Long?,
    tip: String,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Downloading ${ModelConstants.MODEL_DISPLAY_NAME}…",
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
                        append(" • ")
                        append(FileUtil.formatFileSize(speedBytesPerSec))
                        append("/s")
                    }
                },
            )
            if (etaSeconds != null && etaSeconds > 0 && speedBytesPerSec > 0) {
                RowWithIcon(
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = Outline) },
                    text = formatEta(etaSeconds),
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

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
private fun CompleteContent(onContinue: () -> Unit) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${ModelConstants.MODEL_DISPLAY_NAME} is ready",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = OnSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Her mind is loaded on your device. No cloud needed.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    GlowButton(
        text = "Meet Her",
        onClick = onContinue,
        icon = Icons.AutoMirrored.Filled.ArrowForward,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorContent(
    errorType: DownloadErrorType?,
    errorMessage: String?,
    onRetry: () -> Unit,
) {
    val message = when (errorType) {
        DownloadErrorType.NoNetwork ->
            "No internet connection. Connect to Wi‑Fi or mobile data to download the AI model."
        DownloadErrorType.InsufficientStorage ->
            "Not enough storage. At least 3 GB of free space is required."
        DownloadErrorType.DownloadFailed ->
            errorMessage ?: "Download failed. Please try again."
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

    Spacer(modifier = Modifier.height(32.dp))

    GlowButton(
        text = "Retry",
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth(),
    )
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

private fun formatEta(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return when {
        minutes > 0 -> "~$minutes min ${secs}s remaining"
        else -> "~$secs seconds remaining"
    }
}
