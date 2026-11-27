package com.htanh.appvk.ui.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htanh.appvk.data.repository.LearningContentRepository
import com.htanh.appvk.data.repository.LearningContentSyncResult
import com.htanh.appvk.domain.learning.LearningCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LearningLevel { SUBJECTS, CHAPTERS, TOPICS, QUESTIONS }

data class LearningUiState(
    val catalog: LearningCatalog = LearningCatalog(),
    val level: LearningLevel = LearningLevel.SUBJECTS,
    val selectedSubjectId: String? = null,
    val selectedChapterId: String? = null,
    val selectedTopicId: String? = null,
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
        if (mutableState.value.isRefreshing) return
        viewModelScope.launch {
            mutableState.update { it.copy(isRefreshing = true, errorMessage = null, syncMessage = "Đang đồng bộ học liệu…") }
            when (val result = repository.refresh()) {
                is LearningContentSyncResult.Success -> mutableState.update {
                    it.copy(
                        isRefreshing = false,
                        syncMessage = "Đồng bộ học liệu thành công.",
                        errorMessage = null
                    )
                }
                is LearningContentSyncResult.Failure -> mutableState.update {
                    it.copy(
                        isRefreshing = false,
                        syncMessage = if (it.catalog.subjects.isNotEmpty()) {
                            "Không thể cập nhật. Đang hiển thị dữ liệu đã lưu."
                        } else null,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun selectSubject(subjectId: String) {
        mutableState.update {
            it.copy(
                level = LearningLevel.CHAPTERS,
                selectedSubjectId = subjectId,
                selectedChapterId = null,
                selectedTopicId = null
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
            it.copy(level = LearningLevel.QUESTIONS, selectedTopicId = topicId)
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
                LearningLevel.QUESTIONS -> current.copy(
                    level = LearningLevel.TOPICS,
                    selectedTopicId = null
                )
            }
        }
    }
}
