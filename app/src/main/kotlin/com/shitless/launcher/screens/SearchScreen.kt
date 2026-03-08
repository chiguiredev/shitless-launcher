package com.shitless.launcher.screens

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import com.shitless.launcher.AppInfo
import com.shitless.launcher.DesignTokens
import com.shitless.launcher.components.AppRow
import com.shitless.launcher.components.formatDuration

@Composable
fun SearchScreen(
    apps: List<AppInfo>,
    pinnedPackages: List<String>,
    query: String,
    hasUsagePermission: Boolean,
    totalDurationMs: Long,
    listState: LazyListState,
    time: String,
    battery: String,
    topPadding: Dp,
    bottomPadding: Dp,
    onQueryChange: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
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

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search apps", color = DesignTokens.Colors.Secondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = DesignTokens.Colors.Primary,
                    unfocusedTextColor = DesignTokens.Colors.Primary,
                    focusedBorderColor = DesignTokens.Colors.Border,
                    unfocusedBorderColor = DesignTokens.Colors.Border,
                    cursorColor = DesignTokens.Colors.Primary,
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(DesignTokens.Spacing.Large))

        if (hasUsagePermission) {
            Text(
                text = "Total time: ${formatDuration(totalDurationMs)}",
                color = DesignTokens.Colors.Primary,
                fontSize = DesignTokens.FontSize.Large,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.Small))
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(apps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        isPinned = app.packageName in pinnedPackages,
                        onClick = { onAppClick(app.packageName) },
                        onLongClick = { onAppLongClick(app.packageName) },
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Usage access required",
                        color = DesignTokens.Colors.Primary,
                        fontSize = DesignTokens.FontSize.Medium,
                    )
                    Spacer(Modifier.height(DesignTokens.Spacing.Large))
                    Button(onClick = onOpenSettings) {
                        Text("Open Settings")
                    }
                }
            }
        }
    }
}
