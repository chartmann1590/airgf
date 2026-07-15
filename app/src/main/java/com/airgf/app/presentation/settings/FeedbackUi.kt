package com.airgf.app.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.airgf.app.data.feedback.BugReport
import com.airgf.app.data.feedback.GithubComment
import com.airgf.app.presentation.components.GlassPanel
import com.airgf.app.presentation.components.PillTextField
import com.airgf.app.presentation.theme.Error
import com.airgf.app.presentation.theme.OnError
import com.airgf.app.presentation.theme.OnSurface
import com.airgf.app.presentation.theme.OnSurfaceVariant
import com.airgf.app.presentation.theme.OutlineVariant
import com.airgf.app.presentation.theme.Primary
import com.airgf.app.presentation.theme.PrimaryContainer
import com.airgf.app.presentation.theme.SurfaceContainerHigh
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun FeedbackSection(
    viewModel: FeedbackViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val bugReports by viewModel.bugReports.collectAsState(initial = emptyList())
    var showReportDialog by remember { mutableStateOf(false) }
    var showIssueDetail by remember { mutableStateOf(false) }
    val uiState by viewModel.state.collectAsState()

    if (showReportDialog) {
        ReportBugDialog(
            viewModel = viewModel,
            onDismiss = {
                showReportDialog = false
                viewModel.resetReportForm()
            },
        )
    }

    if (showIssueDetail && uiState.selectedReport != null) {
        IssueDetailsDialog(
            viewModel = viewModel,
            onDismiss = {
                showIssueDetail = false
                viewModel.closeIssueDetail()
            },
        )
    }

    SettingsSection(
        title = "Support & Feedback",
        icon = Icons.Default.BugReport,
        modifier = modifier,
    ) {
        if (!uiState.isConfigured) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Error.copy(alpha = 0.12f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GitHub not configured. Add github.api.token to local.properties to enable reporting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurface,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = { showReportDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isConfigured,
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Report a Problem")
        }

        if (bugReports.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Submitted Reports",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))

            for (report in bugReports) {
                FeedbackReportRow(
                    report = report,
                    onClick = {
                        viewModel.openIssueDetail(report)
                        showIssueDetail = true
                    },
                )
            }
        }
    }
}

@Composable
private fun FeedbackReportRow(
    report: BugReport,
    onClick: () -> Unit,
) {
    val isOpen = report.status.equals("open", ignoreCase = true)
    val dateFormatted = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val date = parser.parse(report.createdAt)
        SimpleDateFormat("MMM d, yyyy", Locale.US).format(date!!)
    } catch (_: Exception) {
        report.createdAt
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (isOpen) PrimaryContainer else Error
                ),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = report.title.removePrefix("[Feedback] "),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "#${report.number} · $dateFormatted · ${if (isOpen) "Open" else "Closed"}",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
    }
    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.2f))
}

@Composable
fun ReportBugDialog(
    viewModel: FeedbackViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { viewModel.setReportImageUri(it) }
    }

    Dialog(onDismissRequest = { if (!uiState.isSubmittingReport) onDismiss() }) {
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
                    text = "Report a Problem",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Warning box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Primary.copy(alpha = 0.08f),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Your report will be submitted to this app's GitHub issue tracker. Do not include passwords, private keys, medical information, financial information, or anything you do not want visible to the repository maintainers. If this repository is public, your report may be publicly visible.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurface,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                PillTextField(
                    value = uiState.reportTitle,
                    onValueChange = viewModel::updateReportTitle,
                    placeholder = "Subject / Title *",
                )

                Spacer(modifier = Modifier.height(8.dp))

                PillTextField(
                    value = uiState.reportDescription,
                    onValueChange = viewModel::updateReportDescription,
                    placeholder = "Describe the issue in detail *",
                    singleLine = false,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = uiState.reportIncludeDiagnostics,
                        onCheckedChange = viewModel::updateReportIncludeDiagnostics,
                    )
                    Text(
                        text = "Include device/app diagnostics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                PillTextField(
                    value = uiState.reportName,
                    onValueChange = viewModel::updateReportName,
                    placeholder = "Your name (optional)",
                )

                Spacer(modifier = Modifier.height(8.dp))

                PillTextField(
                    value = uiState.reportEmail,
                    onValueChange = viewModel::updateReportEmail,
                    placeholder = "Your email (optional)",
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Attachment
                if (uiState.reportImageUri != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = uiState.reportImageUri,
                            contentDescription = "Attached screenshot",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Screenshot attached",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = viewModel::clearReportImage) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove attachment",
                                tint = OnSurfaceVariant,
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Attach Screenshot")
                    }
                }

                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Error.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Error,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSubmittingReport,
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = viewModel::submitReport,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSubmittingReport &&
                            uiState.reportTitle.isNotBlank() &&
                            uiState.reportDescription.isNotBlank(),
                    ) {
                        if (uiState.isSubmittingReport) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = OnError,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IssueDetailsDialog(
    viewModel: FeedbackViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { viewModel.setCommentImageUri(it) }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.9f)
                .padding(16.dp),
            cornerRadius = 24.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Issue #${uiState.selectedReport?.number ?: ""}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = OnSurfaceVariant,
                        )
                    }
                }

                if (uiState.error != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Error.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Error,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.isLoadingIssue) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else {
                    val issue = uiState.selectedIssue
                    val report = uiState.selectedReport

                    if (issue != null || report != null) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            val title = issue?.title ?: report?.title ?: ""
                            val status = issue?.state ?: report?.status ?: ""
                            val htmlUrl = issue?.htmlUrl ?: report?.htmlUrl ?: ""
                            val isOpen = status.equals("open", ignoreCase = true)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isOpen) PrimaryContainer else Error),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isOpen) "Open" else "Closed",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isOpen) PrimaryContainer else Error,
                                )
                                Spacer(modifier = Modifier.width(16.dp))

                                if (htmlUrl.isNotBlank()) {
                                    TextButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl))
                                            context.startActivity(intent)
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Open on GitHub",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = title.removePrefix("[Feedback] "),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = OnSurface,
                            )

                            issue?.body?.let { body ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant,
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Comments (${uiState.comments.size})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = OnSurface,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (uiState.comments.isEmpty()) {
                                Text(
                                    text = "No comments yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant,
                                )
                            } else {
                                for (comment in uiState.comments) {
                                    CommentBubble(comment)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Reply section
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SurfaceContainerHigh.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    PillTextField(
                                        value = uiState.commentText,
                                        onValueChange = viewModel::updateCommentText,
                                        placeholder = "Write a reply...",
                                        singleLine = false,
                                    )

                                    if (uiState.commentImageUri != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(
                                                model = uiState.commentImageUri,
                                                contentDescription = "Attachment",
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(10.dp)),
                                                contentScale = ContentScale.Crop,
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Image attached",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OnSurfaceVariant,
                                                modifier = Modifier.weight(1f),
                                            )
                                            IconButton(onClick = viewModel::clearCommentImage) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = OnSurfaceVariant,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        IconButton(
                                            onClick = {
                                                imagePickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            enabled = !uiState.isPostingComment,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = "Attach image",
                                                tint = if (uiState.isPostingComment) OnSurfaceVariant.copy(alpha = 0.4f) else OnSurfaceVariant,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = viewModel::postComment,
                                            enabled = uiState.commentText.isNotBlank() && !uiState.isPostingComment,
                                        ) {
                                            if (uiState.isPostingComment) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    color = OnError,
                                                    strokeWidth = 2.dp,
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reply")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentBubble(comment: GithubComment) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerHigh.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = comment.user.login,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OnSurface,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = try {
                        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                        val date = parser.parse(comment.createdAt)
                        SimpleDateFormat("MMM d, h:mm a", Locale.US).format(date!!)
                    } catch (_: Exception) {
                        comment.createdAt
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = comment.body,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurface,
            )
        }
    }
}
