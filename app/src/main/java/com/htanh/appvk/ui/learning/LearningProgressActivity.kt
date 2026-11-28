package com.htanh.appvk.ui.learning

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.htanh.appvk.HtavkApplication
import com.htanh.appvk.R
import com.htanh.appvk.data.repository.ExamHistoryItem
import com.htanh.appvk.ui.applySystemBarInsets
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LearningProgressActivity : AppCompatActivity() {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning_progress)
        applySystemBarInsets(findViewById(R.id.progressRoot))

        findViewById<MaterialToolbar>(R.id.progressToolbar)
            .setNavigationOnClickListener { finish() }
        val summary = findViewById<TextView>(R.id.tvProgressSummary)
        val notice = findViewById<TextView>(R.id.tvProgressNotice)

        val app = application as HtavkApplication
        lifecycleScope.launch {
            app.learningContentRepository.observeCatalog().collect { catalog ->
                summary.text = getString(
                    R.string.progress_catalog_summary,
                    catalog.subjects.count { it.isActive },
                    catalog.chapters.count { it.isActive },
                    catalog.topics.count { it.isActive },
                    catalog.questions.count { it.isActive }
                )
                notice.text = buildHistoryText(app.examHistoryStore.all())
                notice.visibility = View.VISIBLE
            }
        }
    }

    private fun buildHistoryText(items: List<ExamHistoryItem>): String = buildString {
        append(getString(R.string.progress_history_heading))
        append("\n\n")
        if (items.isEmpty()) {
            append(getString(R.string.progress_history_empty))
            return@buildString
        }
        items.forEachIndexed { index, item ->
            append(index + 1)
            append(". ")
            append(item.examName.ifBlank { item.examId })
            append(" • ")
            append(if (item.sessionType == "PRACTICE") "Luyện tập" else "Kiểm tra")
            append("\n")
            append("   ")
            append(if (item.submittedAtMillis > 0L) dateFormat.format(Date(item.submittedAtMillis)) else dateFormat.format(Date(item.startedAtMillis)))
            append(" • ")
            append(
                if (item.status == "SUBMITTED") {
                    "Đã nộp: %.2f / %.2f".format(Locale("vi", "VN"), item.earnedScore, item.maximumScore)
                } else {
                    "Đang thực hiện"
                }
            )
            append("\n")
            if (item.pendingEssayCount > 0) {
                append("   ")
                append("Tự luận chờ chấm: ")
                append(item.pendingEssayCount)
                append("\n")
            }
            append("   ")
            append(
                when (item.syncStatus) {
                    "SYNCED" -> "Đã đồng bộ lên Firebase"
                    "PENDING" -> "Đang chờ xác nhận đồng bộ"
                    else -> "Đã lưu trên thiết bị"
                }
            )
            if (item.autoSubmitted) {
                append(" • Tự động nộp")
            }
            if (index < items.lastIndex) append("\n\n")
        }
    }
}
