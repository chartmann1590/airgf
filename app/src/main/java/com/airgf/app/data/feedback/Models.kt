package com.airgf.app.data.feedback

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BugReport(
    val number: Int,
    val title: String,
    val status: String,
    val createdAt: String,
    val htmlUrl: String
)

@Serializable
data class CreateIssueRequest(
    val title: String,
    val body: String
)

@Serializable
data class GithubIssue(
    val number: Int,
    val title: String,
    val state: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("created_at") val createdAt: String,
    val body: String? = null
)

@Serializable
data class GithubUser(
    val login: String
)

@Serializable
data class GithubComment(
    val id: Long,
    val body: String,
    @SerialName("created_at") val createdAt: String,
    val user: GithubUser
)

@Serializable
data class PostCommentRequest(
    val body: String
)

@Serializable
data class UploadAssetRequest(
    val message: String,
    val content: String
)

@Serializable
data class UploadContentInfo(
    @SerialName("download_url") val downloadUrl: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null
)

@Serializable
data class UploadAssetResponse(
    val content: UploadContentInfo? = null
)
