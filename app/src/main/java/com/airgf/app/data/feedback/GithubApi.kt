package com.airgf.app.data.feedback

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

/**
 * Talks to the cloudflare-worker/ feedback relay, not api.github.com directly — the
 * Worker holds the GitHub token as a server-side secret and hardcodes this app's own
 * repo, so no owner/repo/credential ever needs to travel through this app. Previously
 * embedded BuildConfig.GITHUB_API_TOKEN client-side as a Bearer header, which shipped a
 * real repo-write PAT in every release build (extractable from the APK). See
 * cloudflare-worker/src/index.ts.
 */
@Singleton
class GithubApi @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val baseUrl = "https://airgf-github-feedback.charles-h-hartmann1.workers.dev"
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    // Always true now — the relay is a fixed public Worker URL, not per-install config.
    val isConfigured: Boolean = true

    private fun authRequest(builder: Request.Builder): Request.Builder {
        return builder
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "AirGF-Android/0.1")
    }

    suspend fun createIssue(title: String, body: String): GithubIssue = withContext(Dispatchers.IO) {
        val requestBody = json.encodeToString(
            CreateIssueRequest.serializer(),
            CreateIssueRequest(title, body)
        )
        val request = authRequest(Request.Builder().url("$baseUrl/issue"))
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
            Request.Builder().url("$baseUrl/issue/$issueNumber")
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
            Request.Builder().url("$baseUrl/issue/$issueNumber/comments")
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
            Request.Builder().url("$baseUrl/issue/$issueNumber/comments")
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

    suspend fun uploadAsset(filename: String, base64Content: String): UploadAssetResponse =
        withContext(Dispatchers.IO) {
            val uploadRequest = UploadAssetRequest(
                filename = filename,
                contentBase64 = base64Content
            )
            val requestBody = json.encodeToString(
                UploadAssetRequest.serializer(),
                uploadRequest
            )
            val request = authRequest(
                Request.Builder().url("$baseUrl/upload-image")
            )
                .post(requestBody.toRequestBody(mediaType))
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
        return "issue-$issueNumber-$timestamp-$random.png"
    }
}
