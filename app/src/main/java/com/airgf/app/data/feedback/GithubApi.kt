package com.airgf.app.data.feedback

import com.airgf.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GithubApi @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val baseUrl = "https://api.github.com"
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private val hasToken = BuildConfig.GITHUB_API_TOKEN.isNotBlank()
    private val owner = BuildConfig.GITHUB_REPO_OWNER
    private val repo = BuildConfig.GITHUB_REPO_NAME

    val isConfigured: Boolean
        get() = hasToken && owner.isNotBlank() && repo.isNotBlank()

    private fun authRequest(builder: Request.Builder): Request.Builder {
        return builder
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "AirGF-Android/0.1")
            .apply {
                if (hasToken) {
                    header("Authorization", "Bearer ${BuildConfig.GITHUB_API_TOKEN}")
                }
            }
    }

    suspend fun createIssue(title: String, body: String): GithubIssue = withContext(Dispatchers.IO) {
        val requestBody = json.encodeToString(
            CreateIssueRequest.serializer(),
            CreateIssueRequest(title, body)
        )
        val request = authRequest(Request.Builder().url("$baseUrl/repos/$owner/$repo/issues"))
            .post(requestBody.toRequestBody(mediaType))
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw IOException("Failed to create issue (${response.code}): $errorBody")
        }
        val bodyString = response.body?.string() ?: throw IOException("Empty response body")
        json.decodeFromString(GithubIssue.serializer(), bodyString)
    }

    suspend fun getIssue(issueNumber: Int): GithubIssue = withContext(Dispatchers.IO) {
        val request = authRequest(
            Request.Builder().url("$baseUrl/repos/$owner/$repo/issues/$issueNumber")
        )
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw IOException("Failed to fetch issue (${response.code}): $errorBody")
        }
        val bodyString = response.body?.string() ?: throw IOException("Empty response body")
        json.decodeFromString(GithubIssue.serializer(), bodyString)
    }

    suspend fun getComments(issueNumber: Int): List<GithubComment> = withContext(Dispatchers.IO) {
        val request = authRequest(
            Request.Builder().url("$baseUrl/repos/$owner/$repo/issues/$issueNumber/comments")
        )
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw IOException("Failed to fetch comments (${response.code}): $errorBody")
        }
        val bodyString = response.body?.string() ?: throw IOException("Empty response body")
        json.decodeFromString(ListSerializer(GithubComment.serializer()), bodyString)
    }

    suspend fun postComment(issueNumber: Int, body: String): GithubComment = withContext(Dispatchers.IO) {
        val requestBody = json.encodeToString(
            PostCommentRequest.serializer(),
            PostCommentRequest(body)
        )
        val request = authRequest(
            Request.Builder().url("$baseUrl/repos/$owner/$repo/issues/$issueNumber/comments")
        )
            .post(requestBody.toRequestBody(mediaType))
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw IOException("Failed to post comment (${response.code}): $errorBody")
        }
        val bodyString = response.body?.string() ?: throw IOException("Empty response body")
        json.decodeFromString(GithubComment.serializer(), bodyString)
    }

    suspend fun uploadAsset(path: String, base64Content: String): UploadAssetResponse =
        withContext(Dispatchers.IO) {
            val uploadRequest = UploadAssetRequest(
                message = "Upload asset for feedback",
                content = base64Content
            )
            val requestBody = json.encodeToString(
                UploadAssetRequest.serializer(),
                uploadRequest
            )
            val request = authRequest(
                Request.Builder().url("$baseUrl/repos/$owner/$repo/contents/$path")
            )
                .put(requestBody.toRequestBody(mediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw IOException("Failed to upload asset (${response.code}): $errorBody")
            }
            val bodyString = response.body?.string() ?: throw IOException("Empty response body")
            json.decodeFromString(UploadAssetResponse.serializer(), bodyString)
        }

    fun generateAssetPath(issueNumber: Int): String {
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        val random = (1000..9999).random()
        return "${BuildConfig.FEEDBACK_ASSETS_DIR}/issue-$issueNumber-$timestamp-$random.png"
    }
}
