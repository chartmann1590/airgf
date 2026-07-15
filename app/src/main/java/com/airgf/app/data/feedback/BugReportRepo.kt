package com.airgf.app.data.feedback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.feedbackDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "feedback_bug_reports"
)

@Singleton
class BugReportRepo @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val BUG_REPORTS_LIST = stringPreferencesKey("bug_reports_list")
    }

    val bugReports: Flow<List<BugReport>> = context.feedbackDataStore.data.map { prefs ->
        val raw = prefs[Keys.BUG_REPORTS_LIST] ?: return@map emptyList()
        runCatching {
            json.decodeFromString(ListSerializer(BugReport.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    suspend fun saveBugReport(report: BugReport) {
        context.feedbackDataStore.edit { prefs ->
            val raw = prefs[Keys.BUG_REPORTS_LIST]
            val reports = if (raw != null) {
                runCatching {
                    json.decodeFromString(
                        ListSerializer(BugReport.serializer()),
                        raw
                    ).toMutableList()
                }.getOrDefault(mutableListOf())
            } else {
                mutableListOf()
            }

            val existingIndex = reports.indexOfFirst { it.number == report.number }
            if (existingIndex >= 0) {
                reports[existingIndex] = report
            } else {
                reports.add(0, report)
            }

            prefs[Keys.BUG_REPORTS_LIST] = json.encodeToString(
                ListSerializer(BugReport.serializer()),
                reports
            )
        }
    }

    suspend fun updateBugReports(reports: List<BugReport>) {
        context.feedbackDataStore.edit { prefs ->
            prefs[Keys.BUG_REPORTS_LIST] = json.encodeToString(
                ListSerializer(BugReport.serializer()),
                reports
            )
        }
    }

    suspend fun getBugReportsList(): List<BugReport> {
        val prefs = context.feedbackDataStore.data.first()
        val raw = prefs[Keys.BUG_REPORTS_LIST] ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(BugReport.serializer()), raw)
        }.getOrDefault(emptyList())
    }
}
