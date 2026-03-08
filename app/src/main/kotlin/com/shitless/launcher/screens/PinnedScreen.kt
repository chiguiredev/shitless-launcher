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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Phone
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
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PinnedScreen(
    apps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onCameraClick: () -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    time: String,
    battery: String,
    topPadding: Dp,
    bottomPadding: Dp,
) {
    val lazyListState = rememberLazyListState()
    val reorderState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            onReorder(from.index, to.index)
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = topPadding)
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
        if (apps.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Long press an app to pin it",
                    color = DesignTokens.Colors.Secondary,
                    fontSize = DesignTokens.FontSize.Small,
                )
            }
        } else {
            LazyColumn(state = lazyListState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(apps, key = { it.packageName }) { app ->
                    ReorderableItem(reorderState, key = app.packageName) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppRow(
                                app = app,
                                onClick = { onAppClick(app.packageName) },
                                onLongClick = { onAppLongClick(app.packageName) },
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Drag to reorder",
                                tint = DesignTokens.Colors.Secondary,
                                modifier =
                                    Modifier
                                        .draggableHandle()
                                        .padding(DesignTokens.Spacing.Medium),
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = bottomPadding + DesignTokens.Spacing.XLarge)
                    .padding(vertical = DesignTokens.Spacing.Large),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Phone",
                tint = DesignTokens.Colors.Primary,
                modifier =
                    Modifier
                        .size(DesignTokens.IconSize.Large)
                        .clickable(onClick = onPhoneClick)
                        .padding(DesignTokens.Spacing.Small),
            )
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = DesignTokens.Colors.Primary,
                modifier =
                    Modifier
                        .size(DesignTokens.IconSize.Large)
                        .clickable(onClick = onSearchClick)
                        .padding(DesignTokens.Spacing.Small),
            )
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Camera",
                tint = DesignTokens.Colors.Primary,
                modifier =
                    Modifier
                        .size(DesignTokens.IconSize.Large)
                        .clickable(onClick = onCameraClick)
                        .padding(DesignTokens.Spacing.Small),
            )
        }
    }
}
