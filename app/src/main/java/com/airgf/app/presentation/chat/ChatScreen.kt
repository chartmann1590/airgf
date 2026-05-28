package com.airgf.app.presentation.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.airgf.app.core.navigation.Route
import com.airgf.app.domain.model.Message
import com.airgf.app.llm.LlmEngine
import com.airgf.app.presentation.components.ChatBubble
import com.airgf.app.presentation.components.ChatDateDivider
import com.airgf.app.presentation.components.GfAvatar
import com.airgf.app.presentation.components.GlassPanel
import com.airgf.app.presentation.components.GradientBackground
import com.airgf.app.presentation.components.MainBottomNav
import com.airgf.app.presentation.components.PillTextField
import com.airgf.app.presentation.components.SpicyModeBanner
import com.airgf.app.presentation.components.TypingIndicator
import com.airgf.app.presentation.components.shouldShowDateDivider
import com.airgf.app.presentation.theme.OnSurface
import com.airgf.app.presentation.theme.OnSurfaceVariant
import com.airgf.app.presentation.theme.OutlineVariant
import com.airgf.app.presentation.theme.Primary
import com.airgf.app.presentation.theme.PrimaryContainer
import com.airgf.app.presentation.theme.PurpleShadow
import com.airgf.app.presentation.theme.SurfaceContainerHigh
import com.airgf.app.presentation.theme.UserBubbleEnd
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val gfName = uiState.gfProfile?.name ?: "Your GF"
    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var isInputFocused by remember { mutableStateOf(false) }
    var previewImagePath by remember { mutableStateOf<String?>(null) }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        uri?.let { viewModel.onImagePicked(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            // Camera capture handled via temp URI
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.messages.size, uiState.streamingText, uiState.isGenerating) {
        val itemCount = uiState.messages.size +
            (if (uiState.isGenerating) 1 else 0)
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    LaunchedEffect(isKeyboardOpen, isInputFocused) {
        val itemCount = uiState.messages.size +
            (if (uiState.isGenerating) 1 else 0)
        if (itemCount > 0 && (isKeyboardOpen || isInputFocused)) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    GradientBackground(showRadialGlow = true) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                ChatTopBar(
                    gfName = gfName,
                    visualTemplate = uiState.gfProfile?.visualTemplate,
                    ttsEnabled = uiState.ttsEnabled,
                    isSpeaking = uiState.isSpeaking,
                    onToggleTts = viewModel::toggleTtsEnabled,
                    onCallClick = { navController.navigate(Route.Call.path) },
                    onCharacterClick = { navController.navigate(Route.Character.path) },
                    onSettingsClick = { navController.navigate(Route.Settings.path) },
                    onRequestImage = viewModel::requestImage,
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.ime.union(WindowInsets.navigationBars),
                        ),
                ) {
                    if (uiState.pendingImageUri != null) {
                        PendingImagePreview(
                            imageUri = uiState.pendingImageUri,
                            bitmap = uiState.pendingImageBitmap,
                            onDismiss = viewModel::clearPendingImage,
                        )
                    }

                    if (uiState.showAttachmentOptions) {
                        AttachmentOptionsSheet(
                            onGalleryClick = {
                                pickMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onCameraClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Camera capture coming soon. Use gallery for now.")
                                }
                                viewModel.dismissAttachmentOptions()
                            },
                            onDismiss = viewModel::dismissAttachmentOptions,
                        )
                    }

                    ChatInputBar(
                        inputText = uiState.inputText,
                        isGenerating = uiState.isGenerating,
                        spicyModeEnabled = uiState.gfProfile?.spicyModeEnabled == true,
                        onInputChange = viewModel::onInputChange,
                        onInputFocusChanged = { isInputFocused = it },
                        onSend = viewModel::sendMessage,
                        onVoiceInputClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Voice input is not available yet. Type a message or use TTS for replies.",
                                )
                            }
                        },
                        onToggleSpicy = viewModel::toggleSpicyMode,
                        onAttachImage = viewModel::toggleAttachmentOptions,
                        hasPendingImage = uiState.pendingImageUri != null,
                    )
                    if (!isKeyboardOpen) {
                        MainBottomNav(
                            currentRoute = Route.Chat,
                            onNavigate = { route ->
                                if (route != Route.Chat) {
                                    navController.navigate(route.path) {
                                        launchSingleTop = true
                                    }
                                }
                            },
                        )
                    }
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    uiState.isInitializing || uiState.llmState is LlmEngine.LlmState.Loading -> {
                        LoadingOverlay(
                            message = if (uiState.llmState is LlmEngine.LlmState.Loading) {
                                "Loading AI model..."
                            } else {
                                "Preparing chat..."
                            },
                        )
                    }
                    uiState.llmState is LlmEngine.LlmState.Error -> {
                        ErrorOverlay(
                            message = (uiState.llmState as LlmEngine.LlmState.Error).message,
                            onRetry = viewModel::retryLlmInit,
                        )
                    }
                    else -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (uiState.gfProfile?.spicyModeEnabled == true) {
                                SpicyModeBanner()
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                itemsIndexed(
                                    items = uiState.messages,
                                    key = { _, message -> message.id },
                                ) { index, message ->
                                    val previous = uiState.messages.getOrNull(index - 1)
                                    if (shouldShowDateDivider(message.timestamp, previous?.timestamp)) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            ChatDateDivider(timestamp = message.timestamp)
                                        }
                                    }

                                    val showAvatar = !message.isUser &&
                                        (previous == null || previous.isUser || !shouldShowDateDivider(message.timestamp, previous.timestamp))

                                    ChatBubble(
                                        message = message,
                                        gfName = gfName,
                                        visualTemplate = uiState.gfProfile?.visualTemplate,
                                        showAvatar = showAvatar,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        onImageClick = { previewImagePath = it },
                                    )
                                }

                                if (uiState.isGenerating) {
                                    item(key = "streaming") {
                                        StreamingBubble(
                                            gfName = gfName,
                                            visualTemplate = uiState.gfProfile?.visualTemplate,
                                            streamingText = uiState.streamingText,
                                            showTypingIndicator = uiState.streamingText.isEmpty(),
                                            isGeneratingImage = uiState.isGeneratingImage,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        previewImagePath?.let { imagePath ->
            ImagePreviewDialog(
                imagePath = imagePath,
                onDismiss = { previewImagePath = null },
            )
        }
    }
}

@Composable
private fun ImagePreviewDialog(
    imagePath: String,
    onDismiss: () -> Unit,
) {
    val file = File(imagePath)
    if (!file.exists()) {
        onDismiss()
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = file,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close image", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    gfName: String,
    visualTemplate: com.airgf.app.domain.model.VisualTemplate?,
    ttsEnabled: Boolean,
    isSpeaking: Boolean,
    onToggleTts: () -> Unit,
    onCallClick: () -> Unit,
    onCharacterClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRequestImage: () -> Unit,
) {
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GfAvatar(
                gfName = gfName,
                visualTemplate = visualTemplate,
                prefer3dPreview = true,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = gfName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50)),
                    )
                    Text(
                        text = "Online",
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onRequestImage) {
                Icon(Icons.Outlined.Image, contentDescription = "Request image", tint = Primary)
            }

            IconButton(
                onClick = onToggleTts,
                modifier = if (ttsEnabled && isSpeaking) {
                    Modifier.shadow(8.dp, CircleShape, spotColor = PrimaryContainer.copy(alpha = 0.4f))
                } else {
                    Modifier
                },
            ) {
                Icon(
                    imageVector = if (ttsEnabled) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                    contentDescription = if (ttsEnabled) "Disable speech" else "Enable speech",
                    tint = if (ttsEnabled) Primary else OnSurfaceVariant,
                )
            }

            IconButton(onClick = onCallClick) {
                Icon(Icons.Outlined.Call, contentDescription = "Call", tint = Primary)
            }
            IconButton(onClick = onCharacterClick) {
                Icon(Icons.Outlined.Person, contentDescription = "Character", tint = Primary)
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Primary)
            }
        }
    }
}

@Composable
private fun PendingImagePreview(
    imageUri: Uri?,
    bitmap: android.graphics.Bitmap?,
    onDismiss: () -> Unit,
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        cornerRadius = 16.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerHigh),
            ) {
                if (bitmap != null) {
                    AsyncImage(
                        model = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Text(
                text = "Image attached",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Remove image", tint = OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AttachmentOptionsSheet(
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        cornerRadius = 16.dp,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onGalleryClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null, tint = Primary)
                Text(
                    text = "Gallery",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurface,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onCameraClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Primary)
                Text(
                    text = "Camera",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurface,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    isGenerating: Boolean,
    spicyModeEnabled: Boolean,
    onInputChange: (String) -> Unit,
    onInputFocusChanged: (Boolean) -> Unit,
    onSend: () -> Unit,
    onVoiceInputClick: () -> Unit,
    onToggleSpicy: () -> Unit,
    onAttachImage: () -> Unit,
    hasPendingImage: Boolean,
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        cornerRadius = 28.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onToggleSpicy) {
                Icon(
                    imageVector = if (spicyModeEnabled) {
                        Icons.Filled.LocalFireDepartment
                    } else {
                        Icons.Outlined.LocalFireDepartment
                    },
                    contentDescription = "Toggle spicy mode",
                    tint = if (spicyModeEnabled) UserBubbleEnd else OnSurfaceVariant,
                )
            }

            IconButton(onClick = onAttachImage) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = "Attach image",
                    tint = if (hasPendingImage) Primary else OnSurfaceVariant,
                )
            }

            PillTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = "Message...",
                modifier = Modifier.weight(1f),
                singleLine = false,
                onFocusChanged = onInputFocusChanged,
            )

            IconButton(onClick = onVoiceInputClick) {
                Icon(Icons.Outlined.Mic, contentDescription = "Voice input", tint = OnSurfaceVariant)
            }

            IconButton(
                onClick = onSend,
                enabled = (inputText.isNotBlank() || hasPendingImage) && !isGenerating,
                modifier = Modifier
                    .size(44.dp)
                    .shadow(8.dp, CircleShape, spotColor = PrimaryContainer.copy(alpha = 0.4f))
                    .clip(CircleShape)
                    .background(
                        if ((inputText.isNotBlank() || hasPendingImage) && !isGenerating) {
                            PrimaryContainer
                        } else {
                            PrimaryContainer.copy(alpha = 0.4f)
                        },
                    ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = OnSurface,
                )
            }
        }
    }
}

@Composable
private fun StreamingBubble(
    gfName: String,
    visualTemplate: com.airgf.app.domain.model.VisualTemplate?,
    streamingText: String,
    showTypingIndicator: Boolean,
    isGeneratingImage: Boolean = false,
) {
    if (showTypingIndicator) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            GfAvatar(
                gfName = gfName,
                visualTemplate = visualTemplate,
                modifier = Modifier.padding(end = 8.dp),
                prefer3dPreview = true,
            )
            if (isGeneratingImage) {
                Column {
                    TypingIndicator()
                    ImageGeneratingPlaceholder(
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            } else {
                TypingIndicator()
            }
        }
    } else {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            ChatBubble(
                message = Message(
                    conversationId = 0,
                    content = streamingText,
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                ),
                gfName = gfName,
                visualTemplate = visualTemplate,
                showAvatar = true,
                displayContent = streamingText,
            )
            if (isGeneratingImage) {
                ImageGeneratingPlaceholder(
                    modifier = Modifier.padding(start = 48.dp, top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ImageGeneratingPlaceholder(
    modifier: Modifier = Modifier,
) {
    val bubbleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
    val infiniteTransition = rememberInfiniteTransition(label = "imageShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(200.dp)
                .clip(bubbleShape)
                .background(SurfaceContainerHigh.copy(alpha = 0.9f))
                .border(
                    width = 1.dp,
                    color = OutlineVariant.copy(alpha = 0.3f),
                    shape = bubbleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    PrimaryContainer.copy(alpha = 0.0f),
                    PrimaryContainer.copy(alpha = 0.25f),
                    PrimaryContainer.copy(alpha = 0.0f),
                ),
                start = Offset(shimmerOffset * 400f, 0f),
                end = Offset(shimmerOffset * 400f + 400f, 400f),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(shimmerBrush),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Primary.copy(alpha = pulseAlpha),
                    strokeWidth = 2.5.dp,
                )
                Text(
                    text = "Generating image...",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun LoadingOverlay(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Primary)
            Text(
                text = message,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorOverlay(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
            )
            IconButton(
                onClick = onRetry,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .border(1.dp, Primary.copy(alpha = 0.5f), CircleShape),
            ) {
                Text(text = "Retry", color = Primary)
            }
        }
    }
}
