package com.htanh.appvk.ui.learning

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.htanh.appvk.HtavkApplication
import com.htanh.appvk.R
import com.htanh.appvk.domain.learning.Chapter
import com.htanh.appvk.domain.learning.LearningQuestion
import com.htanh.appvk.domain.learning.QuestionType
import com.htanh.appvk.domain.learning.Subject
import com.htanh.appvk.domain.learning.Topic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class LearningActivity : AppCompatActivity() {
    private lateinit var viewModel: LearningViewModel
    private lateinit var titleView: TextView
    private lateinit var syncStatusView: TextView
    private lateinit var errorView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var itemsContainer: LinearLayout
    private lateinit var backButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning)

        titleView = findViewById(R.id.tvLearningTitle)
        syncStatusView = findViewById(R.id.tvSyncStatus)
        errorView = findViewById(R.id.tvLearningError)
        progressBar = findViewById(R.id.learningProgress)
        itemsContainer = findViewById(R.id.learningItemsContainer)
        backButton = findViewById(R.id.btnLearningBack)

        val repository = (application as HtavkApplication).learningContentRepository
        viewModel = ViewModelProvider(this, LearningViewModelFactory(repository))[LearningViewModel::class.java]

        findViewById<MaterialButton>(R.id.btnLearningRefresh).setOnClickListener { viewModel.refresh() }
        backButton.setOnClickListener { viewModel.navigateBack() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.state.value.level == LearningLevel.SUBJECTS) {
                    finish()
                } else {
                    viewModel.navigateBack()
                }
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: LearningUiState) {
        val catalog = state.catalog
        titleView.text = when (state.level) {
            LearningLevel.SUBJECTS -> getString(R.string.learning_subjects_title)
            LearningLevel.CHAPTERS -> catalog.subjects.firstOrNull { it.id == state.selectedSubjectId }?.name
                ?: getString(R.string.learning_chapters_title)
            LearningLevel.TOPICS -> catalog.chapters.firstOrNull { it.id == state.selectedChapterId }?.name
                ?: getString(R.string.learning_topics_title)
            LearningLevel.QUESTIONS -> catalog.topics.firstOrNull { it.id == state.selectedTopicId }?.name
                ?: getString(R.string.learning_questions_title)
        }
        backButton.visibility = if (state.level == LearningLevel.SUBJECTS) View.GONE else View.VISIBLE
        progressBar.visibility = if (state.isRefreshing) View.VISIBLE else View.GONE

        val lastSync = catalog.lastSuccessfulSyncAt?.let {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it))
        }
        syncStatusView.text = buildString {
            append(state.syncMessage ?: getString(R.string.learning_sync_not_started))
            if (lastSync != null) {
                append("\n")
                append(getString(R.string.learning_last_sync, lastSync))
            }
        }
        errorView.text = state.errorMessage.orEmpty()
        errorView.visibility = if (state.errorMessage.isNullOrBlank()) View.GONE else View.VISIBLE

        itemsContainer.removeAllViews()
        when (state.level) {
            LearningLevel.SUBJECTS -> {
                val items = catalog.subjects.filter { it.isActive }
                items.forEach { subject ->
                    addItem(subject.name, subject.description, "${catalog.chapters.count { it.subjectId == subject.id && it.isActive }} chương") {
                        viewModel.selectSubject(subject.id)
                    }
                }
                showEmptyIfNeeded(items.isEmpty())
            }
            LearningLevel.CHAPTERS -> {
                val items = catalog.chapters.filter { it.isActive && it.subjectId == state.selectedSubjectId }
                items.forEach { chapter ->
                    addItem(chapter.name, chapter.description, "${catalog.topics.count { it.chapterId == chapter.id && it.isActive }} chủ đề") {
                        viewModel.selectChapter(chapter.id)
                    }
                }
                showEmptyIfNeeded(items.isEmpty())
            }
            LearningLevel.TOPICS -> {
                val items = catalog.topics.filter { it.isActive && it.chapterId == state.selectedChapterId }
                items.forEach { topic ->
                    addItem(topic.name, topic.description, "${catalog.questions.count { it.topicId == topic.id && it.isActive }} câu hỏi") {
                        viewModel.selectTopic(topic.id)
                    }
                }
                showEmptyIfNeeded(items.isEmpty())
            }
            LearningLevel.QUESTIONS -> {
                val items = catalog.questions.filter { it.isActive && it.topicId == state.selectedTopicId }
                items.forEach { question -> addQuestion(question, catalog.options.filter { it.questionId == question.id }) }
                showEmptyIfNeeded(items.isEmpty())
            }
        }
    }

    private fun addItem(title: String, description: String, metadata: String, onClick: () -> Unit) {
        val card = MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(1).toFloat()
            strokeWidth = dp(1)
            setStrokeColor(ColorStateListCompat.stroke())
            setCardBackgroundColor(Color.WHITE)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        content.addView(textView(title, 17f, true, "#172033"))
        if (description.isNotBlank()) {
            content.addView(textView(description, 14f, false, "#667085").apply {
                setPadding(0, dp(5), 0, 0)
            })
        }
        content.addView(textView(metadata, 12f, true, "#5263C5").apply {
            setPadding(0, dp(9), 0, 0)
        })
        card.addView(content)
        itemsContainer.addView(card)
    }

    private fun addQuestion(question: LearningQuestion, options: List<com.htanh.appvk.domain.learning.QuestionOption>) {
        val card = MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(1).toFloat()
            strokeWidth = dp(1)
            setStrokeColor(ColorStateListCompat.stroke())
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val typeLabel = if (question.type == QuestionType.MCQ) "Trắc nghiệm" else "Tự luận"
        content.addView(textView("$typeLabel • ${question.difficulty.name} • ${question.points} điểm", 12f, true, "#5263C5"))
        content.addView(textView(question.content, 16f, true, "#172033").apply {
            setPadding(0, dp(8), 0, dp(6))
        })
        options.forEach { option ->
            val prefix = if (option.isCorrect) "✓ " else "• "
            content.addView(textView(prefix + option.text, 14f, option.isCorrect, if (option.isCorrect) "#16834A" else "#475467").apply {
                setPadding(dp(4), dp(3), 0, dp(3))
            })
        }
        if (question.explanation.isNotBlank()) {
            content.addView(textView(question.explanation, 13f, false, "#667085").apply {
                setPadding(0, dp(8), 0, 0)
            })
        }
        card.addView(content)
        itemsContainer.addView(card)
    }

    private fun showEmptyIfNeeded(isEmpty: Boolean) {
        if (isEmpty) {
            itemsContainer.addView(textView(getString(R.string.learning_empty), 14f, false, "#667085"))
        }
    }

    private fun textView(text: String, size: Float, bold: Boolean, color: String) = TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        setTextColor(Color.parseColor(color))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private class LearningViewModelFactory(
        private val repository: com.htanh.appvk.data.repository.LearningContentRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LearningViewModel::class.java)) {
                return LearningViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    private object ColorStateListCompat {
        fun stroke() = android.content.res.ColorStateList.valueOf(Color.parseColor("#E8EBF2"))
    }
}
