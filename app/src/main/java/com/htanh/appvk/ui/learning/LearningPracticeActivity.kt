package com.htanh.appvk.ui.learning

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.htanh.appvk.HtavkApplication
import com.htanh.appvk.R
import com.htanh.appvk.domain.learning.AnswerEvaluator
import com.htanh.appvk.domain.learning.LearningQuestion
import com.htanh.appvk.domain.learning.QuestionOption
import com.htanh.appvk.domain.learning.QuestionType
import com.htanh.appvk.ui.applySystemBarInsets
import kotlinx.coroutines.launch
import kotlin.math.abs

class LearningPracticeActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar
    private lateinit var questionMetadata: TextView
    private lateinit var questionContent: TextView
    private lateinit var questionImage: ImageView
    private lateinit var optionsGroup: LinearLayout
    private lateinit var essayAnswer: EditText
    private lateinit var essayNotice: TextView
    private lateinit var checkButton: MaterialButton
    private lateinit var feedback: TextView
    private lateinit var previousButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var questionPosition: TextView
    private lateinit var swipeDetector: GestureDetector

    private var questionIds: List<String> = emptyList()
    private var currentIndex = 0
    private var catalogQuestions: Map<String, Pair<LearningQuestion, List<QuestionOption>>> = emptyMap()
    private var currentQuestion: LearningQuestion? = null
    private var currentOptions: List<QuestionOption> = emptyList()
    private var selectedOptionId: String? = null
    private val selectedAnswers = mutableMapOf<String, String>()
    private val essayDrafts = mutableMapOf<String, String>()
    private val checkedQuestions = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning_practice)
        applySystemBarInsets(findViewById(R.id.practiceRoot))

        toolbar = findViewById(R.id.practiceToolbar)
        questionMetadata = findViewById(R.id.tvPracticeMetadata)
        questionContent = findViewById(R.id.tvPracticeContent)
        questionImage = findViewById(R.id.ivPracticeQuestionImage)
        optionsGroup = findViewById(R.id.practiceOptions)
        essayAnswer = findViewById(R.id.etEssayAnswer)
        essayNotice = findViewById(R.id.tvEssayNotice)
        checkButton = findViewById(R.id.btnCheckAnswer)
        feedback = findViewById(R.id.tvAnswerFeedback)
        previousButton = findViewById(R.id.btnPreviousQuestion)
        nextButton = findViewById(R.id.btnNextQuestion)
        questionPosition = findViewById(R.id.tvQuestionPosition)

        val startQuestionId = intent.getStringExtra(EXTRA_QUESTION_ID)
        val suppliedIds = intent.getStringArrayListExtra(EXTRA_QUESTION_IDS).orEmpty()
        questionIds = suppliedIds.distinct().ifEmpty { listOfNotNull(startQuestionId) }
        currentIndex = questionIds.indexOf(startQuestionId).takeIf { it >= 0 } ?: 0

        toolbar.setNavigationOnClickListener { finish() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finish()
        })
        checkButton.setOnClickListener { checkSelectedAnswer() }
        previousButton.setOnClickListener { navigateBy(-1) }
        nextButton.setOnClickListener { navigateBy(1) }

        swipeDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val deltaX = e2.x - e1.x
                val deltaY = e2.y - e1.y
                if (abs(deltaX) < dp(72) || abs(deltaX) < abs(deltaY) * 1.3f) return false
                navigateBy(if (deltaX < 0) 1 else -1)
                return true
            }
        })

        val repository = (application as HtavkApplication).learningContentRepository
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeCatalog().collect { catalog ->
                    catalogQuestions = catalog.questions.associate { question ->
                        question.id to (question to catalog.options
                            .filter { it.questionId == question.id }
                            .sortedBy { it.sortOrder })
                    }
                    renderCurrentQuestion()
                }
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        swipeDetector.onTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

    private fun navigateBy(offset: Int) {
        val nextIndex = currentIndex + offset
        if (nextIndex !in questionIds.indices) return
        saveCurrentDraft()
        currentIndex = nextIndex
        renderCurrentQuestion()
    }

    private fun saveCurrentDraft() {
        val questionId = currentQuestion?.id ?: return
        selectedOptionId?.let { selectedAnswers[questionId] = it }
        if (currentQuestion?.type == QuestionType.ESSAY) {
            essayDrafts[questionId] = essayAnswer.text?.toString().orEmpty()
        }
    }

    private fun renderCurrentQuestion() {
        val questionId = questionIds.getOrNull(currentIndex)
        val entry = questionId?.let(catalogQuestions::get)
        if (entry == null) {
            showLoadError(
                if (questionId == null) getString(R.string.practice_question_missing)
                else getString(R.string.practice_question_unavailable)
            )
            updateNavigation()
            return
        }

        val (question, options) = entry
        currentQuestion = question
        currentOptions = options
        selectedOptionId = selectedAnswers[question.id]
        questionMetadata.visibility = View.VISIBLE
        questionMetadata.text = getString(
            R.string.practice_question_metadata,
            question.difficulty.name,
            question.points.toString()
        ) + question.estimatedTimeSeconds?.let { " • ${it}s" }.orEmpty()
        questionContent.text = question.content
        LearningImageLoader.load(questionImage, question.imageUrl, lifecycleScope)
        questionImage.visibility = if (LearningImageLoader.supportsUrl(question.imageUrl)) View.VISIBLE else View.GONE

        val isEssay = question.type == QuestionType.ESSAY
        optionsGroup.visibility = if (isEssay) View.GONE else View.VISIBLE
        essayAnswer.visibility = if (isEssay) View.VISIBLE else View.GONE
        essayNotice.visibility = if (isEssay) View.VISIBLE else View.GONE
        checkButton.visibility = if (isEssay) View.GONE else View.VISIBLE
        essayAnswer.setText(essayDrafts[question.id].orEmpty())

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
                isChecked = option.id == selectedOptionId
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

        if (question.id in checkedQuestions && selectedOptionId != null) {
            checkSelectedAnswer()
        } else {
            feedback.visibility = View.GONE
        }
        updateNavigation()
    }

    private fun updateNavigation() {
        val total = questionIds.size
        questionPosition.text = if (total == 0) "0/0" else getString(R.string.practice_question_position, currentIndex + 1, total)
        previousButton.isEnabled = currentIndex > 0
        nextButton.isEnabled = currentIndex < total - 1
        nextButton.text = if (currentIndex == total - 1) {
            getString(R.string.practice_last_question)
        } else {
            getString(R.string.practice_next_question)
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
        checkedQuestions += question.id
    }

    private fun showLoadError(message: String) {
        currentQuestion = null
        currentOptions = emptyList()
        selectedOptionId = null
        optionsGroup.removeAllViews()
        feedback.visibility = View.GONE
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
        currentQuestion?.id?.let { selectedAnswers[it] = optionId }
        for (index in 0 until optionsGroup.childCount) {
            val row = optionsGroup.getChildAt(index) as? LinearLayout ?: continue
            val radio = row.getChildAt(0) as? RadioButton ?: continue
            radio.isChecked = radio.tag == optionId
        }
        feedback.visibility = View.GONE
        currentQuestion?.id?.let(checkedQuestions::remove)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_QUESTION_ID = "com.htanh.appvk.extra.QUESTION_ID"
        const val EXTRA_QUESTION_IDS = "com.htanh.appvk.extra.QUESTION_IDS"
    }
}
