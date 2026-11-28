package com.htanh.appvk.data.repository

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ExamHistoryItem(
    val attemptId: String,
    val examId: String,
    val examName: String,
    val sessionType: String,
    val startedAtMillis: Long,
    val submittedAtMillis: Long = 0L,
    val status: String = "IN_PROGRESS",
    val syncStatus: String = "LOCAL_ONLY",
    val maximumScore: Double = 0.0,
    val earnedScore: Double = 0.0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val unansweredCount: Int = 0,
    val pendingEssayCount: Int = 0,
    val autoSubmitted: Boolean = false
)

class ExamHistoryStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized
    fun upsert(item: ExamHistoryItem) {
        val current = all().filterNot { it.attemptId == item.attemptId }
        val next = JSONArray()
        (current + item).sortedByDescending { it.startedAtMillis }.forEach { next.put(it.toJson()) }
        preferences.edit().putString(KEY_ITEMS, next.toString()).apply()
    }

    @Synchronized
    fun all(): List<ExamHistoryItem> {
        val raw = preferences.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                runCatching { array.getJSONObject(index).toHistoryItem() }.getOrNull()
            }.sortedByDescending { it.startedAtMillis }
        }.getOrDefault(emptyList())
    }

    private fun ExamHistoryItem.toJson() = JSONObject().apply {
        put("attempt_id", attemptId)
        put("exam_id", examId)
        put("exam_name", examName)
        put("session_type", sessionType)
        put("started_at_ms", startedAtMillis)
        put("submitted_at_ms", submittedAtMillis)
        put("status", status)
        put("sync_status", syncStatus)
        put("maximum_score", maximumScore)
        put("earned_score", earnedScore)
        put("correct_count", correctCount)
        put("incorrect_count", incorrectCount)
        put("unanswered_count", unansweredCount)
        put("pending_essay_count", pendingEssayCount)
        put("auto_submitted", autoSubmitted)
    }

    private fun JSONObject.toHistoryItem() = ExamHistoryItem(
        attemptId = getString("attempt_id"),
        examId = optString("exam_id"),
        examName = optString("exam_name"),
        sessionType = optString("session_type", "EXAM"),
        startedAtMillis = optLong("started_at_ms"),
        submittedAtMillis = optLong("submitted_at_ms"),
        status = optString("status", "IN_PROGRESS"),
        syncStatus = optString("sync_status", "LOCAL_ONLY"),
        maximumScore = optDouble("maximum_score"),
        earnedScore = optDouble("earned_score"),
        correctCount = optInt("correct_count"),
        incorrectCount = optInt("incorrect_count"),
        unansweredCount = optInt("unanswered_count"),
        pendingEssayCount = optInt("pending_essay_count"),
        autoSubmitted = optBoolean("auto_submitted")
    )

    companion object {
        private const val PREFERENCES = "htavk_exam_history"
        private const val KEY_ITEMS = "items"
    }
}
