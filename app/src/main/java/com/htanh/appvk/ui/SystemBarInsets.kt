package com.htanh.appvk.ui

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun Activity.applySystemBarInsets(root: View) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = android.graphics.Color.TRANSPARENT
    window.navigationBarColor = android.graphics.Color.TRANSPARENT
    WindowInsetsControllerCompat(window, root).apply {
        isAppearanceLightStatusBars = true
        isAppearanceLightNavigationBars = true
    }

    val initialLeft = root.paddingLeft
    val initialTop = root.paddingTop
    val initialRight = root.paddingRight
    val initialBottom = root.paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val systemBars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.setPadding(
            initialLeft + systemBars.left,
            initialTop + systemBars.top,
            initialRight + systemBars.right,
            initialBottom + systemBars.bottom
        )
        insets
    }
    ViewCompat.requestApplyInsets(root)
}
