package com.shitless.launcher

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

    private val daily = MutableStateFlow<Map<String, Long>>(emptyMap())

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    val totalDurationMs: StateFlow<Long> =
        daily
            .map { stats -> stats.values.sum() }
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
                app.copy(durationMs = dailyMap[app.packageName] ?: 0L)
            }.sortedByDescending { it.durationMs }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val pinnedPrefs = app.getSharedPreferences("launcher_pinned", Context.MODE_PRIVATE)

    private val _pinnedPackages =
        MutableStateFlow<Set<String>>(
            pinnedPrefs.getStringSet("pinned", emptySet()) ?: emptySet(),
        )
    val pinnedPackages: StateFlow<Set<String>> = _pinnedPackages.asStateFlow()

    val pinnedApps: StateFlow<List<AppInfo>> =
        combine(apps, _pinnedPackages, daily) { appList, pinned, dailyMap ->
            appList
                .filter { it.packageName in pinned }
                .map { app -> app.copy(durationMs = dailyMap[app.packageName] ?: 0L) }
                .sortedBy { it.label.lowercase() }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var appWasLaunched = false

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

            val lastResumedAt = mutableMapOf<String, Long>()
            val lastPausedAt = mutableMapOf<String, Long>()

            val events = usageStatsManager.queryEvents(startOfDay, now)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> lastResumedAt[pkg] = event.timeStamp
                    UsageEvents.Event.ACTIVITY_PAUSED -> lastPausedAt[pkg] = event.timeStamp
                }
            }

            val activePackage =
                lastResumedAt
                    .filter { (pkg, resumedAt) -> resumedAt > (lastPausedAt[pkg] ?: 0L) }
                    .maxByOrNull { it.value }
                    ?.key

            val todayStats = usageStatsManager.queryAndAggregateUsageStats(startOfDay, now)

            val dailyMap = mutableMapOf<String, Long>()
            for (pkg in todayStats.keys) {
                var duration = todayStats[pkg]?.totalTimeInForeground ?: 0L
                if (pkg == activePackage) {
                    duration += now - (lastResumedAt[pkg] ?: now)
                }
                if (duration > 0) dailyMap[pkg] = duration
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
                    .map { resolveInfo ->
                        AppInfo(
                            label = resolveInfo.loadLabel(pm).toString(),
                            packageName = resolveInfo.activityInfo.packageName,
                        )
                    }
                    .sortedBy { it.label.lowercase() }
        }
    }

    fun setQuery(newQuery: String) {
        _query.value = newQuery
    }

    fun togglePin(packageName: String) {
        val current = _pinnedPackages.value.toMutableSet()
        if (packageName in current) current.remove(packageName) else current.add(packageName)
        _pinnedPackages.value = current
        pinnedPrefs.edit().putStringSet("pinned", current).apply()
    }

    fun launch(packageName: String) {
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appWasLaunched = true
        getApplication<Application>().startActivity(intent)
    }

    fun onActivityResumed() {
        checkUsagePermission()
        loadUsageStats()
        if (!appWasLaunched) return
        appWasLaunched = false
        viewModelScope.launch { _returnedFromApp.emit(Unit) }
    }
}
