package com.htanh.appvk.domain.learning

data class AnswerEvaluation(
    val isCorrect: Boolean,
    val correctOptionText: String?
)

object AnswerEvaluator {
    fun evaluate(selectedOptionId: String, options: List<QuestionOption>): AnswerEvaluation? {
        val selected = options.firstOrNull { it.id == selectedOptionId } ?: return null
        return AnswerEvaluation(
            isCorrect = selected.isCorrect,
            correctOptionText = options.firstOrNull { it.isCorrect }?.text
        )
    }
}
