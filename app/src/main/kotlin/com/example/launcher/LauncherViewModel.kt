package com.example.launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val pm = app.packageManager
    private val prefs = app.getSharedPreferences("launcher_stats", Context.MODE_PRIVATE)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _usage = MutableStateFlow<Map<String, Pair<Int, Long>>>(emptyMap())

    val filtered: StateFlow<List<AppInfo>> = combine(_apps, _query, _usage) { apps, query, usage ->
        val base = if (query.trim().isEmpty()) apps
                   else apps.filter { it.label.lowercase().contains(query.trim().lowercase()) }
        base.map { app ->
            val (opens, duration) = usage[app.packageName] ?: (0 to 0L)
            app.copy(opens = opens, durationMs = duration)
        }.sortedByDescending { it.durationMs }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var lastLaunchedPackage: String? = null
    private var lastLaunchTime: Long = 0L

    init {
        checkMidnightReset()
        loadUsage()
        loadApps()
    }

    private fun checkMidnightReset() {
        val today = LocalDate.now().toString()
        if (prefs.getString("reset_date", null) != today) {
            prefs.edit().clear().putString("reset_date", today).apply()
            _usage.value = emptyMap()
        }
    }

    private fun loadUsage() {
        val all = prefs.all
        val map = mutableMapOf<String, Pair<Int, Long>>()
        for (key in all.keys) {
            if (key.startsWith("opens_")) {
                val pkg = key.removePrefix("opens_")
                val opens = (all[key] as? Int) ?: 0
                val duration = prefs.getLong("duration_$pkg", 0L)
                map[pkg] = opens to duration
            }
        }
        _usage.value = map
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolved: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
            val list = resolved
                .map { ri ->
                    AppInfo(
                        label = ri.loadLabel(pm).toString(),
                        packageName = ri.activityInfo.packageName,
                        icon = ri.loadIcon(pm),
                    )
                }
                .sortedBy { it.label.lowercase() }
            _apps.value = list
        }
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun launch(packageName: String) {
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val current = _usage.value
        val (opens, duration) = current[packageName] ?: (0 to 0L)
        val newOpens = opens + 1
        _usage.value = current + (packageName to (newOpens to duration))
        prefs.edit().putInt("opens_$packageName", newOpens).apply()

        lastLaunchedPackage = packageName
        lastLaunchTime = System.currentTimeMillis()

        getApplication<Application>().startActivity(intent)
    }

    fun onActivityResumed() {
        checkMidnightReset()
        val pkg = lastLaunchedPackage ?: return
        lastLaunchedPackage = null

        val elapsed = System.currentTimeMillis() - lastLaunchTime
        if (elapsed <= 0) return

        val current = _usage.value
        val (opens, duration) = current[pkg] ?: (0 to 0L)
        val newDuration = duration + elapsed
        _usage.value = current + (pkg to (opens to newDuration))
        prefs.edit().putLong("duration_$pkg", newDuration).apply()
    }
}
