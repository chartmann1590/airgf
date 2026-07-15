package com.airgf.app.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airgf.app.domain.model.PersonalityTrait
import com.airgf.app.domain.model.RelationshipType
import com.airgf.app.domain.model.VisualTemplate
import com.airgf.app.domain.model.CompanionPresentation
import com.airgf.app.presentation.components.AvatarModelPreview
import com.airgf.app.presentation.components.GlassPanel
import com.airgf.app.presentation.components.GlowButton
import com.airgf.app.presentation.components.GradientBackground
import com.airgf.app.presentation.components.OnboardingHeader
import com.airgf.app.presentation.components.PillTextField
import com.airgf.app.presentation.components.SelectableChip
import com.airgf.app.presentation.components.SelectionCard
import com.airgf.app.presentation.components.TemplateCard
import com.airgf.app.presentation.theme.OnSurfaceVariant

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GfCustomizationScreen(
    viewModel: OnboardingViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val canProceed = state.gfName.isNotBlank() &&
        state.personalityTraits.isNotEmpty() &&
        state.personalityTraits.size <= 3

    GradientBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingHeader(
                step = OnboardingStep.GF_CUSTOMIZATION,
                onBack = onBack,
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 100.dp),
            ) {
                item {
                    Text(
                        text = "Create Your Companion",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose their role, look, personality, and style",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "COMPANION ROLE",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CompanionPresentation.entries.forEach { presentation ->
                            SelectableChip(
                                label = presentation.relationshipNoun.replaceFirstChar { it.uppercase() },
                                selected = state.companionPresentation == presentation,
                                onClick = { viewModel.updateCompanionPresentation(presentation) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "COMPANION NAME",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                    )
                    PillTextField(
                        value = state.gfName,
                        onValueChange = viewModel::updateGfName,
                        placeholder = "e.g. Luna, Sakura, Chloe...",
                        leadingIcon = Icons.Default.AutoAwesome,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Text(
                        text = "3D AVATARS",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
                    )
                    AvatarModelPreview(
                        template = state.visualTemplate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                    )
                    Text(
                        text = state.visualTemplate.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 16.dp),
                    )
                }
                item {
                    val templates = VisualTemplate.entries.filter { it.supports(state.companionPresentation) }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        templates.chunked(2).forEach { rowTemplates ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                rowTemplates.forEach { template ->
                                    TemplateCard(
                                        template = template,
                                        selected = state.visualTemplate == template,
                                        onClick = { viewModel.updateVisualTemplate(template) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (rowTemplates.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Text(
                        text = "CORE TRAITS (PICK UP TO 3)",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PersonalityTrait.entries.forEach { trait ->
                            val selected = state.personalityTraits.contains(trait)
                            val atMax = state.personalityTraits.size >= 3 && !selected
                            SelectableChip(
                                label = trait.displayName,
                                selected = selected,
                                onClick = { viewModel.togglePersonalityTrait(trait) },
                                enabled = !atMax,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "RELATIONSHIP DYNAMIC",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
                    )
                }
                items(RelationshipType.entries.toList()) { type ->
                    val icon = when (type) {
                        RelationshipType.CASUAL -> Icons.Default.Coffee
                        RelationshipType.ROMANTIC -> Icons.Default.Favorite
                        RelationshipType.BESTFRIEND -> Icons.Default.Handshake
                        RelationshipType.PASSIONATE -> Icons.Default.LocalFireDepartment
                    }
                    SelectionCard(
                        title = type.displayName,
                        subtitle = type.description,
                        selected = state.relationshipType == type,
                        onClick = { viewModel.updateRelationshipType(type) },
                        icon = icon,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
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
                    enabled = canProceed,
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
    }
}
