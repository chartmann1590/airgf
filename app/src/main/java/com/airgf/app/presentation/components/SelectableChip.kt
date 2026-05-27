package com.airgf.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.airgf.app.presentation.theme.OnSurface
import com.airgf.app.presentation.theme.Secondary
import com.airgf.app.presentation.theme.SecondaryContainer
import com.airgf.app.presentation.theme.UserBubbleStart

@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) {
                    SecondaryContainer.copy(alpha = 0.4f)
                } else {
                    SecondaryContainer.copy(alpha = 0.2f)
                },
            )
            .border(
                width = 1.dp,
                color = if (selected) SecondaryContainer else SecondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(50),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) OnSurface else Secondary,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) OnSurface else Secondary,
        )
    }
}
