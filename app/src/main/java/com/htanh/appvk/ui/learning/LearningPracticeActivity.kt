package com.htanh.appvk.ui.learning

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.htanh.appvk.HtavkApplication
import com.htanh.appvk.R
import com.htanh.appvk.domain.learning.AnswerEvaluator
import com.htanh.appvk.domain.learning.LearningQuestion
import com.htanh.appvk.domain.learning.QuestionOption
import com.htanh.appvk.domain.learning.QuestionType
import com.htanh.appvk.ui.applySystemBarInsets
import kotlinx.coroutines.launch

class LearningPracticeActivity : AppCompatActivity() {
    private lateinit var questionTitle: TextView
    private lateinit var questionMetadata: TextView
    private lateinit var questionContent: TextView
    private lateinit var optionsGroup: RadioGroup
    private lateinit var essayAnswer: EditText
    private lateinit var essayNotice: TextView
    private lateinit var checkButton: MaterialButton
    private lateinit var feedback: TextView
    private var renderedQuestionId: String? = null
    private var currentQuestion: LearningQuestion? = null
    private var currentOptions: List<QuestionOption> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning_practice)
        applySystemBarInsets(findViewById(R.id.practiceRoot))

        questionTitle = findViewById(R.id.tvPracticeTitle)
        questionMetadata = findViewById(R.id.tvPracticeMetadata)
        questionContent = findViewById(R.id.tvPracticeContent)
        optionsGroup = findViewById(R.id.practiceOptions)
        essayAnswer = findViewById(R.id.etEssayAnswer)
        essayNotice = findViewById(R.id.tvEssayNotice)
        checkButton = findViewById(R.id.btnCheckAnswer)
        feedback = findViewById(R.id.tvAnswerFeedback)

        findViewById<MaterialButton>(R.id.btnPracticeBack).setOnClickListener { finish() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finish()
        })
        checkButton.setOnClickListener { checkSelectedAnswer() }

        val questionId = intent.getStringExtra(EXTRA_QUESTION_ID)
        if (questionId.isNullOrBlank()) {
            showLoadError(getString(R.string.practice_question_missing))
            return
        }

        val repository = (application as HtavkApplication).learningContentRepository
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeCatalog().collect { catalog ->
                    val question = catalog.questions.firstOrNull { it.id == questionId }
                    if (question == null) {
                        showLoadError(getString(R.string.practice_question_unavailable))
                    } else {
                        val options = catalog.options.filter { it.questionId == question.id }
                            .sortedBy { it.sortOrder }
                        bindQuestion(question, options)
                    }
                }
            }
        }
    }

    private fun bindQuestion(question: LearningQuestion, options: List<QuestionOption>) {
        currentQuestion = question
        currentOptions = options
        questionTitle.text = getString(R.string.practice_question_title)
        questionMetadata.visibility = View.VISIBLE
        questionMetadata.text = getString(
            R.string.practice_question_metadata,
            question.difficulty.name,
            question.points.toString()
        )
        questionContent.text = question.content
        feedback.visibility = View.GONE

        val isEssay = question.type == QuestionType.ESSAY
        optionsGroup.visibility = if (isEssay) View.GONE else View.VISIBLE
        essayAnswer.visibility = if (isEssay) View.VISIBLE else View.GONE
        essayNotice.visibility = if (isEssay) View.VISIBLE else View.GONE
        checkButton.visibility = if (isEssay) View.GONE else View.VISIBLE

        if (renderedQuestionId != question.id) {
            renderedQuestionId = question.id
            optionsGroup.removeAllViews()
            options.forEach { option ->
                val radioButton = RadioButton(this).apply {
                    id = View.generateViewId()
                    tag = option.id
                    text = option.text
                    textSize = 16f
                    setPadding(8, 16, 8, 16)
                }
                optionsGroup.addView(radioButton)
            }
            essayAnswer.text?.clear()
        }
    }

    private fun checkSelectedAnswer() {
        val question = currentQuestion ?: return
        val selectedId = optionsGroup.findViewById<RadioButton>(optionsGroup.checkedRadioButtonId)?.tag as? String
        if (selectedId == null) {
            feedback.text = getString(R.string.practice_select_option)
            feedback.setTextColor(ContextCompat.getColor(this, R.color.practice_feedback_error))
            feedback.visibility = View.VISIBLE
            return
        }

        val evaluation = AnswerEvaluator.evaluate(selectedId, currentOptions) ?: return
        val message = buildString {
            append(
                if (evaluation.isCorrect) {
                    getString(R.string.practice_answer_correct)
                } else {
                    getString(R.string.practice_answer_incorrect)
                }
            )
            if (question.explanation.isNotBlank()) {
                append("\n\n")
                append(getString(R.string.practice_explanation, question.explanation))
            }
            if (!evaluation.isCorrect && !evaluation.correctOptionText.isNullOrBlank()) {
                append("\n\n")
                append(getString(R.string.practice_correct_answer, evaluation.correctOptionText))
            }
        }
        feedback.text = message
        feedback.setTextColor(
            ContextCompat.getColor(
                this,
                if (evaluation.isCorrect) R.color.practice_feedback_success else R.color.practice_feedback_error
            )
        )
        feedback.visibility = View.VISIBLE
    }

    private fun showLoadError(message: String) {
        questionTitle.text = getString(R.string.practice_question_title)
        questionContent.text = message
        questionMetadata.visibility = View.GONE
        optionsGroup.visibility = View.GONE
        essayAnswer.visibility = View.GONE
        essayNotice.visibility = View.GONE
        checkButton.visibility = View.GONE
    }

    companion object {
        const val EXTRA_QUESTION_ID = "com.htanh.appvk.extra.QUESTION_ID"
    }
}
