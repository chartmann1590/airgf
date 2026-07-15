package com.airgf.app.data.repository

import com.airgf.app.BuildConfig
import com.airgf.app.data.local.db.dao.AiReportDao
import com.airgf.app.data.local.db.entity.AiReportEntity
import com.airgf.app.domain.model.Message
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class AiReportRepository @Inject constructor(
    private val dao: AiReportDao,
    private val client: OkHttpClient,
) {
    private val json = Json { encodeDefaults = true }

    suspend fun report(message: Message, reason: String, context: String?): Boolean = withContext(Dispatchers.IO) {
        val createdAt = System.currentTimeMillis()
        val reportId = dao.insert(
            AiReportEntity(
                messageId = message.id,
                reason = reason,
                content = message.content,
                context = context,
                createdAt = createdAt,
            ),
        )
        val endpoint = BuildConfig.REPORT_ENDPOINT
        if (endpoint.isBlank()) return@withContext false
        val payload = json.encodeToString(ReportPayload(reason, message.content, context, createdAt))
        val request = Request.Builder()
            .url(endpoint)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        val sent = runCatching { client.newCall(request).execute().use { it.isSuccessful } }.getOrDefault(false)
        if (sent) dao.updateStatus(reportId, "SENT")
        sent
    }

    @Serializable
    private data class ReportPayload(
        val reason: String,
        val content: String,
        val context: String?,
        val createdAt: Long,
        val appVersion: String = BuildConfig.VERSION_NAME,
    )
}
