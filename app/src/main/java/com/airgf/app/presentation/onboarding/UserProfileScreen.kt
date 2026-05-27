package com.airgf.app.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airgf.app.domain.model.CommunicationStyle
import com.airgf.app.presentation.components.GlassPanel
import com.airgf.app.presentation.components.GlowButton
import com.airgf.app.presentation.components.GradientBackground
import com.airgf.app.presentation.components.OnboardingHeader
import com.airgf.app.presentation.components.PillTextField
import com.airgf.app.presentation.components.SelectableChip
import com.airgf.app.presentation.components.SelectionCard
import com.airgf.app.presentation.theme.OnSurfaceVariant
import com.airgf.app.presentation.theme.Primary

private val interestIcons: Map<String, ImageVector> = mapOf(
    "Gaming" to Icons.Default.SportsEsports,
    "Music" to Icons.Default.Headphones,
    "Movies" to Icons.Default.Movie,
    "Reading" to Icons.Default.MenuBook,
    "Fitness" to Icons.Default.FitnessCenter,
    "Art" to Icons.Default.Palette,
    "Travel" to Icons.Default.Flight,
    "Food" to Icons.Default.Restaurant,
    "Technology" to Icons.Default.Devices,
    "Nature" to Icons.Default.Park,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val canProceed = state.userName.isNotBlank() &&
        (state.userAge.toIntOrNull() ?: 0) >= 18 &&
        state.interests.size >= 3

    GradientBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingHeader(
                step = OnboardingStep.USER_PROFILE,
                onBack = onBack,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Text(
                    text = "Tell me about yourself",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "So I can get to know you better",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Name",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                        )
                        PillTextField(
                            value = state.userName,
                            onValueChange = viewModel::updateUserName,
                            placeholder = "What should I call you?",
                            leadingIcon = Icons.Default.Favorite,
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.width(96.dp)) {
                        Text(
                            text = "Age",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                        )
                        PillTextField(
                            value = state.userAge,
                            onValueChange = viewModel::updateUserAge,
                            placeholder = "18+",
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Interests",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                    )
                    Text(
                        text = "Pick 3 or more",
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary.copy(alpha = 0.7f),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    INTEREST_OPTIONS.forEach { interest ->
                        SelectableChip(
                            label = interest,
                            selected = state.interests.contains(interest),
                            onClick = { viewModel.toggleInterest(interest) },
                            icon = interestIcons[interest],
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Communication style",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
                )
                SelectionCard(
                    title = CommunicationStyle.CASUAL.displayName,
                    subtitle = "Lighthearted chats, everyday updates.",
                    selected = state.communicationStyle == CommunicationStyle.CASUAL,
                    onClick = { viewModel.updateCommunicationStyle(CommunicationStyle.CASUAL) },
                    icon = Icons.Default.Favorite,
                )
                Spacer(modifier = Modifier.height(12.dp))
                SelectionCard(
                    title = CommunicationStyle.DEEP.displayName,
                    subtitle = "Philosophical talks, emotional connection.",
                    selected = state.communicationStyle == CommunicationStyle.DEEP,
                    onClick = { viewModel.updateCommunicationStyle(CommunicationStyle.DEEP) },
                    icon = Icons.Default.AutoAwesome,
                )
                Spacer(modifier = Modifier.height(12.dp))
                SelectionCard(
                    title = CommunicationStyle.FUNNY.displayName,
                    subtitle = "Banter, jokes, and playful teasing.",
                    selected = state.communicationStyle == CommunicationStyle.FUNNY,
                    onClick = { viewModel.updateCommunicationStyle(CommunicationStyle.FUNNY) },
                    icon = Icons.Default.SentimentVerySatisfied,
                )
                Spacer(modifier = Modifier.height(100.dp))
            }
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (!canProceed) {
                        val missing = buildList {
                            if (state.userName.isBlank()) add("name")
                            if ((state.userAge.toIntOrNull() ?: 0) < 18) add("age (18+)")
                            if (state.interests.size < 3) add("${3 - state.interests.size} more interest${if (3 - state.interests.size != 1) "s" else ""}")
                        }
                        Text(
                            text = "Still need: ${missing.joinToString(", ")}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    GlowButton(
                        text = "Continue",
                        onClick = onContinue,
                        enabled = canProceed,
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                    )
                }
            }
        }
    }
}
