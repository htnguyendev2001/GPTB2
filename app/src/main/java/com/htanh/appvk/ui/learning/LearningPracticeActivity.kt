package com.htanh.appvk.ui.learning

import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.htanh.appvk.HtavkApplication
import com.htanh.appvk.R
import com.htanh.appvk.data.repository.ExamAttemptRepository
import com.htanh.appvk.domain.learning.AnswerEvaluator
import com.htanh.appvk.domain.learning.ExamSessionRules
import com.htanh.appvk.domain.learning.LearningQuestion
import com.htanh.appvk.domain.learning.QuestionOption
import com.htanh.appvk.domain.learning.QuestionType
import com.htanh.appvk.domain.learning.SessionAnswer
import com.htanh.appvk.ui.applySystemBarInsets
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.util.Locale

class LearningPracticeActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar
    private lateinit var timerView: TextView
    private lateinit var questionMetadata: TextView
    private lateinit var questionContent: TextView
    private lateinit var questionImage: ImageView
    private lateinit var optionsGroup: LinearLayout
    private lateinit var essayAnswer: EditText
    private lateinit var essayNotice: TextView
    private lateinit var checkButton: MaterialButton
    private lateinit var submitButton: MaterialButton
    private lateinit var feedback: TextView
    private lateinit var previousButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var questionPosition: TextView
    private lateinit var swipeDetector: GestureDetector

    private var questionIds: List<String> = emptyList()
    private var currentIndex = 0
    private var examQuestionScores: Map<String, Double> = emptyMap()
    private var catalogQuestions: Map<String, Pair<LearningQuestion, List<QuestionOption>>> = emptyMap()
    private var currentQuestion: LearningQuestion? = null
    private var currentOptions: List<QuestionOption> = emptyList()
    private var selectedOptionId: String? = null
    private val selectedAnswers = mutableMapOf<String, String>()
    private val essayDrafts = mutableMapOf<String, String>()
    private val checkedQuestions = mutableSetOf<String>()
    private var attemptRepository: ExamAttemptRepository? = null
    private var attemptId: String? = null
    private var deadlineAtEpochMillis: Long? = null
    private var timerJob: Job? = null
    private var sessionInitialized = false
    private var sessionSubmitted = false

    private val isExam: Boolean get() = intent.getStringExtra(EXTRA_SESSION_TYPE) == "EXAM"
    private val isTimedExam: Boolean get() = isExam && intent.getIntExtra(EXTRA_DURATION_MINUTES, 0) > 0
    private val examId: String get() = intent.getStringExtra(EXTRA_EXAM_ID).orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning_practice)
        applySystemBarInsets(findViewById(R.id.practiceRoot))

        toolbar = findViewById(R.id.practiceToolbar)
        timerView = findViewById(R.id.tvExamTimer)
        questionMetadata = findViewById(R.id.tvPracticeMetadata)
        questionContent = findViewById(R.id.tvPracticeContent)
        questionImage = findViewById(R.id.ivPracticeQuestionImage)
        optionsGroup = findViewById(R.id.practiceOptions)
        essayAnswer = findViewById(R.id.etEssayAnswer)
        essayNotice = findViewById(R.id.tvEssayNotice)
        checkButton = findViewById(R.id.btnCheckAnswer)
        submitButton = findViewById(R.id.btnSubmitSession)
        feedback = findViewById(R.id.tvAnswerFeedback)
        previousButton = findViewById(R.id.btnPreviousQuestion)
        nextButton = findViewById(R.id.btnNextQuestion)
        questionPosition = findViewById(R.id.tvQuestionPosition)

        val startQuestionId = intent.getStringExtra(EXTRA_QUESTION_ID)
        val suppliedIds = intent.getStringArrayListExtra(EXTRA_QUESTION_IDS).orEmpty()
        questionIds = suppliedIds.distinct().ifEmpty { listOfNotNull(startQuestionId) }
        val suppliedScores = intent.getStringArrayListExtra(EXTRA_QUESTION_SCORES).orEmpty()
        examQuestionScores = questionIds.mapIndexedNotNull { index, id ->
            suppliedScores.getOrNull(index)?.toDoubleOrNull()?.let { id to it }
        }.toMap()
        currentIndex = questionIds.indexOf(startQuestionId).takeIf { it >= 0 } ?: 0

        savedInstanceState?.let { state ->
            attemptId = state.getString(STATE_ATTEMPT_ID)
            deadlineAtEpochMillis = state.takeIf { it.containsKey(STATE_DEADLINE) }?.getLong(STATE_DEADLINE)
            currentIndex = state.getInt(STATE_CURRENT_INDEX, currentIndex).coerceIn(0, (questionIds.size - 1).coerceAtLeast(0))
            state.getStringArrayList(STATE_SELECTED_ANSWERS).orEmpty().forEach { entry ->
                val parts = entry.split(STATE_SEPARATOR, limit = 2)
                if (parts.size == 2) selectedAnswers[parts[0]] = parts[1]
            }
            state.getStringArrayList(STATE_ESSAY_DRAFTS).orEmpty().forEach { entry ->
                val parts = entry.split(STATE_SEPARATOR, limit = 2)
                if (parts.size == 2) essayDrafts[parts[0]] = parts[1]
            }
            sessionInitialized = attemptId != null || deadlineAtEpochMillis != null
        }

        intent.getStringExtra(EXTRA_SESSION_TITLE)?.takeIf(String::isNotBlank)?.let { toolbar.title = it }
        toolbar.setNavigationOnClickListener { requestLeaveOrSubmit() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = requestLeaveOrSubmit()
        })

        checkButton.setOnClickListener { checkSelectedAnswer() }
        submitButton.setOnClickListener { confirmSubmit(autoSubmitted = false) }
        previousButton.setOnClickListener { navigateBy(-1) }
        nextButton.setOnClickListener { navigateBy(1) }

        submitButton.text = if (isExam) getString(R.string.exam_submit) else getString(R.string.practice_finish)
        checkButton.visibility = if (isExam) View.GONE else View.VISIBLE
        timerView.visibility = if (isTimedExam) View.VISIBLE else View.GONE

        swipeDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val deltaX = e2.x - e1.x
                val deltaY = e2.y - e1.y
                if (abs(deltaX) < dp(72) || abs(deltaX) < abs(deltaY) * 1.3f) return false
                navigateBy(if (deltaX < 0) 1 else -1)
                return true
            }
        })

        attemptRepository = (application as HtavkApplication).examAttemptRepository
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                (application as HtavkApplication).learningContentRepository.observeCatalog().collect { catalog ->
                    catalogQuestions = catalog.questions.associate { question ->
                        question.id to (question to catalog.options.filter { it.questionId == question.id }.sortedBy { it.sortOrder })
                    }
                    renderCurrentQuestion()
                    if (!sessionInitialized && questionIds.all(catalogQuestions::containsKey)) {
                        initializeSession()
                    }
                }
            }
        }
        if (deadlineAtEpochMillis != null) startCountdown()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        saveCurrentDraft(persistRemotely = false)
        outState.putString(STATE_ATTEMPT_ID, attemptId)
        deadlineAtEpochMillis?.let { outState.putLong(STATE_DEADLINE, it) }
        outState.putInt(STATE_CURRENT_INDEX, currentIndex)
        outState.putStringArrayList(STATE_SELECTED_ANSWERS, ArrayList(selectedAnswers.map { "${it.key}$STATE_SEPARATOR${it.value}" }))
        outState.putStringArrayList(STATE_ESSAY_DRAFTS, ArrayList(essayDrafts.map { "${it.key}$STATE_SEPARATOR${it.value}" }))
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        saveCurrentDraft()
        super.onPause()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        swipeDetector.onTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

    private fun initializeSession() {
        if (sessionInitialized) return
        sessionInitialized = true
        val duration = intent.getIntExtra(EXTRA_DURATION_MINUTES, 0)
        deadlineAtEpochMillis = if (isTimedExam && duration > 0) System.currentTimeMillis() + duration * 60_000L else null

        if (isExam) {
            val repository = attemptRepository
            if (repository == null) {
                Toast.makeText(this, R.string.exam_firebase_not_configured, Toast.LENGTH_LONG).show()
            } else {
                runCatching {
                    repository.createAttempt(
                        exam = com.htanh.appvk.domain.learning.LearningExam(
                            id = examId,
                            name = intent.getStringExtra(EXTRA_SESSION_TITLE).orEmpty(),
                            description = "",
                            subjectId = "",
                            durationMinutes = duration,
                            type = com.htanh.appvk.domain.learning.ExamType.EXAM,
                            totalScore = intent.getDoubleExtra(EXTRA_TOTAL_SCORE, 0.0),
                            isActive = true
                        ),
                        questionIds = questionIds,
                        questionScores = examQuestionScores
                    )
                }.onSuccess { started ->
                    attemptId = started.id
                    deadlineAtEpochMillis = started.deadlineAtEpochMillis ?: deadlineAtEpochMillis
                }.onFailure {
                    Toast.makeText(this, R.string.exam_auth_required, Toast.LENGTH_LONG).show()
                }
            }
        }
        if (deadlineAtEpochMillis != null) startCountdown()
        updateTimer()
    }

    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive && !sessionSubmitted) {
                    val remaining = (deadlineAtEpochMillis ?: break) - System.currentTimeMillis()
                    updateTimer()
                    if (remaining <= 0L) {
                        Toast.makeText(this@LearningPracticeActivity, R.string.exam_time_expired, Toast.LENGTH_LONG).show()
                        submitSession(autoSubmitted = true)
                        break
                    }
                    delay(1_000L)
                }
            }
        }
    }

    private fun updateTimer() {
        val deadline = deadlineAtEpochMillis ?: return
        val seconds = ((deadline - System.currentTimeMillis()).coerceAtLeast(0L) / 1_000L)
        val minutesPart = seconds / 60L
        val secondsPart = seconds % 60L
        timerView.text = getString(R.string.exam_timer_label, String.format(Locale.getDefault(), "%02d:%02d", minutesPart, secondsPart))
        timerView.setTextColor(ContextCompat.getColor(this, if (seconds <= 60L) R.color.practice_feedback_error else R.color.practice_feedback_success))
    }

    private fun navigateBy(offset: Int) {
        if (sessionSubmitted) return
        val nextIndex = currentIndex + offset
        if (nextIndex !in questionIds.indices) return
        saveCurrentDraft()
        currentIndex = nextIndex
        renderCurrentQuestion()
    }

    private fun saveCurrentDraft(persistRemotely: Boolean = true) {
        val question = currentQuestion ?: return
        val answer = if (question.type == QuestionType.ESSAY) {
            SessionAnswer(question.id, essayText = essayAnswer.text?.toString().orEmpty())
                .also { essayDrafts[question.id] = it.essayText.orEmpty() }
        } else {
            SessionAnswer(question.id, selectedOptionId = selectedOptionId)
                .also { selectedOptionId?.let { selectedAnswers[question.id] = it } }
        }
        val id = attemptId
        if (persistRemotely && isExam && id != null) {
            attemptRepository?.saveAnswer(
                id, examId, answer, question.type.name, examQuestionScores[question.id] ?: question.points
            )?.addOnFailureListener {
                Toast.makeText(applicationContext, R.string.exam_answer_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderCurrentQuestion() {
        val questionId = questionIds.getOrNull(currentIndex)
        val entry = questionId?.let(catalogQuestions::get)
        if (entry == null) {
            showLoadError(if (questionId == null) getString(R.string.practice_question_missing) else getString(R.string.practice_question_unavailable))
            updateNavigation()
            return
        }

        val (question, options) = entry
        currentQuestion = question
        currentOptions = options
        selectedOptionId = selectedAnswers[question.id]
        questionMetadata.visibility = View.VISIBLE
        val displayedScore = examQuestionScores[question.id] ?: question.points
        questionMetadata.text = getString(R.string.practice_question_metadata, question.difficulty.name, displayedScore.toString()) +
            question.estimatedTimeSeconds?.let { " • ${it}s" }.orEmpty()
        questionContent.text = question.content
        LearningImageLoader.load(questionImage, question.imageUrl, lifecycleScope)
        questionImage.visibility = if (LearningImageLoader.supportsUrl(question.imageUrl)) View.VISIBLE else View.GONE

        val isEssay = question.type == QuestionType.ESSAY
        optionsGroup.visibility = if (isEssay) View.GONE else View.VISIBLE
        essayAnswer.visibility = if (isEssay) View.VISIBLE else View.GONE
        essayNotice.visibility = if (isEssay) View.VISIBLE else View.GONE
        essayNotice.text = if (isExam) getString(R.string.exam_essay_notice) else getString(R.string.practice_essay_draft_notice)
        essayAnswer.isEnabled = !sessionSubmitted
        checkButton.visibility = if (isEssay || isExam) View.GONE else View.VISIBLE
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
                isEnabled = !sessionSubmitted
                setOnClickListener { selectOption(option.id) }
            }
            optionRow.addView(radioButton)
            if (LearningImageLoader.supportsUrl(option.imageUrl)) {
                val optionImage = ImageView(this).apply {
                    contentDescription = option.label?.let { getString(R.string.practice_option_image_description, it) }
                        ?: getString(R.string.practice_option_image)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = LinearLayout.LayoutParams(dp(88), dp(72)).apply { marginStart = dp(8) }
                }
                optionRow.addView(optionImage)
                LearningImageLoader.load(optionImage, option.imageUrl, lifecycleScope)
            }
            optionRow.setOnClickListener { if (!sessionSubmitted) selectOption(option.id) }
            optionsGroup.addView(optionRow)
        }

        if (!isExam && question.id in checkedQuestions && selectedOptionId != null) checkSelectedAnswer()
        else feedback.visibility = View.GONE
        updateNavigation()
    }

    private fun updateNavigation() {
        val total = questionIds.size
        questionPosition.text = if (total == 0) "0/0" else getString(R.string.practice_question_position, currentIndex + 1, total)
        previousButton.isEnabled = !sessionSubmitted && currentIndex > 0
        nextButton.isEnabled = !sessionSubmitted && currentIndex < total - 1
        submitButton.isEnabled = !sessionSubmitted
        nextButton.text = if (currentIndex == total - 1) getString(R.string.practice_last_question) else getString(R.string.practice_next_question)
    }

    private fun checkSelectedAnswer() {
        if (isExam || sessionSubmitted) return
        val question = currentQuestion ?: return
        val selectedId = selectedOptionId
        if (selectedId == null) {
            feedback.text = getString(R.string.practice_select_option)
            feedback.setTextColor(ContextCompat.getColor(this, R.color.practice_feedback_error))
            feedback.visibility = View.VISIBLE
            return
        }
        val evaluation = AnswerEvaluator.evaluate(selectedId, currentOptions) ?: return
        feedback.text = buildString {
            append(if (evaluation.isCorrect) getString(R.string.practice_answer_correct) else getString(R.string.practice_answer_incorrect))
            if (question.explanation.isNotBlank()) {
                append("\n\n")
                append(getString(R.string.practice_explanation, question.explanation))
            }
            if (!evaluation.isCorrect && !evaluation.correctOptionText.isNullOrBlank()) {
                append("\n\n")
                append(getString(R.string.practice_correct_answer, evaluation.correctOptionText))
            }
        }
        feedback.setTextColor(ContextCompat.getColor(this, if (evaluation.isCorrect) R.color.practice_feedback_success else R.color.practice_feedback_error))
        feedback.visibility = View.VISIBLE
        checkedQuestions += question.id
    }

    private fun selectOption(optionId: String) {
        if (sessionSubmitted) return
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

    private fun confirmSubmit(autoSubmitted: Boolean) {
        if (sessionSubmitted) return
        saveCurrentDraft()
        val unanswered = questionIds.count { id ->
            val question = catalogQuestions[id]?.first
            if (question?.type == QuestionType.ESSAY) essayDrafts[id].isNullOrBlank() else selectedAnswers[id].isNullOrBlank()
        }
        if (autoSubmitted) {
            submitSession(autoSubmitted = true)
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.exam_submit_confirm_title)
            .setMessage(getString(R.string.exam_submit_confirm_message, unanswered))
            .setNegativeButton(R.string.exam_submit_confirm_no, null)
            .setPositiveButton(R.string.exam_submit_confirm_yes) { _, _ -> submitSession(autoSubmitted = false) }
            .show()
    }

    private fun submitSession(autoSubmitted: Boolean) {
        if (sessionSubmitted) return
        saveCurrentDraft()
        sessionSubmitted = true
        timerJob?.cancel()
        val questions = questionIds.mapNotNull { catalogQuestions[it]?.first }
        val options = questions.flatMap { catalogQuestions[it.id]?.second.orEmpty() }
        val answers = questions.map { question ->
            if (question.type == QuestionType.ESSAY) {
                SessionAnswer(question.id, essayText = essayDrafts[question.id].orEmpty())
            } else {
                SessionAnswer(question.id, selectedOptionId = selectedAnswers[question.id])
            }
        }
        val result = ExamSessionRules.score(questions, options, answers.associateBy { it.questionId }, examQuestionScores)
        val attempt = attemptId
        if (isExam && attempt != null) {
            attemptRepository?.submitAttempt(
                attempt, answers, questions.associate { it.id to it.type.name }, examQuestionScores, result, autoSubmitted
            )?.addOnFailureListener {
                Toast.makeText(applicationContext, R.string.exam_submit_save_failed, Toast.LENGTH_LONG).show()
            }
        }

        startActivity(
            android.content.Intent(this, LearningExamResultActivity::class.java)
                .putExtra(LearningExamResultActivity.EXTRA_TITLE, intent.getStringExtra(EXTRA_SESSION_TITLE))
                .putExtra(LearningExamResultActivity.EXTRA_MAX_SCORE, result.maximumScore)
                .putExtra(LearningExamResultActivity.EXTRA_EARNED_SCORE, result.earnedAutomaticScore)
                .putExtra(LearningExamResultActivity.EXTRA_CORRECT, result.correctCount)
                .putExtra(LearningExamResultActivity.EXTRA_INCORRECT, result.incorrectCount)
                .putExtra(LearningExamResultActivity.EXTRA_UNANSWERED, result.unansweredCount)
                .putExtra(LearningExamResultActivity.EXTRA_PENDING_ESSAYS, result.pendingEssayCount)
                .putExtra(LearningExamResultActivity.EXTRA_AUTO_SUBMITTED, autoSubmitted)
        )
        finish()
    }

    private fun requestLeaveOrSubmit() {
        if (sessionSubmitted) {
            finish()
        } else if (isExam) {
            AlertDialog.Builder(this)
                .setTitle(R.string.exam_leave_confirm_title)
                .setMessage(R.string.exam_leave_confirm_message)
                .setNegativeButton(R.string.exam_submit_confirm_no, null)
                .setPositiveButton(R.string.exam_submit_confirm_yes) { _, _ -> submitSession(autoSubmitted = false) }
                .show()
        } else {
            saveCurrentDraft()
            finish()
        }
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_QUESTION_ID = "com.htanh.appvk.extra.QUESTION_ID"
        const val EXTRA_QUESTION_IDS = "com.htanh.appvk.extra.QUESTION_IDS"
        const val EXTRA_QUESTION_SCORES = "com.htanh.appvk.extra.QUESTION_SCORES"
        const val EXTRA_SESSION_TITLE = "com.htanh.appvk.extra.SESSION_TITLE"
        const val EXTRA_SESSION_TYPE = "com.htanh.appvk.extra.SESSION_TYPE"
        const val EXTRA_DURATION_MINUTES = "com.htanh.appvk.extra.DURATION_MINUTES"
        const val EXTRA_EXAM_ID = "com.htanh.appvk.extra.EXAM_ID"
        const val EXTRA_TOTAL_SCORE = "com.htanh.appvk.extra.TOTAL_SCORE"

        private const val STATE_ATTEMPT_ID = "attempt_id"
        private const val STATE_DEADLINE = "deadline_at"
        private const val STATE_CURRENT_INDEX = "current_index"
        private const val STATE_SELECTED_ANSWERS = "selected_answers"
        private const val STATE_ESSAY_DRAFTS = "essay_drafts"
        private const val STATE_SEPARATOR = "\u001f"
    }
}
