package com.htanh.appvk.ui.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htanh.appvk.data.repository.LearningContentRepository
import com.htanh.appvk.data.repository.LearningContentSyncResult
import com.htanh.appvk.domain.learning.LearningCatalog
import com.htanh.appvk.domain.learning.QuestionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LearningLevel { SUBJECTS, CHAPTERS, TOPICS, QUESTIONS, EXAMS }

data class LearningUiState(
    val catalog: LearningCatalog = LearningCatalog(),
    val level: LearningLevel = LearningLevel.SUBJECTS,
    val selectedSubjectId: String? = null,
    val selectedChapterId: String? = null,
    val selectedTopicId: String? = null,
    val questionTypeFilter: QuestionType? = null,
    val isStandaloneQuestionList: Boolean = false,
    val isRefreshing: Boolean = false,
    val syncMessage: String? = null,
    val errorMessage: String? = null
)

class LearningViewModel(
    private val repository: LearningContentRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(LearningUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCatalog().collect { catalog ->
                mutableState.update { it.copy(catalog = catalog) }
            }
        }
        refresh()
    }

    fun refresh() {
        // Set the guard before launching. Otherwise rapid calls made before the
        // coroutine starts can each observe isRefreshing=false and queue duplicate API requests.
        if (mutableState.value.isRefreshing) return
        mutableState.update {
            it.copy(
                isRefreshing = true,
                errorMessage = null,
                syncMessage = "Đang đồng bộ học liệu…"
            )
        }

        viewModelScope.launch {
            try {
                when (val result = repository.refresh()) {
                    is LearningContentSyncResult.Success -> mutableState.update {
                        it.copy(
                            syncMessage = "Đồng bộ học liệu thành công.",
                            errorMessage = null
                        )
                    }
                    is LearningContentSyncResult.Failure -> mutableState.update {
                        it.copy(
                            syncMessage = if (it.catalog.subjects.isNotEmpty()) {
                                "Không thể cập nhật. Đang hiển thị dữ liệu đã lưu."
                            } else null,
                            errorMessage = result.message
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        syncMessage = if (it.catalog.subjects.isNotEmpty()) {
                            "Không thể cập nhật. Đang hiển thị dữ liệu đã lưu."
                        } else null,
                        errorMessage = error.message ?: "Không thể đồng bộ học liệu."
                    )
                }
            } finally {
                mutableState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun selectSubject(subjectId: String) {
        mutableState.update {
            it.copy(
                level = LearningLevel.CHAPTERS,
                selectedSubjectId = subjectId,
                selectedChapterId = null,
                selectedTopicId = null,
                questionTypeFilter = null,
                isStandaloneQuestionList = false
            )
        }
    }

    fun selectChapter(chapterId: String) {
        mutableState.update {
            it.copy(
                level = LearningLevel.TOPICS,
                selectedChapterId = chapterId,
                selectedTopicId = null
            )
        }
    }

    fun selectTopic(topicId: String) {
        mutableState.update {
            it.copy(
                level = LearningLevel.QUESTIONS,
                selectedTopicId = topicId,
                questionTypeFilter = null,
                isStandaloneQuestionList = false
            )
        }
    }

    fun showExams() {
        mutableState.update { it.copy(level = LearningLevel.EXAMS, selectedSubjectId = null, selectedChapterId = null, selectedTopicId = null, questionTypeFilter = null, isStandaloneQuestionList = false) }
    }

    fun showQuestions(type: QuestionType? = null) {
        mutableState.update {
            it.copy(
                level = LearningLevel.QUESTIONS,
                selectedSubjectId = null,
                selectedChapterId = null,
                selectedTopicId = null,
                questionTypeFilter = type,
                isStandaloneQuestionList = true
            )
        }
    }

    fun navigateBack() {
        mutableState.update { current ->
            when (current.level) {
                LearningLevel.SUBJECTS -> current
                LearningLevel.CHAPTERS -> current.copy(
                    level = LearningLevel.SUBJECTS,
                    selectedSubjectId = null
                )
                LearningLevel.TOPICS -> current.copy(
                    level = LearningLevel.CHAPTERS,
                    selectedChapterId = null
                )
                LearningLevel.EXAMS -> current.copy(level = LearningLevel.SUBJECTS)
                LearningLevel.QUESTIONS -> if (current.isStandaloneQuestionList) {
                    current.copy(
                        level = LearningLevel.SUBJECTS,
                        selectedSubjectId = null,
                        selectedChapterId = null,
                        selectedTopicId = null,
                        questionTypeFilter = null,
                        isStandaloneQuestionList = false
                    )
                } else current.copy(
                    level = LearningLevel.TOPICS,
                    selectedTopicId = null
                )
            }
        }
    }
}
