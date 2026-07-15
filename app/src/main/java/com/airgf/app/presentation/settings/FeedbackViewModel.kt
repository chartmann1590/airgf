package com.airgf.app.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgf.app.data.feedback.BugReport
import com.airgf.app.data.feedback.BugReportRepo
import com.airgf.app.data.feedback.DiagnosticsHelper
import com.airgf.app.data.feedback.GithubApi
import com.airgf.app.data.feedback.GithubComment
import com.airgf.app.data.feedback.GithubIssue
import com.airgf.app.data.feedback.ImageUploadHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val isConfigured: Boolean = false,
    val isSubmittingReport: Boolean = false,
    val isLoadingIssue: Boolean = false,
    val isPostingComment: Boolean = false,
    val reportTitle: String = "",
    val reportDescription: String = "",
    val reportIncludeDiagnostics: Boolean = true,
    val reportName: String = "",
    val reportEmail: String = "",
    val reportImageUri: Uri? = null,
    val selectedIssue: GithubIssue? = null,
    val selectedReport: BugReport? = null,
    val comments: List<GithubComment> = emptyList(),
    val commentText: String = "",
    val commentImageUri: Uri? = null,
    val error: String? = null,
    val successMessage: String? = null,
)

sealed interface FeedbackEvent {
    data class ShowMessage(val message: String) : FeedbackEvent
}

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val githubApi: GithubApi,
    private val bugReportRepo: BugReportRepo,
    private val diagnosticsHelper: DiagnosticsHelper,
    private val imageUploadHelper: ImageUploadHelper,
) : ViewModel() {

    private val _state = MutableStateFlow(FeedbackUiState(isConfigured = githubApi.isConfigured))
    val state: StateFlow<FeedbackUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<FeedbackEvent>()
    val events: SharedFlow<FeedbackEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            bugReportRepo.bugReports.collect { reports ->
                _state.update { it.copy() }
            }
        }
    }

    val bugReports = bugReportRepo.bugReports

    fun updateReportTitle(value: String) {
        _state.update { it.copy(reportTitle = value) }
    }

    fun updateReportDescription(value: String) {
        _state.update { it.copy(reportDescription = value) }
    }

    fun updateReportIncludeDiagnostics(value: Boolean) {
        _state.update { it.copy(reportIncludeDiagnostics = value) }
    }

    fun updateReportName(value: String) {
        _state.update { it.copy(reportName = value) }
    }

    fun updateReportEmail(value: String) {
        _state.update { it.copy(reportEmail = value) }
    }

    fun setReportImageUri(uri: Uri?) {
        _state.update { it.copy(reportImageUri = uri) }
    }

    fun clearReportImage() {
        _state.update { it.copy(reportImageUri = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
    }

    fun resetReportForm() {
        _state.update {
            it.copy(
                reportTitle = "",
                reportDescription = "",
                reportIncludeDiagnostics = true,
                reportName = "",
                reportEmail = "",
                reportImageUri = null,
                isSubmittingReport = false,
                error = null,
            )
        }
    }

    fun submitReport() {
        val s = _state.value
        if (s.reportTitle.isBlank() || s.reportDescription.isBlank()) return
        if (!githubApi.isConfigured) {
            _state.update { it.copy(error = "GitHub configuration is missing. Check local.properties.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmittingReport = true, error = null) }
            try {
                val imageUrl = s.reportImageUri?.let { uri ->
                    val base64 = imageUploadHelper.uriToBase64(uri)
                    val path = githubApi.generateAssetPath(0)
                    val response = githubApi.uploadAsset(path, base64)
                    response.content?.downloadUrl
                }

                val contactInfo = buildString {
                    appendLine("## Contact Info")
                    appendLine()
                    appendLine(
                        "- Name: ${
                            s.reportName.ifBlank { "Not provided" }
                        }"
                    )
                    appendLine(
                        "- Email: ${
                            s.reportEmail.ifBlank { "Not provided" }
                        }"
                    )
                }

                val body = buildString {
                    appendLine("## Description")
                    appendLine()
                    appendLine(s.reportDescription)
                    appendLine()
                    appendLine(contactInfo)
                    if (imageUrl != null) {
                        appendLine()
                        appendLine("## Attachment")
                        appendLine()
                        appendLine("![Screenshot]($imageUrl)")
                    }
                    if (s.reportIncludeDiagnostics) {
                        appendLine()
                        appendLine(diagnosticsHelper.collectDiagnostics())
                    }
                }

                val issue = githubApi.createIssue("[Feedback] ${s.reportTitle}", body)

                val report = BugReport(
                    number = issue.number,
                    title = issue.title,
                    status = issue.state,
                    createdAt = issue.createdAt,
                    htmlUrl = issue.htmlUrl,
                )
                bugReportRepo.saveBugReport(report)

                resetReportForm()
                _state.update { it.copy(successMessage = "Issue #${issue.number} created successfully.") }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSubmittingReport = false,
                        error = e.message ?: "Failed to submit report."
                    )
                }
            }
        }
    }

    fun openIssueDetail(report: BugReport) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    selectedReport = report,
                    selectedIssue = null,
                    comments = emptyList(),
                    commentText = "",
                    commentImageUri = null,
                    isLoadingIssue = true,
                    error = null,
                )
            }
            try {
                val issue = githubApi.getIssue(report.number)
                val comments = githubApi.getComments(report.number)

                if (issue.state != report.status) {
                    bugReportRepo.saveBugReport(
                        report.copy(status = issue.state)
                    )
                }

                _state.update {
                    it.copy(
                        selectedIssue = issue,
                        comments = comments,
                        isLoadingIssue = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingIssue = false,
                        error = e.message ?: "Failed to load issue details.",
                    )
                }
            }
        }
    }

    fun closeIssueDetail() {
        _state.update {
            it.copy(
                selectedReport = null,
                selectedIssue = null,
                comments = emptyList(),
                commentText = "",
                commentImageUri = null,
                isLoadingIssue = false,
                isPostingComment = false,
                error = null,
            )
        }
    }

    fun updateCommentText(value: String) {
        _state.update { it.copy(commentText = value) }
    }

    fun setCommentImageUri(uri: Uri?) {
        _state.update { it.copy(commentImageUri = uri) }
    }

    fun clearCommentImage() {
        _state.update { it.copy(commentImageUri = null) }
    }

    fun postComment() {
        val s = _state.value
        val issueNumber = s.selectedReport?.number ?: return
        if (s.commentText.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isPostingComment = true, error = null) }
            try {
                val imageUrl = s.commentImageUri?.let { uri ->
                    val base64 = imageUploadHelper.uriToBase64(uri)
                    val path = githubApi.generateAssetPath(issueNumber)
                    val response = githubApi.uploadAsset(path, base64)
                    response.content?.downloadUrl
                }

                val body = buildString {
                    appendLine("## Reply")
                    appendLine()
                    appendLine(s.commentText)
                    if (imageUrl != null) {
                        appendLine()
                        appendLine("## Attachment")
                        appendLine()
                        appendLine("![Screenshot]($imageUrl)")
                    }
                }

                githubApi.postComment(issueNumber, body)

                val comments = githubApi.getComments(issueNumber)
                _state.update {
                    it.copy(
                        comments = comments,
                        commentText = "",
                        commentImageUri = null,
                        isPostingComment = false,
                        successMessage = "Comment posted.",
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isPostingComment = false,
                        error = e.message ?: "Failed to post comment.",
                    )
                }
            }
        }
    }

    fun showMessage(message: String) {
        viewModelScope.launch {
            _events.emit(FeedbackEvent.ShowMessage(message))
        }
    }
}
