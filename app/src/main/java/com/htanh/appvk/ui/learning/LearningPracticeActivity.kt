package com.htanh.appvk.ui.learning

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.view.Gravity
import android.view.ViewGroup
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
    private lateinit var questionImage: ImageView
    private lateinit var optionsGroup: LinearLayout
    private lateinit var essayAnswer: EditText
    private lateinit var essayNotice: TextView
    private lateinit var checkButton: MaterialButton
    private lateinit var feedback: TextView
    private var renderedQuestionId: String? = null
    private var currentQuestion: LearningQuestion? = null
    private var currentOptions: List<QuestionOption> = emptyList()
    private var selectedOptionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning_practice)
        applySystemBarInsets(findViewById(R.id.practiceRoot))

        questionTitle = findViewById(R.id.tvPracticeTitle)
        questionMetadata = findViewById(R.id.tvPracticeMetadata)
        questionContent = findViewById(R.id.tvPracticeContent)
        questionImage = findViewById(R.id.ivPracticeQuestionImage)
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
        ) + question.estimatedTimeSeconds?.let { " • ${it}s" }.orEmpty()
        questionContent.text = question.content
        LearningImageLoader.load(questionImage, question.imageUrl, lifecycleScope)
        questionImage.visibility = if (LearningImageLoader.supportsUrl(question.imageUrl)) View.VISIBLE else View.GONE
        feedback.visibility = View.GONE

        val isEssay = question.type == QuestionType.ESSAY
        optionsGroup.visibility = if (isEssay) View.GONE else View.VISIBLE
        essayAnswer.visibility = if (isEssay) View.VISIBLE else View.GONE
        essayNotice.visibility = if (isEssay) View.VISIBLE else View.GONE
        checkButton.visibility = if (isEssay) View.GONE else View.VISIBLE

        if (renderedQuestionId != question.id) {
            renderedQuestionId = question.id
            selectedOptionId = null
            optionsGroup.removeAllViews()
            options.forEach { option ->
                val optionRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(8, 4, 8, 4)
                }
                val radioButton = RadioButton(this).apply {
                    id = View.generateViewId()
                    tag = option.id
                    text = listOfNotNull(option.label?.takeIf(String::isNotBlank), option.text).joinToString(". ")
                    textSize = 16f
                    setPadding(8, 12, 8, 12)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener { selectOption(option.id) }
                }
                optionRow.addView(radioButton)

                if (LearningImageLoader.supportsUrl(option.imageUrl)) {
                    val optionImage = ImageView(this).apply {
                        contentDescription = option.label?.let { getString(R.string.practice_option_image_description, it) }
                            ?: getString(R.string.practice_option_image)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        layoutParams = LinearLayout.LayoutParams(dp(88), dp(72)).apply {
                            marginStart = dp(8)
                        }
                    }
                    optionRow.addView(optionImage)
                    LearningImageLoader.load(optionImage, option.imageUrl, lifecycleScope)
                }
                optionRow.setOnClickListener { selectOption(option.id) }
                optionsGroup.addView(optionRow)
            }
            essayAnswer.text?.clear()
        }
    }

    private fun checkSelectedAnswer() {
        val question = currentQuestion ?: return
        val selectedId = selectedOptionId
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
        questionImage.visibility = View.GONE
        essayAnswer.visibility = View.GONE
        essayNotice.visibility = View.GONE
        checkButton.visibility = View.GONE
    }

    private fun selectOption(optionId: String) {
        selectedOptionId = optionId
        for (index in 0 until optionsGroup.childCount) {
            val row = optionsGroup.getChildAt(index) as? LinearLayout ?: continue
            val radio = row.getChildAt(0) as? RadioButton ?: continue
            radio.isChecked = radio.tag == optionId
        }
        feedback.visibility = View.GONE
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_QUESTION_ID = "com.htanh.appvk.extra.QUESTION_ID"
    }
}
