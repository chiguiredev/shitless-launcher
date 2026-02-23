# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Build + install in one step
./gradlew installDebug && adb shell am start -n com.example.launcher/.HomeActivity
```

Requires Android SDK with `ANDROID_HOME` set (or `local.properties` with `sdk.dir`).

## Project Structure

```
app/src/main/
  kotlin/com/example/launcher/
    HomeActivity.kt       — entry point, sets Compose content
    LauncherApp.kt        — root Composable + AppRow/AppIcon
    LauncherViewModel.kt  — loads installed apps, handles search filter, launches apps
    AppInfo.kt            — data class (label, packageName, icon)
  AndroidManifest.xml     — declares HOME + DEFAULT category so Android offers it as a launcher
  res/values/
    themes.xml            — fullscreen black theme, transparent system bars
```

## Architecture

Single-activity, single-screen app. `LauncherViewModel` (AndroidViewModel) uses `PackageManager.queryIntentActivities` with `ACTION_MAIN / CATEGORY_LAUNCHER` to get all launchable apps, sorts them alphabetically, and exposes a `filtered` StateFlow driven by a `query` StateFlow. `LauncherApp.kt` collects both flows and renders a search field + `LazyColumn` of apps.

## Key Android Details

- `AndroidManifest.xml` must declare `CATEGORY_HOME` and `CATEGORY_DEFAULT` for Android to recognize this as a launcher.
- `launchMode="singleTask"` and `stateNotNeeded="true"` are standard launcher flags that prevent multiple instances and allow the OS to kill/restore the activity freely.
- Apps are launched via `PackageManager.getLaunchIntentForPackage` with `FLAG_ACTIVITY_NEW_TASK`.
