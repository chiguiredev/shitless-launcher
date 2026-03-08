package com.shitless.launcher.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shitless.launcher.AppInfo
import com.shitless.launcher.DesignTokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isPinned: Boolean = false,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(vertical = DesignTokens.Spacing.Medium),
    ) {
        Text(
            text = if (isPinned) "• ${app.label}" else app.label,
            color = DesignTokens.Colors.Primary,
            fontSize = DesignTokens.FontSize.Medium,
        )
        Text(
            text = formatDuration(app.durationMs),
            color = DesignTokens.Colors.Secondary,
            fontSize = DesignTokens.FontSize.Small,
        )
    }
}

fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60
    return "%02dh %02dm %02ds".format(hours, minutes, seconds)
}
