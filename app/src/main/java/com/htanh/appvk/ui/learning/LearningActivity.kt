package com.htanh.appvk.ui.learning

import android.content.Intent
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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.htanh.appvk.HtavkApplication
import com.htanh.appvk.R
import com.htanh.appvk.domain.learning.LearningQuestion
import com.htanh.appvk.domain.learning.QuestionType
import com.htanh.appvk.ui.applySystemBarInsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class LearningActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_QUESTION_FILTER = "com.htanh.appvk.extra.QUESTION_FILTER"
        const val FILTER_MCQ = "mcq"
        const val FILTER_ESSAY = "essay"
    }

    private lateinit var viewModel: LearningViewModel
    private lateinit var toolbar: MaterialToolbar
    private lateinit var syncStatusView: TextView
    private lateinit var errorView: TextView
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var itemsList: RecyclerView
    private lateinit var refreshButton: MaterialButton
    private lateinit var listAdapter: LearningListAdapter
    private var visibleQuestionIds: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning)
        applySystemBarInsets(findViewById(R.id.learningRoot))

        toolbar = findViewById(R.id.learningToolbar)
        syncStatusView = findViewById(R.id.tvSyncStatus)
        errorView = findViewById(R.id.tvLearningError)
        emptyView = findViewById(R.id.tvLearningEmpty)
        progressBar = findViewById(R.id.learningProgress)
        itemsList = findViewById(R.id.learningItemsList)
        refreshButton = findViewById(R.id.btnLearningRefresh)

        val repository = (application as HtavkApplication).learningContentRepository
        viewModel = ViewModelProvider(this, LearningViewModelFactory(repository))[LearningViewModel::class.java]

        listAdapter = LearningListAdapter { id ->
            when (viewModel.state.value.level) {
                LearningLevel.SUBJECTS -> viewModel.selectSubject(id)
                LearningLevel.CHAPTERS -> viewModel.selectChapter(id)
                LearningLevel.TOPICS -> viewModel.selectTopic(id)
                LearningLevel.QUESTIONS -> startActivity(
                    Intent(this, LearningPracticeActivity::class.java)
                        .putExtra(LearningPracticeActivity.EXTRA_QUESTION_ID, id)
                        .putStringArrayListExtra(
                            LearningPracticeActivity.EXTRA_QUESTION_IDS,
                            ArrayList(visibleQuestionIds)
                        )
                )
            }
        }
        itemsList.layoutManager = LinearLayoutManager(this)
        itemsList.adapter = listAdapter
        itemsList.setHasFixedSize(false)

        val requestedFilter = intent.getStringExtra(EXTRA_QUESTION_FILTER)
        when (requestedFilter) {
            FILTER_MCQ -> viewModel.showQuestions(QuestionType.MCQ)
            FILTER_ESSAY -> viewModel.showQuestions(QuestionType.ESSAY)
            else -> Unit
        }

        refreshButton.setOnClickListener { viewModel.refresh() }
        toolbar.setNavigationOnClickListener {
            val state = viewModel.state.value
            if (state.level == LearningLevel.SUBJECTS || state.isStandaloneQuestionList) finish()
            else viewModel.navigateBack()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val state = viewModel.state.value
                if (state.level == LearningLevel.SUBJECTS || state.isStandaloneQuestionList) finish()
                else viewModel.navigateBack()
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
        toolbar.title = when (state.level) {
            LearningLevel.SUBJECTS -> getString(R.string.learning_subjects_title)
            LearningLevel.CHAPTERS -> catalog.subjects.firstOrNull { it.id == state.selectedSubjectId }?.name
                ?: getString(R.string.learning_chapters_title)
            LearningLevel.TOPICS -> catalog.chapters.firstOrNull { it.id == state.selectedChapterId }?.name
                ?: getString(R.string.learning_topics_title)
            LearningLevel.QUESTIONS -> when (state.questionTypeFilter) {
                QuestionType.ESSAY -> getString(R.string.home_essay_title)
                QuestionType.MCQ -> getString(R.string.home_practice_title)
                null -> if (state.isStandaloneQuestionList) getString(R.string.learning_questions_title)
                    else catalog.topics.firstOrNull { it.id == state.selectedTopicId }?.name
                        ?: getString(R.string.learning_questions_title)
            }
        }
        progressBar.visibility = if (state.isRefreshing) View.VISIBLE else View.GONE
        refreshButton.isEnabled = !state.isRefreshing

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
                .filter { question ->
                    question.isActive &&
                        (state.isStandaloneQuestionList || question.topicId == state.selectedTopicId) &&
                        (state.questionTypeFilter == null || question.type == state.questionTypeFilter)
                }
                .map { question -> question.toListItem(catalog.options.count { it.questionId == question.id }) }
        }

        visibleQuestionIds = if (state.level == LearningLevel.QUESTIONS) rows.map { it.id } else emptyList()
        listAdapter.submitList(rows)
        emptyView.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        itemsList.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun LearningQuestion.toListItem(optionCount: Int): LearningListItem {
        val typeLabel = if (type == com.htanh.appvk.domain.learning.QuestionType.MCQ) {
            "Trắc nghiệm"
        } else {
            "Tự luận"
        }
        return LearningListItem(
            id = id,
            title = content,
            description = "",
            metadata = "$typeLabel • $optionCount lựa chọn • ${difficulty.name} • $points điểm",
            isNavigable = true
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
