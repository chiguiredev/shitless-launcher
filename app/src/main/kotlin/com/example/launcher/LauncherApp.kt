package com.example.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LauncherApp(vm: LauncherViewModel = viewModel()) {
    val apps by vm.filtered.collectAsState()
    val query by vm.query.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(48.dp))

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

            LazyColumn(modifier = Modifier.fillMaxSize()) {
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
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
