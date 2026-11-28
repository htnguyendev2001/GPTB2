package com.htanh.appvk.ui.learning

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.htanh.appvk.HtavkApplication
import com.htanh.appvk.R
import com.htanh.appvk.ui.applySystemBarInsets
import kotlinx.coroutines.launch

class LearningProgressActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning_progress)
        applySystemBarInsets(findViewById(R.id.progressRoot))

        findViewById<MaterialToolbar>(R.id.progressToolbar)
            .setNavigationOnClickListener { finish() }
        val summary = findViewById<TextView>(R.id.tvProgressSummary)
        val notice = findViewById<TextView>(R.id.tvProgressNotice)

        val repository = (application as HtavkApplication).learningContentRepository
        lifecycleScope.launch {
            repository.observeCatalog().collect { catalog ->
                summary.text = getString(
                    R.string.progress_catalog_summary,
                    catalog.subjects.count { it.isActive },
                    catalog.chapters.count { it.isActive },
                    catalog.topics.count { it.isActive },
                    catalog.questions.count { it.isActive }
                )
                notice.visibility = View.VISIBLE
            }
        }
    }
}