package com.htanh.appvk

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.htanh.appvk.ui.applySystemBarInsets
import com.htanh.appvk.ui.learning.LearningActivity
import com.htanh.appvk.ui.learning.LearningProgressActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySystemBarInsets(findViewById(R.id.mainRoot))

        findViewById<MaterialButton>(R.id.btnOpenLearning).setOnClickListener {
            startActivity(Intent(this, LearningActivity::class.java))
        }
        findViewById<View>(R.id.cardLearning).setOnClickListener {
            startActivity(
                Intent(this, LearningActivity::class.java)
                    .putExtra(LearningActivity.EXTRA_QUESTION_FILTER, LearningActivity.FILTER_MCQ)
            )
        }
        findViewById<View>(R.id.cardPractice).setOnClickListener {
            startActivity(
                Intent(this, LearningActivity::class.java)
                    .putExtra(LearningActivity.EXTRA_QUESTION_FILTER, LearningActivity.FILTER_ESSAY)
            )
        }
        findViewById<View>(R.id.cardProgress).setOnClickListener {
            startActivity(Intent(this, LearningProgressActivity::class.java))
        }
    }
}
