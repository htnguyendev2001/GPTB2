package com.htanh.appvk.ui.learning

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.htanh.appvk.HtavkApplication
import com.htanh.appvk.R
import com.htanh.appvk.domain.learning.LearningQuestion
import com.htanh.appvk.domain.learning.QuestionOption
import com.htanh.appvk.domain.learning.QuestionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class LearningActivity : AppCompatActivity() {
    private lateinit var viewModel: LearningViewModel
    private lateinit var titleView: TextView
    private lateinit var syncStatusView: TextView
    private lateinit var errorView: TextView
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var itemsList: RecyclerView
    private lateinit var backButton: MaterialButton
    private lateinit var listAdapter: LearningListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning)

        titleView = findViewById(R.id.tvLearningTitle)
        syncStatusView = findViewById(R.id.tvSyncStatus)
        errorView = findViewById(R.id.tvLearningError)
        emptyView = findViewById(R.id.tvLearningEmpty)
        progressBar = findViewById(R.id.learningProgress)
        itemsList = findViewById(R.id.learningItemsList)
        backButton = findViewById(R.id.btnLearningBack)

        val repository = (application as HtavkApplication).learningContentRepository
        viewModel = ViewModelProvider(this, LearningViewModelFactory(repository))[LearningViewModel::class.java]

        listAdapter = LearningListAdapter { id ->
            when (viewModel.state.value.level) {
                LearningLevel.SUBJECTS -> viewModel.selectSubject(id)
                LearningLevel.CHAPTERS -> viewModel.selectChapter(id)
                LearningLevel.TOPICS -> viewModel.selectTopic(id)
                LearningLevel.QUESTIONS -> Unit
            }
        }
        itemsList.layoutManager = LinearLayoutManager(this)
        itemsList.adapter = listAdapter
        itemsList.setHasFixedSize(false)

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

        val rows = when (state.level) {
            LearningLevel.SUBJECTS -> catalog.subjects
                .filter { it.isActive }
                .map { subject ->
                    LearningListItem(
                        id = subject.id,
                        title = subject.name,
                        description = subject.description,
                        metadata = "${catalog.chapters.count { it.subjectId == subject.id && it.isActive }} chương",
                        isNavigable = true
                    )
                }
            LearningLevel.CHAPTERS -> catalog.chapters
                .filter { it.isActive && it.subjectId == state.selectedSubjectId }
                .map { chapter ->
                    LearningListItem(
                        id = chapter.id,
                        title = chapter.name,
                        description = chapter.description,
                        metadata = "${catalog.topics.count { it.chapterId == chapter.id && it.isActive }} chủ đề",
                        isNavigable = true
                    )
                }
            LearningLevel.TOPICS -> catalog.topics
                .filter { it.isActive && it.chapterId == state.selectedChapterId }
                .map { topic ->
                    LearningListItem(
                        id = topic.id,
                        title = topic.name,
                        description = topic.description,
                        metadata = "${catalog.questions.count { it.topicId == topic.id && it.isActive }} câu hỏi",
                        isNavigable = true
                    )
                }
            LearningLevel.QUESTIONS -> catalog.questions
                .filter { it.isActive && it.topicId == state.selectedTopicId }
                .map { question ->
                    question.toListItem(catalog.options.filter { it.questionId == question.id })
                }
        }

        listAdapter.submitList(rows)
        emptyView.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        itemsList.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun LearningQuestion.toListItem(options: List<QuestionOption>): LearningListItem {
        val optionText = options.joinToString("\n") { option ->
            (if (option.isCorrect) "✓ " else "• ") + option.text
        }
        val description = listOf(optionText, explanation)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
        val typeLabel = if (type == QuestionType.MCQ) "Trắc nghiệm" else "Tự luận"
        return LearningListItem(
            id = id,
            title = content,
            description = description,
            metadata = "$typeLabel • ${difficulty.name} • $points điểm",
            isNavigable = false
        )
    }

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
}
