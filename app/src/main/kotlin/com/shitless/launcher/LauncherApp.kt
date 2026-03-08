package com.shitless.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shitless.launcher.screens.PinnedScreen
import com.shitless.launcher.screens.SearchScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun LauncherApp(vm: LauncherViewModel = viewModel()) {
    val apps by vm.filtered.collectAsState()
    val pinnedApps by vm.pinnedApps.collectAsState()
    val pinnedPackages by vm.pinnedPackages.collectAsState()
    val query by vm.query.collectAsState()
    val hasUsagePermission by vm.hasUsagePermission.collectAsState()
    val totalDurationMs by vm.totalDurationMs.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showSearch by remember { mutableStateOf(false) }

    BackHandler {
        if (showSearch) {
            if (query.isNotEmpty()) vm.setQuery("") else showSearch = false
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
                    context: Context,
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
        if (showSearch) {
            SearchScreen(
                apps = apps,
                pinnedPackages = pinnedPackages,
                query = query,
                hasUsagePermission = hasUsagePermission,
                totalDurationMs = totalDurationMs,
                listState = listState,
                time = time,
                battery = battery,
                topPadding = topPadding,
                bottomPadding = bottomPadding,
                onQueryChange = vm::setQuery,
                onAppClick = { vm.launch(it) },
                onAppLongClick = { vm.togglePin(it) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )
        } else {
            PinnedScreen(
                apps = pinnedApps,
                onAppClick = { vm.launch(it) },
                onAppLongClick = { vm.togglePin(it) },
                onSearchClick = { showSearch = true },
                onReorder = { from, to -> vm.reorderPinnedApps(from, to) },
                time = time,
                battery = battery,
                topPadding = topPadding,
                bottomPadding = bottomPadding,
            )
        }
    }
}
