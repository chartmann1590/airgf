package com.airgf.app.presentation.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.airgf.app.core.navigation.Route
import com.airgf.app.domain.model.PersonalityTrait
import com.airgf.app.domain.model.VisualTemplate
import com.airgf.app.domain.model.VoiceOption
import com.airgf.app.notification.hasNotificationPermission
import com.airgf.app.presentation.components.AvatarModelPreview
import com.airgf.app.presentation.components.GlassPanel
import com.airgf.app.presentation.components.GradientBackground
import com.airgf.app.presentation.components.MainBottomNav
import com.airgf.app.presentation.components.PillTextField
import com.airgf.app.presentation.components.SelectableChip
import com.airgf.app.presentation.components.SelectionCard
import com.airgf.app.presentation.components.TemplateCard
import com.airgf.app.presentation.theme.Error
import com.airgf.app.presentation.theme.OnError
import com.airgf.app.presentation.theme.OnSurface
import com.airgf.app.presentation.theme.OnSurfaceVariant
import com.airgf.app.presentation.theme.OutlineVariant
import com.airgf.app.presentation.theme.Primary
import com.airgf.app.presentation.theme.SurfaceContainerHigh
import kotlinx.coroutines.launch

private enum class SettingsEditor {
    GfName,
    Appearance,
    Personality,
    CustomNotes,
    UserInfo,
    Interests,
    Voice,
    ModelStatus,
    ImageModelStatus,
    ResetConfirmation,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeEditor by remember { mutableStateOf<SettingsEditor?>(null) }
    val notificationsGranted = context.hasNotificationPermission()
    val requestNotificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.setProactiveMessagesEnabled(true)
            scope.launch { snackbarHostState.showSnackbar("Proactive messages enabled.") }
        } else {
            viewModel.setProactiveMessagesEnabled(false)
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Notifications stayed off. You can enable them later from Settings.",
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is SettingsEvent.ShareExport -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Export chat history"))
                }
                SettingsEvent.NavigateToOnboarding -> {
                    navController.navigate(Route.Welcome.path) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    GradientBackground(showRadialGlow = true) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                SettingsTopBar()
            },
            bottomBar = {
                MainBottomNav(
                    currentRoute = Route.Settings,
                    onNavigate = { route ->
                        if (route != Route.Settings) {
                            navController.navigate(route.path) {
                                launchSingleTop = true
                            }
                        }
                    },
                )
            },
        ) { paddingValues ->
            if (uiState.isLoading) {
                SettingsLoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        SettingsSection(
                            title = "Her Profile",
                            icon = Icons.Default.Favorite,
                        ) {
                            SettingsRow(
                                title = "Name",
                                value = uiState.gfProfile?.name.orEmpty(),
                                onClick = { activeEditor = SettingsEditor.GfName },
                            )
                            SettingsRow(
                                title = "Appearance",
                                value = uiState.gfProfile?.visualTemplate?.displayName.orEmpty(),
                                onClick = { activeEditor = SettingsEditor.Appearance },
                            )
                            SettingsChipSummaryRow(
                                title = "Personality Traits",
                                chips = uiState.gfProfile?.personalityTraits?.map { it.displayName }.orEmpty(),
                                actionLabel = "Edit",
                                onActionClick = { activeEditor = SettingsEditor.Personality },
                            )
                            SettingsRow(
                                title = "Custom Notes",
                                subtitle = uiState.gfProfile?.customPromptAdditions ?: "Add extra notes for her personality and vibe.",
                                onClick = { activeEditor = SettingsEditor.CustomNotes },
                            )
                        }
                    }

                    item {
                        SettingsSection(
                            title = "Your Profile",
                            icon = Icons.Default.Person,
                        ) {
                            val userProfile = uiState.userProfile
                            SettingsRow(
                                title = "Personal Info",
                                subtitle = listOfNotNull(
                                    userProfile?.name,
                                    userProfile?.age?.let { "Age $it" },
                                ).joinToString(" · "),
                                onClick = { activeEditor = SettingsEditor.UserInfo },
                            )
                            SettingsChipSummaryRow(
                                title = "Your Interests",
                                chips = uiState.userProfile?.interests.orEmpty(),
                                actionLabel = "Add",
                                onActionClick = { activeEditor = SettingsEditor.Interests },
                            )
                        }
                    }

                    item {
                        SettingsSection(
                            title = "Modes & Behavior",
                            icon = Icons.Default.AutoAwesome,
                        ) {
                            SettingsToggleRow(
                                title = "Spicy Mode",
                                subtitle = "Allow more intimate interactions.",
                                checked = uiState.gfProfile?.spicyModeEnabled == true,
                                onCheckedChange = viewModel::setSpicyMode,
                            )
                        }
                    }

                    item {
                        SettingsSection(
                            title = "Voice & Speech",
                            icon = Icons.AutoMirrored.Outlined.VolumeUp,
                        ) {
                            SettingsToggleRow(
                                title = "Text to Speech",
                                subtitle = "Speak her replies out loud after each message.",
                                checked = uiState.ttsEnabled,
                                onCheckedChange = viewModel::setTtsEnabled,
                            )
                            SettingsRow(
                                title = "Voice Selection",
                                value = uiState.gfProfile?.voiceOption?.displayName.orEmpty(),
                                onClick = { activeEditor = SettingsEditor.Voice },
                            )
                            SpeechSpeedRow(
                                speed = uiState.speechSpeed,
                                onSpeedChange = viewModel::setSpeechSpeed,
                            )
                        }
                    }

                    item {
                        SettingsSection(
                            title = "Notifications",
                            icon = Icons.Default.Notifications,
                        ) {
                            SettingsToggleRow(
                                title = "Proactive Messages",
                                subtitle = if (!notificationsGranted &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                ) {
                                    "Let her check in on you during the day. Android will ask for notification permission first."
                                } else {
                                    "Let her check in on you during the day."
                                },
                                checked = uiState.proactiveMessagesEnabled,
                                onCheckedChange = { enabled ->
                                    when {
                                        !enabled -> viewModel.setProactiveMessagesEnabled(false)
                                        notificationsGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> {
                                            viewModel.setProactiveMessagesEnabled(true)
                                        }
                                        else -> requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                            )
                            NotificationFrequencyRow(
                                selected = uiState.notificationFrequency,
                                onSelected = viewModel::setNotificationFrequency,
                                enabled = uiState.proactiveMessagesEnabled,
                            )
                        }
                    }

                    item {
                        SettingsSection(
                            title = "Data",
                            icon = Icons.Default.Storage,
                        ) {
                            SettingsRow(
                                title = "Export Chat History",
                                subtitle = if (uiState.isExporting) {
                                    "Preparing your chat archive..."
                                } else {
                                    "Create a JSON archive of every saved message."
                                },
                                onClick = viewModel::exportChatHistory,
                                enabled = !uiState.isExporting && !uiState.isResetting,
                            )
                            SettingsRow(
                                title = "Reset Everything",
                                subtitle = "Clear profiles, messages, and preferences, then return to onboarding.",
                                onClick = { activeEditor = SettingsEditor.ResetConfirmation },
                                trailingIcon = Icons.Default.WarningAmber,
                                destructive = true,
                                enabled = !uiState.isResetting && !uiState.isExporting && !uiState.isDownloadingModel,
                            )
                        }
                    }

                    item {
                        SettingsSection(
                            title = "Model",
                            icon = Icons.Default.Psychology,
                        ) {
                            SettingsRow(
                                title = "Model Status",
                                subtitle = if (uiState.isDownloadingModel) {
                                    "Re-downloading ${com.airgf.app.llm.ModelConstants.MODEL_DISPLAY_NAME}..."
                                } else {
                                    modelStatusText(uiState.modelStatus)
                                },
                                onClick = { activeEditor = SettingsEditor.ModelStatus },
                            )
                            SettingsRow(
                                title = "Re-download Model",
                                subtitle = "Download the bundled AI model again.",
                                onClick = viewModel::redownloadModel,
                                trailingIcon = Icons.Default.Download,
                                enabled = !uiState.isDownloadingModel && !uiState.isDeletingModel,
                            )
                            SettingsRow(
                                title = "Delete Model",
                                subtitle = "Remove the local AI model file from this device.",
                                onClick = viewModel::deleteModel,
                                trailingIcon = Icons.Default.DeleteForever,
                                destructive = true,
                                enabled = uiState.modelStatus is SettingsModelStatus.Downloaded &&
                                    !uiState.isDownloadingModel &&
                                    !uiState.isDeletingModel,
                            )
                        }
                    }

                    item {
                        SettingsSection(
                            title = "Image Generation",
                            icon = Icons.Default.Image,
                        ) {
                            SettingsRow(
                                title = "Image Model Status",
                                subtitle = if (uiState.isDownloadingImageModel) {
                                    "Downloading image generation model..."
                                } else {
                                    imageModelStatusText(uiState.imageModelStatus)
                                },
                                onClick = { activeEditor = SettingsEditor.ImageModelStatus },
                            )
                            SettingsRow(
                                title = "Download Image Model",
                                subtitle = "Download the Stable Diffusion model for image generation.",
                                onClick = viewModel::redownloadImageModel,
                                trailingIcon = Icons.Default.Download,
                                enabled = !uiState.isDownloadingImageModel && !uiState.isDeletingImageModel,
                            )
                            SettingsRow(
                                title = "Delete Image Model",
                                subtitle = "Remove the image generation model from this device.",
                                onClick = viewModel::deleteImageModel,
                                trailingIcon = Icons.Default.DeleteForever,
                                destructive = true,
                                enabled = uiState.imageModelStatus is SettingsImageModelStatus.Downloaded &&
                                    !uiState.isDownloadingImageModel &&
                                    !uiState.isDeletingImageModel,
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    when (activeEditor) {
        SettingsEditor.ImageModelStatus -> {
            ImageModelStatusDialog(
                status = uiState.imageModelStatus,
                onDismiss = { activeEditor = null },
            )
        }
        SettingsEditor.GfName -> {
            TextValueEditorDialog(
                title = "Edit Her Name",
                initialValue = uiState.gfProfile?.name.orEmpty(),
                placeholder = "Enter her name",
                onDismiss = { activeEditor = null },
                onConfirm = {
                    viewModel.updateGfName(it)
                    activeEditor = null
                },
            )
        }
        SettingsEditor.Appearance -> {
            AppearancePickerDialog(
                selected = uiState.gfProfile?.visualTemplate,
                onDismiss = { activeEditor = null },
                onSelected = viewModel::updateVisualTemplate,
            )
        }
        SettingsEditor.Personality -> {
            PersonalityEditorDialog(
                selectedTraits = uiState.gfProfile?.personalityTraits.orEmpty(),
                onDismiss = { activeEditor = null },
                onToggleTrait = viewModel::togglePersonalityTrait,
            )
        }
        SettingsEditor.CustomNotes -> {
            TextValueEditorDialog(
                title = "Custom Notes",
                initialValue = uiState.gfProfile?.customPromptAdditions.orEmpty(),
                placeholder = "Add personality notes, pet names, or extra context",
                singleLine = false,
                onDismiss = { activeEditor = null },
                onConfirm = {
                    viewModel.updateCustomNotes(it)
                    activeEditor = null
                },
            )
        }
        SettingsEditor.UserInfo -> {
            PersonalInfoDialog(
                initialName = uiState.userProfile?.name.orEmpty(),
                initialAge = uiState.userProfile?.age?.toString().orEmpty(),
                onDismiss = { activeEditor = null },
                onConfirm = { name, age ->
                    viewModel.updateUserName(name)
                    viewModel.updateUserAge(age)
                    activeEditor = null
                },
            )
        }
        SettingsEditor.Interests -> {
            InterestsEditorDialog(
                interests = uiState.userProfile?.interests.orEmpty(),
                onDismiss = { activeEditor = null },
                onAddInterest = viewModel::addInterest,
                onRemoveInterest = viewModel::removeInterest,
            )
        }
        SettingsEditor.Voice -> {
            VoiceSelectionDialog(
                selected = uiState.gfProfile?.voiceOption,
                onDismiss = {
                    viewModel.stopPreview()
                    activeEditor = null
                },
                onSelect = { option -> viewModel.setVoiceOption(option) },
                onPreview = viewModel::previewVoice,
            )
        }
        SettingsEditor.ModelStatus -> {
            ModelStatusDialog(
                status = uiState.modelStatus,
                onDismiss = { activeEditor = null },
            )
        }
        SettingsEditor.ResetConfirmation -> {
            ResetConfirmationDialog(
                onDismiss = { activeEditor = null },
                onConfirm = {
                    viewModel.resetEverything()
                    activeEditor = null
                },
            )
        }
        null -> Unit
    }
}

@Composable
private fun SettingsTopBar() {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        cornerRadius = 20.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = Primary,
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface,
                    )
                    Text(
                        text = "Tune her personality, voice, and behavior.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsLoadingState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(4) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LoadingBlock(widthFraction = 0.35f, height = 18.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    LoadingBlock(widthFraction = 1f, height = 48.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    LoadingBlock(widthFraction = 0.85f, height = 48.dp)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                color = Primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Loading your settings...",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

@Composable
private fun LoadingBlock(
    widthFraction: Float,
    height: androidx.compose.ui.unit.Dp,
) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerHigh.copy(alpha = 0.7f)),
    )
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh.copy(alpha = 0.55f)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Primary,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OnSurface,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    trailingIcon: ImageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.55f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (destructive) Error else OnSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (!value.isNullOrBlank() && subtitle == null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = if (destructive) Error else OnSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.2f))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsChipSummaryRow(
    title: String,
    chips: List<String>,
    actionLabel: String,
    onActionClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurface,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Primary,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (chips.isEmpty()) {
            Text(
                text = "Nothing added yet.",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEach { chip ->
                    SelectableChip(
                        label = chip,
                        selected = true,
                        onClick = onActionClick,
                    )
                }
            }
        }
    }
    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.2f))
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.2f))
}

@Composable
private fun SpeechSpeedRow(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Speech Speed",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${"%.2f".format(speed)}x",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
            )
        }
        Slider(
            value = speed,
            onValueChange = onSpeedChange,
            valueRange = com.airgf.app.tts.TtsManager.MIN_SPEECH_RATE..com.airgf.app.tts.TtsManager.MAX_SPEECH_RATE,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationFrequencyRow(
    selected: String,
    onSelected: (String) -> Unit,
    enabled: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.55f)
            .padding(top = 10.dp),
    ) {
        Text(
            text = "Message Frequency",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = OnSurface,
        )
        Text(
            text = "How often she should check in when proactive messages are enabled.",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                FREQUENCY_RARELY to "Rarely",
                FREQUENCY_SOMETIMES to "Sometimes",
                FREQUENCY_OFTEN to "Often",
            ).forEach { (value, label) ->
                SelectableChip(
                    label = label,
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun TextValueEditorDialog(
    title: String,
    initialValue: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    singleLine: Boolean = true,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    SettingsDialogScaffold(
        title = title,
        onDismiss = onDismiss,
        onConfirm = {
            onConfirm(value.trim())
        },
    ) {
        PillTextField(
            value = value,
            onValueChange = { value = it },
            placeholder = placeholder,
            singleLine = singleLine,
        )
    }
}

@Composable
private fun PersonalInfoDialog(
    initialName: String,
    initialAge: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var age by remember(initialAge) { mutableStateOf(initialAge) }

    SettingsDialogScaffold(
        title = "Personal Info",
        onDismiss = onDismiss,
        onConfirm = {
            onConfirm(name.trim(), age)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PillTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Your name",
            )
            PillTextField(
                value = age,
                onValueChange = { age = it.filter(Char::isDigit).take(3) },
                placeholder = "Age",
                keyboardType = KeyboardType.Number,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonalityEditorDialog(
    selectedTraits: List<PersonalityTrait>,
    onDismiss: () -> Unit,
    onToggleTrait: (PersonalityTrait) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            cornerRadius = 24.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Personality Traits",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface,
                )
                Text(
                    text = "Pick up to 3 traits that shape how she talks and behaves.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PersonalityTrait.entries.forEach { trait ->
                        val selected = selectedTraits.contains(trait)
                        val atMax = selectedTraits.size >= 3 && !selected
                        SelectableChip(
                            label = trait.displayName,
                            selected = selected,
                            enabled = !atMax,
                            onClick = { onToggleTrait(trait) },
                        )
                    }
                }
                DialogButtons(
                    onDismiss = onDismiss,
                    onConfirm = onDismiss,
                    confirmLabel = "Done",
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun AppearancePickerDialog(
    selected: VisualTemplate?,
    onDismiss: () -> Unit,
    onSelected: (VisualTemplate) -> Unit,
) {
    var previewTemplate by remember(selected) {
        mutableStateOf(selected ?: VisualTemplate.entries.first())
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            cornerRadius = 24.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Change Appearance",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface,
                )
                Text(
                    text = "Choose the visual template you want to see in chat and character view.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
                AvatarModelPreview(
                    template = previewTemplate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                )
                Text(
                    text = previewTemplate.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OnSurface,
                    modifier = Modifier.padding(top = 10.dp, bottom = 16.dp),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    VisualTemplate.entries.toList().chunked(2).forEach { rowTemplates ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowTemplates.forEach { template ->
                                TemplateCard(
                                    template = template,
                                    selected = previewTemplate == template,
                                    onClick = {
                                        previewTemplate = template
                                        onSelected(template)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowTemplates.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                DialogButtons(
                    onDismiss = onDismiss,
                    onConfirm = onDismiss,
                    confirmLabel = "Done",
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun VoiceSelectionDialog(
    selected: VoiceOption?,
    onDismiss: () -> Unit,
    onSelect: (VoiceOption) -> Unit,
    onPreview: (VoiceOption) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            cornerRadius = 24.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Voice Selection",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface,
                )
                Text(
                    text = "Pick how she sounds when text-to-speech is enabled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
                voiceDescriptions.forEach { (option, description) ->
                    SelectionCard(
                        title = option.displayName,
                        subtitle = description,
                        selected = selected == option,
                        onClick = { onSelect(option) },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Row(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .clickable { onPreview(option) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Preview voice",
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary,
                        )
                    }
                }
                DialogButtons(
                    onDismiss = onDismiss,
                    onConfirm = onDismiss,
                    confirmLabel = "Done",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterestsEditorDialog(
    interests: List<String>,
    onDismiss: () -> Unit,
    onAddInterest: (String) -> Unit,
    onRemoveInterest: (String) -> Unit,
) {
    var pendingInterest by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            cornerRadius = 24.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Your Interests",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface,
                )
                Text(
                    text = "Add or remove the topics she should know you care about.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PillTextField(
                        value = pendingInterest,
                        onValueChange = { pendingInterest = it },
                        placeholder = "Add an interest",
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            onAddInterest(pendingInterest)
                            pendingInterest = ""
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add interest",
                            tint = Primary,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (interests.isEmpty()) {
                    Text(
                        text = "No interests added yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        interests.forEach { interest ->
                            SelectableChip(
                                label = interest,
                                selected = true,
                                onClick = { onRemoveInterest(interest) },
                            )
                        }
                    }
                }
                DialogButtons(
                    onDismiss = onDismiss,
                    onConfirm = onDismiss,
                    confirmLabel = "Done",
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun ResetConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            cornerRadius = 24.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = "Reset Everything?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = "This will clear your profiles, chat history, and preferences, then send you back through onboarding. Your downloaded model will stay on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                DialogButtons(
                    onDismiss = onDismiss,
                    onConfirm = onConfirm,
                    confirmLabel = "Reset",
                    destructive = true,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun ModelStatusDialog(
    status: SettingsModelStatus,
    onDismiss: () -> Unit,
) {
    val detailText = when (status) {
        SettingsModelStatus.Checking -> "Checking whether the AI model is available on this device."
        SettingsModelStatus.NotDownloaded -> "The local model is not available yet. Use Re-download Model to fetch it again."
        is SettingsModelStatus.Downloaded -> "Stored on device at:\n${status.path}\n\nSize: ${status.sizeLabel}"
    }

    SettingsDialogScaffold(
        title = "Model Status",
        onDismiss = onDismiss,
        onConfirm = onDismiss,
        confirmLabel = "Done",
    ) {
        Text(
            text = detailText,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsDialogScaffold(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String = "Save",
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            cornerRadius = 24.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface,
                )
                Spacer(modifier = Modifier.height(16.dp))
                content()
                DialogButtons(
                    onDismiss = onDismiss,
                    onConfirm = onConfirm,
                    confirmLabel = confirmLabel,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun DialogButtons(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = "Save",
    destructive: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
        ) {
            Text(text = "Cancel")
        }
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            colors = if (destructive) {
                ButtonDefaults.buttonColors(
                    containerColor = Error,
                    contentColor = OnError,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        ) {
            Text(text = confirmLabel)
        }
    }
}

private fun modelStatusText(status: SettingsModelStatus): String = when (status) {
    SettingsModelStatus.Checking -> "Checking local model..."
    SettingsModelStatus.NotDownloaded -> "Not downloaded"
    is SettingsModelStatus.Downloaded -> "Downloaded (${status.sizeLabel})"
}

private fun imageModelStatusText(status: SettingsImageModelStatus): String = when (status) {
    SettingsImageModelStatus.Checking -> "Checking image model..."
    SettingsImageModelStatus.NotDownloaded -> "Not downloaded"
    is SettingsImageModelStatus.Downloaded -> "Downloaded (${status.sizeLabel})"
}

@Composable
private fun ImageModelStatusDialog(
    status: SettingsImageModelStatus,
    onDismiss: () -> Unit,
) {
    val detailText = when (status) {
        SettingsImageModelStatus.Checking -> "Checking whether the image generation model is available on this device."
        SettingsImageModelStatus.NotDownloaded -> "The image model is not available yet. Use Download Image Model to fetch it."
        is SettingsImageModelStatus.Downloaded -> "Stored on device at:\n${status.path}\n\nSize: ${status.sizeLabel}"
    }
    SettingsDialogScaffold(
        title = "Image Model Status",
        onDismiss = onDismiss,
        onConfirm = onDismiss,
        confirmLabel = "Done",
    ) {
        Text(
            text = detailText,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
        )
    }
}

private val voiceDescriptions = listOf(
    VoiceOption.SOFT to "Gentle and warm, like a soft whisper.",
    VoiceOption.ENERGETIC to "Bright, lively, and full of energy.",
    VoiceOption.MATURE to "Calm, grounded, and reassuring.",
    VoiceOption.BREATHY to "Intimate, close, and more flirtatious.",
)
