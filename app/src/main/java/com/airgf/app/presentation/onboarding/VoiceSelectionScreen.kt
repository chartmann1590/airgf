package com.airgf.app.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airgf.app.domain.model.VoiceOption
import com.airgf.app.presentation.components.GlassPanel
import com.airgf.app.presentation.components.GlowButton
import com.airgf.app.presentation.components.GradientBackground
import com.airgf.app.presentation.components.OnboardingHeader
import com.airgf.app.presentation.components.WaveformStyle
import com.airgf.app.presentation.components.WaveformVisualizer
import com.airgf.app.presentation.theme.OnSurface
import com.airgf.app.presentation.theme.OnSurfaceVariant
import com.airgf.app.presentation.theme.OutlineVariant
import com.airgf.app.presentation.theme.Primary
import com.airgf.app.presentation.theme.PrimaryContainer
import com.airgf.app.presentation.theme.SurfaceContainer

private data class VoiceCardInfo(
    val option: VoiceOption,
    val description: String,
    val style: WaveformStyle,
)

private val voiceCards = listOf(
    VoiceCardInfo(VoiceOption.SOFT, "Gentle and warm, like a soft whisper", WaveformStyle.SOFT),
    VoiceCardInfo(VoiceOption.ENERGETIC, "Bright and lively, full of energy", WaveformStyle.ENERGETIC),
    VoiceCardInfo(VoiceOption.MATURE, "Calm and reassuring, with depth", WaveformStyle.MATURE),
    VoiceCardInfo(VoiceOption.BREATHY, "Intimate and close, just for you", WaveformStyle.BREATHY),
)

@Composable
fun VoiceSelectionScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    GradientBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingHeader(
                step = OnboardingStep.VOICE_SELECTION,
                onBack = onBack,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Text(
                    text = "Choose Her Voice",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "How should she sound when she speaks?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(24.dp))
                voiceCards.forEach { card ->
                    VoiceCard(
                        title = card.option.displayName,
                        description = card.description,
                        style = card.style,
                        selected = state.voiceOption == card.option,
                        onSelect = { viewModel.updateVoiceOption(card.option) },
                        onPlay = { viewModel.previewVoice(card.option) },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                GlowButton(
                    text = "Continue",
                    onClick = onContinue,
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
    }
}

@Composable
private fun VoiceCard(
    title: String,
    description: String,
    style: WaveformStyle,
    selected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Primary else OutlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onSelect)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            WaveformVisualizer(
                style = style,
                isActive = selected,
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(PrimaryContainer.copy(alpha = 0.3f))
                .clickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Preview voice",
                tint = Primary,
            )
        }
    }
}
