package com.airgf.app.presentation.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airgf.app.presentation.components.AvatarModelPreview
import com.airgf.app.presentation.components.FloatingParticles
import com.airgf.app.presentation.components.GlowButton
import com.airgf.app.presentation.components.GradientBackground
import com.airgf.app.notification.hasNotificationPermission
import com.airgf.app.presentation.theme.InverseSurface
import com.airgf.app.presentation.theme.OnSurfaceVariant
import com.airgf.app.presentation.theme.OutlineVariant
import com.airgf.app.presentation.theme.Primary
import com.airgf.app.presentation.theme.PrimaryContainer
import com.airgf.app.presentation.theme.PurpleShadow
import com.airgf.app.presentation.theme.TertiaryContainer

@Composable
fun SetupCompleteScreen(
    viewModel: OnboardingViewModel,
    onStartChatting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )
    val requestNotifications = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.saveAndComplete(
            enableProactiveMessages = granted,
            onSuccess = onStartChatting,
        )
    }

    GradientBackground(modifier = modifier, showRadialGlow = false) {
        FloatingParticles(particleCount = 25)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PrimaryContainer.copy(alpha = glowAlpha * 0.25f),
                            Color.Transparent,
                        ),
                        radius = 600f,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(0.3f))
            Box(
                modifier = Modifier
                    .size(width = 240.dp, height = 300.dp)
                    .shadow(24.dp, RoundedCornerShape(32.dp), ambientColor = PurpleShadow, spotColor = PurpleShadow)
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                OnSurfaceVariant.copy(alpha = 0.5f),
                                Color.Transparent,
                            ),
                        ),
                        shape = RoundedCornerShape(28.dp),
                    ),
            ) {
                AvatarModelPreview(
                    template = state.visualTemplate,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = state.gfName.ifBlank { "Your Companion" },
                style = MaterialTheme.typography.displaySmall,
                color = Primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x1A181021))
                    .border(
                        width = 1.dp,
                        color = OutlineVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .padding(24.dp),
            ) {
                Text(
                    text = "Hey ${state.userName.ifBlank { "there" }}! I've been waiting to meet you... 💕",
                    style = MaterialTheme.typography.bodyLarge,
                    color = InverseSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            GlowButton(
                text = if (state.isSaving) "Setting up..." else "Start Chatting",
                onClick = {
                    if (!state.isSaving) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !context.hasNotificationPermission()
                        ) {
                            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.saveAndComplete(
                                enableProactiveMessages = true,
                                onSuccess = onStartChatting,
                            )
                        }
                    }
                },
                enabled = !state.isSaving,
                icon = Icons.AutoMirrored.Filled.Send,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "If you skip notifications now, you can turn proactive messages on later in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            if (state.saveError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.saveError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = TertiaryContainer,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
