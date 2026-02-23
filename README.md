# MinLauncher

A minimalist Android launcher built with Kotlin and Jetpack Compose. Shows all installed apps in a searchable list on a plain black screen.

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 11+ | `java -version` to check |
| Android SDK | API 35 | Install via Android Studio or `sdkmanager` |
| Android Build Tools | 35.x | Included with SDK |
| `adb` | any | Part of Android Platform Tools |

You do **not** need Android Studio — any editor works. The Gradle wrapper (`./gradlew`) downloads Gradle automatically on first run.

### Set up ANDROID_HOME

The build requires the Android SDK path to be known. Pick one option:

**Option A — environment variable (recommended)**
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk   # macOS default
export PATH=$ANDROID_HOME/platform-tools:$PATH
```

**Option B — local.properties file** (not committed to git)
```bash
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

---

## Build

```bash
# Debug APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleDebug

# Release APK (unsigned)
./gradlew assembleRelease
```

First build downloads Gradle 8.9 and all dependencies (~200 MB). Subsequent builds are fast.

---

## Run on a Device or Emulator

### Physical device

1. Enable **Developer Options** on the device:
   *Settings → About phone → tap "Build number" 7 times*
2. Enable **USB Debugging**:
   *Settings → Developer options → USB debugging*
3. Connect via USB and verify:
   ```bash
   adb devices
   # Should show your device serial number with "device" status
   ```

### Emulator (Android Studio AVD)

1. Open Android Studio → *Device Manager* → create an AVD with API 26+.
2. Start the emulator.
3. Verify with `adb devices`.

### Install and launch

```bash
# Build + install in one step
./gradlew installDebug

# Launch the app directly
adb shell am start -n com.example.launcher/.HomeActivity
```

---

## Set as Default Launcher

After installing, you need to tell Android to use MinLauncher as the home app:

1. Press the **Home** button on the device.
2. Android will show a dialog: *"Select a home app"*.
3. Choose **MinLauncher** → tap **Always**.

To revert, go to *Settings → Apps → Default apps → Home app* and select a different launcher.

---

## Uninstall

```bash
adb uninstall com.example.launcher
```

---

## Project Structure

```
app/src/main/
  kotlin/com/example/launcher/
    HomeActivity.kt        — entry point
    LauncherApp.kt         — Compose UI
    LauncherViewModel.kt   — app list + search logic
    AppInfo.kt             — data model
  AndroidManifest.xml      — launcher intent filters
  res/values/themes.xml    — fullscreen black theme
```
