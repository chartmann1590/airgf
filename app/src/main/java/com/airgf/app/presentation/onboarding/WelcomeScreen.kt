package com.airgf.app.presentation.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airgf.app.R
import com.airgf.app.presentation.components.FloatingParticles
import com.airgf.app.presentation.components.GlowButton
import com.airgf.app.presentation.components.GradientBackground
import com.airgf.app.presentation.theme.OnSurfaceVariant
import com.airgf.app.presentation.theme.Primary
import com.airgf.app.presentation.theme.PurpleShadow

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onContinueExisting: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    GradientBackground(modifier = modifier) {
        FloatingParticles()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "Amoura",
                    modifier = Modifier
                        .size(160.dp)
                        .shadow(24.dp, RoundedCornerShape(32.dp), ambientColor = PurpleShadow, spotColor = PurpleShadow)
                        .clip(RoundedCornerShape(32.dp)),
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Amoura",
                style = MaterialTheme.typography.displaySmall,
                color = Primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your Perfect Companion",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.weight(1f))
            GlowButton(
                text = "Get Started",
                onClick = onGetStarted,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (onContinueExisting != null) {
                Text(
                    text = "Already have a companion? Continue",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable(onClick = onContinueExisting)
                        .padding(bottom = 32.dp),
                )
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
