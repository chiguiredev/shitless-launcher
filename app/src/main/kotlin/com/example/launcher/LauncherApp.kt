package com.example.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    BackHandler {
        if (query.isNotEmpty()) vm.setQuery("")
        else coroutineScope.launch { listState.scrollToItem(0) }
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
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
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
        topPadding = with(density) {
            snapshotFlow { safeDrawingInsets.getTop(density) }.first().toDp()
        }
    }
    LaunchedEffect(Unit) {
        bottomPadding = with(density) {
            snapshotFlow { navBarInsets.getBottom(density) }.first().toDp()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding, bottom = bottomPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = time, color = Color.White, fontSize = 20.sp)
                Text(text = battery, color = Color.White, fontSize = 20.sp)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search apps", color = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.DarkGray,
                    unfocusedBorderColor = Color.DarkGray,
                    cursorColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(apps, key = { it.packageName }) { app ->
                    AppRow(app = app, onClick = { vm.launch(app.packageName) })
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 16.sp,
        )
        Text(
            text = "${app.opens} opens · ${formatDuration(app.durationMs)}",
            color = Color.Gray,
            fontSize = 12.sp,
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60
    return "${hours}h ${minutes}m ${seconds}s"
}
