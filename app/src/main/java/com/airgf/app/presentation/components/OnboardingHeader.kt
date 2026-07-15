package com.airgf.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.airgf.app.presentation.onboarding.OnboardingStep
import com.airgf.app.presentation.theme.OnSurfaceVariant
import com.airgf.app.presentation.theme.Primary
import com.airgf.app.presentation.theme.SurfaceVariant

@Composable
fun OnboardingHeader(
    step: OnboardingStep,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showStepLabel: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                Box(modifier = Modifier.size(48.dp))
            }
            if (showStepLabel) {
                Text(
                    text = "Step ${step.index + 1} of ${OnboardingStep.entries.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
            } else {
                Box(modifier = Modifier.size(48.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OnboardingStep.entries.forEach { s ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (s.index <= step.index) Primary else SurfaceVariant,
                        ),
                )
            }
        }

    }
}
