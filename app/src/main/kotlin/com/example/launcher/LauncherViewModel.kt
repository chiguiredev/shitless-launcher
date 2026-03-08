package com.example.launcher

import android.app.AppOpsManager
import android.app.Application
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val pm = app.packageManager
    private val usageStatsManager =
        app.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val appOpsManager =
        app.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    private val _returnedFromApp = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val returnedFromApp: SharedFlow<Unit> = _returnedFromApp.asSharedFlow()

    private val apps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // daily stats: packageName -> (opens, durationMs)
    private val daily = MutableStateFlow<Map<String, Pair<Int, Long>>>(emptyMap())

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    val totalDurationMs: StateFlow<Long> =
        daily
            .map { d -> d.values.sumOf { it.second } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val filtered: StateFlow<List<AppInfo>> =
        combine(apps, _query, daily) { appList, query, dailyMap ->
            val base =
                if (query.trim().isEmpty()) {
                    appList
                } else {
                    appList.filter { it.label.lowercase().contains(query.trim().lowercase()) }
                }
            base.map { app ->
                val duration = dailyMap[app.packageName]?.second ?: 0L
                app.copy(durationMs = duration)
            }.sortedByDescending { it.durationMs }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var lastLaunchedPackage: String? = null

    init {
        checkUsagePermission()
        loadUsageStats()
        loadApps()
    }

    @Suppress("DEPRECATION")
    fun checkUsagePermission() {
        val uid = getApplication<Application>().applicationInfo.uid
        val mode =
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                uid,
                getApplication<Application>().packageName,
            )
        _hasUsagePermission.value = (mode == AppOpsManager.MODE_ALLOWED)
    }

    fun loadUsageStats() {
        if (!_hasUsagePermission.value) return
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val startOfDay =
                LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            // Count opens from the event log using reference counting so that navigating
            // between activities within the same app doesn't inflate the count.
            // Also track last RESUMED / PAUSED timestamps to identify the one package
            // currently in the foreground (needed to add its live session below).
            //
            // We do NOT compute duration from events: PAUSED events are not guaranteed to
            // appear in the log (especially for home launchers and system apps), which causes
            // runaway over-counting. We also miss carry-over sessions that started before
            // midnight. The system aggregation handles both correctly.
            val opensMap = mutableMapOf<String, Int>()
            val activeCount = mutableMapOf<String, Int>()
            val lastResumedAt = mutableMapOf<String, Long>()
            val lastPausedAt = mutableMapOf<String, Long>()

            val events = usageStatsManager.queryEvents(startOfDay, now)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        val count = activeCount.getOrDefault(pkg, 0)
                        if (count == 0) opensMap[pkg] = (opensMap[pkg] ?: 0) + 1
                        activeCount[pkg] = count + 1
                        lastResumedAt[pkg] = event.timeStamp
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        val count = activeCount.getOrDefault(pkg, 0)
                        if (count > 0) activeCount[pkg] = count - 1
                        lastPausedAt[pkg] = event.timeStamp
                    }
                }
            }

            // The currently active package is the one whose last RESUMED is more recent
            // than its last PAUSED. Only one app can be in the foreground at a time, so
            // take the candidate with the latest RESUMED timestamp.
            val activePackage =
                lastResumedAt
                    .filter { (pkg, resumedTs) -> resumedTs > (lastPausedAt[pkg] ?: 0L) }
                    .maxByOrNull { it.value }
                    ?.key

            // Use system aggregation for all completed-session durations.
            // Add the live (unfinished) session only for the one currently-active package.
            val todayStats = usageStatsManager.queryAndAggregateUsageStats(startOfDay, now)

            val dailyMap = mutableMapOf<String, Pair<Int, Long>>()
            for (pkg in (opensMap.keys + todayStats.keys).toSet()) {
                val opens = opensMap[pkg] ?: 0
                var duration = todayStats[pkg]?.totalTimeInForeground ?: 0L
                if (pkg == activePackage) {
                    duration += now - (lastResumedAt[pkg] ?: now)
                }
                if (opens > 0 || duration > 0) dailyMap[pkg] = opens to duration
            }
            daily.value = dailyMap
        }
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val intent =
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
            val resolved: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
            apps.value =
                resolved
                    .map { ri ->
                        AppInfo(
                            label = ri.loadLabel(pm).toString(),
                            packageName = ri.activityInfo.packageName,
                            icon = ri.loadIcon(pm),
                        )
                    }
                    .sortedBy { it.label.lowercase() }
        }
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun launch(packageName: String) {
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        lastLaunchedPackage = packageName
        getApplication<Application>().startActivity(intent)
    }

    fun onActivityResumed() {
        checkUsagePermission()
        loadUsageStats()
        val pkg = lastLaunchedPackage ?: return
        lastLaunchedPackage = null
        viewModelScope.launch { _returnedFromApp.emit(Unit) }
    }
}
