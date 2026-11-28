package com.htanh.appvk.ui.learning

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.appbar.MaterialToolbar
import com.htanh.appvk.R
import com.htanh.appvk.ui.applySystemBarInsets

class LearningExamResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning_exam_result)
        applySystemBarInsets(findViewById(R.id.examResultRoot))

        findViewById<MaterialToolbar>(R.id.examResultToolbar).apply {
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { getString(R.string.exam_result_title) }
            setNavigationOnClickListener { finish() }
        }
        val max = intent.getDoubleExtra(EXTRA_MAX_SCORE, 0.0)
        val earned = intent.getDoubleExtra(EXTRA_EARNED_SCORE, 0.0)
        findViewById<TextView>(R.id.tvExamResultScore).text =
            getString(R.string.exam_result_score, earned, max)
        findViewById<TextView>(R.id.tvExamResultSummary).text = getString(
            R.string.exam_result_summary,
            intent.getIntExtra(EXTRA_CORRECT, 0),
            intent.getIntExtra(EXTRA_INCORRECT, 0),
            intent.getIntExtra(EXTRA_UNANSWERED, 0)
        )
        val pending = intent.getIntExtra(EXTRA_PENDING_ESSAYS, 0)
        findViewById<TextView>(R.id.tvExamResultEssayStatus).text =
            if (pending > 0) getString(R.string.exam_result_pending_essays, pending)
            else getString(R.string.exam_result_no_pending_essays)
        findViewById<TextView>(R.id.tvExamResultMessage).text =
            if (intent.getBooleanExtra(EXTRA_AUTO_SUBMITTED, false)) {
                getString(R.string.exam_result_auto_submitted)
            } else {
                getString(R.string.exam_result_submitted)
            }
        findViewById<MaterialButton>(R.id.btnExamResultClose).setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_TITLE = "com.htanh.appvk.extra.RESULT_TITLE"
        const val EXTRA_MAX_SCORE = "com.htanh.appvk.extra.RESULT_MAX_SCORE"
        const val EXTRA_EARNED_SCORE = "com.htanh.appvk.extra.RESULT_EARNED_SCORE"
        const val EXTRA_CORRECT = "com.htanh.appvk.extra.RESULT_CORRECT"
        const val EXTRA_INCORRECT = "com.htanh.appvk.extra.RESULT_INCORRECT"
        const val EXTRA_UNANSWERED = "com.htanh.appvk.extra.RESULT_UNANSWERED"
        const val EXTRA_PENDING_ESSAYS = "com.htanh.appvk.extra.RESULT_PENDING_ESSAYS"
        const val EXTRA_AUTO_SUBMITTED = "com.htanh.appvk.extra.RESULT_AUTO_SUBMITTED"
    }
}
