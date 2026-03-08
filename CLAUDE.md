# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Release build (minified + signed)
./gradlew assembleRelease

# Install debug on connected device/emulator
./gradlew installDebug

# Build + install + launch in one step
./gradlew installDebug && adb shell am start -n com.shitless.launcher/.HomeActivity
```

Requires Android SDK with `ANDROID_HOME` set (or `local.properties` with `sdk.dir`).

## Project Structure

```
app/src/main/
  kotlin/com/shitless/launcher/
    HomeActivity.kt       — entry point, sets Compose content, tracks usage on resume
    LauncherApp.kt        — root Composable + AppRow (shows opens + duration stats)
    LauncherViewModel.kt  — loads apps, search filter, usage tracking, scoring, launch
    AppInfo.kt            — data class (label, packageName, icon, opens, durationMs)
    DesignTokens.kt       — shared design constants (colors, spacing, typography)
  AndroidManifest.xml     — declares HOME + DEFAULT + QUERY_ALL_PACKAGES permission
  res/values/
    themes.xml            — fullscreen black theme, transparent system bars
    strings.xml           — app_name = "Shitless Launcher"
proguard-rules.pro        — keep annotations, suppress kotlinx warnings
setup-environment.md      — macOS SDK setup guide
README.md                 — user-facing documentation
```

## Architecture

Single-activity, single-screen app using MVVM + Jetpack Compose.

`LauncherViewModel` (AndroidViewModel):
- Loads all launchable apps via `PackageManager.queryIntentActivities(ACTION_MAIN / CATEGORY_LAUNCHER)`
- Maintains two `SharedPreferences` stores:
  - `launcher_daily` — daily open count + duration per app (resets at midnight via `LocalDate.now()` check)
  - `launcher_score` — all-time persistent score per app (never resets, used for sort order)
- Exposes a `filtered` StateFlow combining `_apps`, `_query`, `_daily`, and `_scores` via `Flow.combine()`
- Sort order is by persistent score (descending); alphabetical as tiebreaker

`LauncherApp.kt` collects state flows and renders:
- `OutlinedTextField` for live search
- `LazyColumn` of `AppRow` items showing label, daily opens count, and formatted duration
- Auto-scrolls list to top on `ON_RESUME` via `DisposableEffect` + `LifecycleEventObserver`

## Usage Tracking & Scoring

When an app is launched (`LauncherViewModel.launch()`):
1. Daily opens count incremented, persisted to `launcher_daily`
2. Launch timestamp recorded
3. `OPEN_SCORE_MS` (60,000 ms) added to persistent score

When the launcher resumes (`HomeActivity.onActivityResumed()` → `vm.onActivityResumed()`):
1. Elapsed time since launch is calculated
2. Added to daily duration (`launcher_daily`) and persistent score (`launcher_score`)

Daily stats reset automatically when the date changes (checked on init and every resume).

## Build Configuration

- `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`
- Release build: `isMinifyEnabled = true`, `isShrinkResources = true`
- Release signing uses `~/.android/debug.keystore` (debug key, not for production distribution)
- Kotlin 2.1.0, AGP 8.7.3, Compose BOM 2025.01.00

## Key Android Details

- `AndroidManifest.xml` must declare `CATEGORY_HOME` and `CATEGORY_DEFAULT` for Android to recognize this as a launcher.
- `QUERY_ALL_PACKAGES` permission is required to list all installed apps on API 30+.
- `launchMode="singleTask"` and `stateNotNeeded="true"` are standard launcher flags that prevent multiple instances and allow the OS to kill/restore the activity freely.
- Apps are launched via `PackageManager.getLaunchIntentForPackage` with `FLAG_ACTIVITY_NEW_TASK`.
