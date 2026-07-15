package com.airgf.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.airgf.app.domain.model.Message
import com.airgf.app.domain.model.VisualTemplate
import com.airgf.app.presentation.theme.OnSurface
import com.airgf.app.presentation.theme.OnSurfaceVariant
import com.airgf.app.presentation.theme.OutlineVariant
import com.airgf.app.presentation.theme.PrimaryContainer
import com.airgf.app.presentation.theme.PurpleShadow
import com.airgf.app.presentation.theme.SurfaceContainerHigh
import com.airgf.app.presentation.theme.UserBubbleEnd
import com.airgf.app.presentation.theme.UserBubbleStart
import java.io.File

@Composable
fun ChatBubble(
    message: Message,
    gfName: String,
    visualTemplate: VisualTemplate?,
    showAvatar: Boolean,
    modifier: Modifier = Modifier,
    displayContent: String = message.content,
    onImageClick: (String) -> Unit = {},
    onReport: (Message) -> Unit = {},
) {
    if (message.isUser) {
        UserChatBubble(
            content = displayContent,
            imagePath = message.imagePath,
            timestamp = message.timestamp,
            modifier = modifier,
            onImageClick = onImageClick,
        )
    } else {
        GfChatBubble(
            content = displayContent,
            imagePath = message.imagePath,
            timestamp = message.timestamp,
            gfName = gfName,
            visualTemplate = visualTemplate,
            showAvatar = showAvatar,
            modifier = modifier,
            onImageClick = onImageClick,
            onReport = { onReport(message) },
        )
    }
}

@Composable
private fun ChatBubbleImage(
    imagePath: String?,
    shape: RoundedCornerShape,
    onImageClick: (String) -> Unit,
) {
    if (imagePath == null) return
    val file = File(imagePath)
    if (!file.exists()) return
    AsyncImage(
        model = file,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(shape)
            .clickable { onImageClick(imagePath) },
    )
}

@Composable
private fun UserChatBubble(
    content: String,
    imagePath: String?,
    timestamp: Long,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit,
) {
    val bubbleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .shadow(4.dp, bubbleShape, spotColor = PurpleShadow)
                .clip(bubbleShape)
                .background(
                    Brush.linearGradient(listOf(UserBubbleStart, UserBubbleEnd)),
                ),
        ) {
            Column {
                ChatBubbleImage(
                    imagePath = imagePath,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (content.isBlank()) 16.dp else 4.dp, bottomEnd = if (content.isBlank()) 4.dp else 4.dp),
                    onImageClick = onImageClick,
                )
                if (content.isNotBlank()) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                } else if (imagePath == null) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
        Text(
            text = formatChatTime(timestamp),
            modifier = Modifier.padding(top = 4.dp, end = 4.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize * 0.85f),
            color = OnSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun GfChatBubble(
    content: String,
    imagePath: String?,
    timestamp: Long,
    gfName: String,
    visualTemplate: VisualTemplate?,
    showAvatar: Boolean,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit,
    onReport: () -> Unit,
) {
    val bubbleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (showAvatar) {
            GfAvatar(
                gfName = gfName,
                visualTemplate = visualTemplate,
                modifier = Modifier.padding(end = 8.dp),
                prefer3dPreview = true,
            )
        } else {
            Box(modifier = Modifier.size(40.dp).padding(end = 8.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(bubbleShape)
                    .background(SurfaceContainerHigh.copy(alpha = 0.8f))
                    .border(
                        width = 1.dp,
                        color = OutlineVariant.copy(alpha = 0.4f),
                        shape = bubbleShape,
                    ),
            ) {
                Column {
                    ChatBubbleImage(
                        imagePath = imagePath,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = if (content.isBlank()) 16.dp else 4.dp, bottomStart = if (content.isBlank()) 4.dp else 4.dp),
                        onImageClick = onImageClick,
                    )
                    if (content.isNotBlank()) {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    } else if (imagePath == null) {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatChatTime(timestamp),
                    modifier = Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize * 0.85f),
                    color = OnSurfaceVariant.copy(alpha = 0.6f),
                )
                IconButton(onClick = onReport, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Flag, contentDescription = "Report AI response", modifier = Modifier.size(16.dp), tint = OnSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun GfAvatar(
    gfName: String,
    visualTemplate: VisualTemplate?,
    modifier: Modifier = Modifier,
    showOnlineIndicator: Boolean = true,
    prefer3dPreview: Boolean = false,
) {
    Box(modifier = modifier.size(40.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(6.dp, CircleShape, spotColor = PrimaryContainer.copy(alpha = 0.5f))
                .clip(CircleShape)
                .border(2.dp, PrimaryContainer.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (visualTemplate != null) {
                CharacterThumbnail(
                    template = visualTemplate,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(SurfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = gfName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface,
                    )
                }
            }
        }
        if (showOnlineIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
                    .border(1.5.dp, SurfaceContainerHigh, CircleShape),
            )
        }
    }
}
