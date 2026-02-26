package com.example.launcher

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val durationMs: Long = 0L,
)
