package com.shitless.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun LauncherApp(vm: LauncherViewModel = viewModel()) {
    val apps by vm.filtered.collectAsState()
    val query by vm.query.collectAsState()
    val hasUsagePermission by vm.hasUsagePermission.collectAsState()
    val totalDurationMs by vm.totalDurationMs.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    BackHandler {
        if (query.isNotEmpty()) {
            vm.setQuery("")
        } else {
            coroutineScope.launch { listState.scrollToItem(0) }
        }
    }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    var time by remember { mutableStateOf(LocalTime.now().format(timeFormatter)) }
    LaunchedEffect(Unit) {
        while (true) {
            time = LocalTime.now().format(timeFormatter)
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }

    var battery by remember { mutableStateOf("") }
    DisposableEffect(Unit) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context,
                    intent: Intent,
                ) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) battery = "${level * 100 / scale}%"
                }
            }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(Unit) {
        vm.returnedFromApp.collect { listState.scrollToItem(0) }
    }

    // Freeze the status/nav bar insets on first valid reading so the layout never jumps
    // when the notification shade is pulled (which triggers live inset updates).
    val density = LocalDensity.current
    val safeDrawingInsets = WindowInsets.safeDrawing
    val navBarInsets = WindowInsets.navigationBars
    var topPadding by remember { mutableStateOf(0.dp) }
    var bottomPadding by remember { mutableStateOf(0.dp) }
    LaunchedEffect(Unit) {
        topPadding =
            with(density) {
                snapshotFlow { safeDrawingInsets.getTop(density) }.first().toDp()
            }
    }
    LaunchedEffect(Unit) {
        bottomPadding =
            with(density) {
                snapshotFlow { navBarInsets.getBottom(density) }.first().toDp()
            }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DesignTokens.Colors.Background,
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
                onValueChange = vm::setQuery,
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
                        AppRow(app = app, onClick = { vm.launch(app.packageName) })
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
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            },
                        ) {
                            Text("Open Settings")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = DesignTokens.Spacing.Medium),
    ) {
        Text(
            text = app.label,
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

private fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60
    return "%02dh %02dm %02ds".format(hours, minutes, seconds)
}
