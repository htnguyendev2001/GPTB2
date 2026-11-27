package com.htanh.appvk

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.htanh.appvk.ui.applySystemBarInsets
import com.htanh.appvk.ui.learning.LearningActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySystemBarInsets(findViewById(R.id.mainRoot))

        findViewById<MaterialButton>(R.id.btnOpenLearning).setOnClickListener {
            startActivity(Intent(this, LearningActivity::class.java))
        }
    }
}
