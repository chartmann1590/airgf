package com.airgf.app.presentation.character

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.airgf.app.core.navigation.Route
import com.airgf.app.presentation.components.AvatarRenderer
import com.airgf.app.presentation.components.GlassPanel
import com.airgf.app.presentation.components.GradientBackground
import com.airgf.app.presentation.components.MainBottomNav
import com.airgf.app.presentation.components.WaveformVisualizer
import com.airgf.app.presentation.theme.OnSurface
import com.airgf.app.presentation.theme.OnSurfaceVariant
import com.airgf.app.presentation.theme.Primary
import com.airgf.app.presentation.theme.PrimaryContainer
import com.airgf.app.presentation.theme.Secondary

@Composable
fun CharacterScreen(
    navController: NavController,
    viewModel: CharacterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val template = uiState.gfProfile?.visualTemplate ?: return
    val snackbarHostState = remember { SnackbarHostState() }

    val hintTransition = rememberInfiniteTransition(label = "hintPulse")
    val hintAlpha by hintTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hintAlpha",
    )

    GradientBackground(showRadialGlow = true) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                MainBottomNav(
                    currentRoute = Route.Character,
                    onNavigate = { route ->
                        if (route != Route.Character) {
                            navController.navigate(route.path) { launchSingleTop = true }
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = uiState.gfProfile?.name ?: "Your GF",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )

                // Main character display area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            width = if (uiState.isSpeaking) 2.dp else 1.dp,
                            brush = Brush.verticalGradient(
                                if (uiState.isSpeaking) {
                                    listOf(PrimaryContainer.copy(alpha = 0.8f), Primary.copy(alpha = 0.4f))
                                } else {
                                    listOf(OnSurfaceVariant.copy(alpha = 0.3f), Color.Transparent)
                                },
                            ),
                            shape = RoundedCornerShape(24.dp),
                        )
                        .clickable { viewModel.tapToTalk() },
                    contentAlignment = Alignment.Center,
                ) {
                    AvatarRenderer(
                        template = template,
                        emotion = uiState.emotion,
                        mouthShape = uiState.mouthShape,
                        assetRepository = viewModel.avatarAssetRepository,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp)),
                    )

                    // Bottom gradient for text readability
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                ),
                            ),
                    )

                    // Waveform when speaking
                    if (uiState.isSpeaking) {
                        WaveformVisualizer(
                            style = uiState.waveformStyle,
                            isActive = true,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .fillMaxWidth(0.5f)
                                .height(32.dp),
                        )
                    }

                    // Speech bubble
                    uiState.speechBubbleText?.let { text ->
                        GlassPanel(
                            cornerRadius = 20.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(
                                    start = 20.dp,
                                    end = 20.dp,
                                    bottom = if (uiState.isSpeaking) 56.dp else 20.dp,
                                ),
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            )
                        }
                    }

                }

                Spacer(modifier = Modifier.height(12.dp))

                // Emotion indicator
                GlassPanel(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    cornerRadius = 24.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = uiState.emotion.emoji, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Feeling ${uiState.emotion.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurface,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                ) {
                    QuickActionButton(
                        icon = { Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "Chat", tint = Secondary) },
                        onClick = { navController.navigate(Route.Chat.path) },
                    )
                    QuickActionButton(
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = "Heart", tint = PrimaryContainer) },
                        onClick = { viewModel.sendQuickReaction("I love you!") },
                    )
                    QuickActionButton(
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Outlined.VolumeUp,
                                contentDescription = "Talk",
                                tint = if (uiState.isSpeaking) PrimaryContainer else Primary,
                            )
                        },
                        onClick = {
                            if (uiState.isSpeaking) viewModel.stopSpeaking() else viewModel.tapToTalk()
                        },
                    )
                    QuickActionButton(
                        icon = {
                            Icon(
                                Icons.Filled.LocalFireDepartment,
                                contentDescription = "Spicy",
                                tint = if (uiState.gfProfile?.spicyModeEnabled == true) PrimaryContainer else Color(0xFFFF6B6B),
                            )
                        },
                        onClick = viewModel::toggleSpicyMode,
                    )
                }

                // Hint text
                Text(
                    text = if (uiState.isSpeaking) "Tap to stop" else "Tap to talk",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing * 2,
                    ),
                    color = OnSurfaceVariant.copy(alpha = hintAlpha),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFF9C27B0).copy(alpha = 0.2f))
            .border(1.dp, Color(0xFF9C27B0), CircleShape),
    ) {
        icon()
    }
}
