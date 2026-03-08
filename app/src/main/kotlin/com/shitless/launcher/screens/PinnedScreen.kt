package com.shitless.launcher.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.shitless.launcher.AppInfo
import com.shitless.launcher.DesignTokens
import com.shitless.launcher.components.AppRow

@Composable
fun PinnedScreen(
    apps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    time: String,
    battery: String,
    topPadding: Dp,
    bottomPadding: Dp,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = topPadding, bottom = bottomPadding)
                .padding(horizontal = DesignTokens.Spacing.Large),
    ) {
        Spacer(Modifier.height(DesignTokens.Spacing.Large))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = time, color = DesignTokens.Colors.Primary, fontSize = DesignTokens.FontSize.Large)
            Text(text = battery, color = DesignTokens.Colors.Primary, fontSize = DesignTokens.FontSize.Large)
        }
        Spacer(Modifier.height(DesignTokens.Spacing.Large))
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = DesignTokens.Colors.Primary,
            modifier = Modifier.clickable(onClick = onSearchClick),
        )
        Spacer(Modifier.height(DesignTokens.Spacing.Large))
        if (apps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Long press an app to pin it",
                    color = DesignTokens.Colors.Secondary,
                    fontSize = DesignTokens.FontSize.Small,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(apps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        onClick = { onAppClick(app.packageName) },
                        onLongClick = { onAppLongClick(app.packageName) },
                    )
                }
            }
        }
    }
}
