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

private const val OPEN_SCORE_MS = 60_000L // each open counts as 1 minute toward score

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val pm = app.packageManager
    private val dailyPrefs = app.getSharedPreferences("launcher_daily", Context.MODE_PRIVATE)
    private val scorePrefs = app.getSharedPreferences("launcher_score", Context.MODE_PRIVATE)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // daily stats: packageName -> (opens, durationMs) — resets at midnight
    private val _daily = MutableStateFlow<Map<String, Pair<Int, Long>>>(emptyMap())
    // persistent score: packageName -> ms — never resets
    private val _scores = MutableStateFlow<Map<String, Long>>(emptyMap())

    val filtered: StateFlow<List<AppInfo>> = combine(_apps, _query, _daily, _scores) { apps, query, daily, scores ->
        val base = if (query.trim().isEmpty()) apps
                   else apps.filter { it.label.lowercase().contains(query.trim().lowercase()) }
        base.map { app ->
            val (opens, duration) = daily[app.packageName] ?: (0 to 0L)
            app.copy(opens = opens, durationMs = duration)
        }.sortedByDescending { scores[it.packageName] ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var lastLaunchedPackage: String? = null
    private var lastLaunchTime: Long = 0L

    init {
        checkDailyReset()
        loadData()
        loadApps()
    }

    private fun checkDailyReset() {
        val today = LocalDate.now().toString()
        if (dailyPrefs.getString("date", null) != today) {
            dailyPrefs.edit().clear().putString("date", today).apply()
            _daily.value = emptyMap()
        }
    }

    private fun loadData() {
        val all = dailyPrefs.all
        val dailyMap = mutableMapOf<String, Pair<Int, Long>>()
        for (key in all.keys) {
            if (key.startsWith("opens_")) {
                val pkg = key.removePrefix("opens_")
                dailyMap[pkg] = ((all[key] as? Int) ?: 0) to dailyPrefs.getLong("duration_$pkg", 0L)
            }
        }
        _daily.value = dailyMap

        val scoresAll = scorePrefs.all
        _scores.value = scoresAll
            .filterKeys { it.startsWith("score_") }
            .mapKeys { it.key.removePrefix("score_") }
            .mapValues { (it.value as? Long) ?: 0L }
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolved: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
            _apps.value = resolved
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

        val (opens, duration) = _daily.value[packageName] ?: (0 to 0L)
        _daily.value = _daily.value + (packageName to (opens + 1 to duration))
        dailyPrefs.edit().putInt("opens_$packageName", opens + 1).apply()

        val newScore = (_scores.value[packageName] ?: 0L) + OPEN_SCORE_MS
        _scores.value = _scores.value + (packageName to newScore)
        scorePrefs.edit().putLong("score_$packageName", newScore).apply()

        lastLaunchedPackage = packageName
        lastLaunchTime = System.currentTimeMillis()

        getApplication<Application>().startActivity(intent)
    }

    fun onActivityResumed() {
        checkDailyReset()
        val pkg = lastLaunchedPackage ?: return
        lastLaunchedPackage = null

        val elapsed = System.currentTimeMillis() - lastLaunchTime
        if (elapsed <= 0) return

        val (opens, duration) = _daily.value[pkg] ?: (0 to 0L)
        _daily.value = _daily.value + (pkg to (opens to duration + elapsed))
        dailyPrefs.edit().putLong("duration_$pkg", duration + elapsed).apply()

        val newScore = (_scores.value[pkg] ?: 0L) + elapsed
        _scores.value = _scores.value + (pkg to newScore)
        scorePrefs.edit().putLong("score_$pkg", newScore).apply()
    }
}
