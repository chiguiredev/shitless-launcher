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

private const val OPENS_WEIGHT_MS = 60_000L // each open counts as 1 minute toward score

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val pm = app.packageManager
    private val prefs = app.getSharedPreferences("launcher_stats", Context.MODE_PRIVATE)
    private val allTimePrefs = app.getSharedPreferences("launcher_alltime", Context.MODE_PRIVATE)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _usage = MutableStateFlow<Map<String, Pair<Int, Long>>>(emptyMap())
    // all-time: packageName -> Pair(durationMs, opens)
    private val _allTimeUsage = MutableStateFlow<Map<String, Pair<Long, Int>>>(emptyMap())

    val filtered: StateFlow<List<AppInfo>> = combine(_apps, _query, _usage, _allTimeUsage) { apps, query, usage, allTime ->
        val base = if (query.trim().isEmpty()) apps
                   else apps.filter { it.label.lowercase().contains(query.trim().lowercase()) }
        base.map { app ->
            val (opens, duration) = usage[app.packageName] ?: (0 to 0L)
            app.copy(opens = opens, durationMs = duration)
        }.sortedByDescending { app ->
            val (allTimeDuration, allTimeOpens) = allTime[app.packageName] ?: (0L to 0)
            allTimeDuration + allTimeOpens * OPENS_WEIGHT_MS
        }
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

        val allTimeAll = allTimePrefs.all
        val allTimeMap = mutableMapOf<String, Pair<Long, Int>>()
        for (key in allTimeAll.keys) {
            if (key.startsWith("duration_")) {
                val pkg = key.removePrefix("duration_")
                val duration = (allTimeAll[key] as? Long) ?: 0L
                val opens = allTimePrefs.getInt("opens_$pkg", 0)
                allTimeMap[pkg] = duration to opens
            }
        }
        _allTimeUsage.value = allTimeMap
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

        val (allTimeDuration, allTimeOpens) = _allTimeUsage.value[packageName] ?: (0L to 0)
        val newAllTimeOpens = allTimeOpens + 1
        _allTimeUsage.value = _allTimeUsage.value + (packageName to (allTimeDuration to newAllTimeOpens))
        allTimePrefs.edit().putInt("opens_$packageName", newAllTimeOpens).apply()

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

        val (allTimeDuration, allTimeOpens) = _allTimeUsage.value[pkg] ?: (0L to 0)
        val newAllTimeDuration = allTimeDuration + elapsed
        _allTimeUsage.value = _allTimeUsage.value + (pkg to (newAllTimeDuration to allTimeOpens))
        allTimePrefs.edit().putLong("duration_$pkg", newAllTimeDuration).apply()
    }
}
