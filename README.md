# Shitless Launcher

Minimalist Android launcher. Searchable app list on a plain black screen.

## Setup (macOS)

```bash
brew install openjdk@17
brew install --cask android-commandlinetools
```

Add to `~/.zshrc`:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
```

> `JAVA_HOME` must point into `libexec/openjdk.jdk/Contents/Home` — not the Homebrew prefix root. Gradle fails to find Java otherwise.

Accept licenses and install SDK components:

```bash
sdkmanager --licenses
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

No Android Studio required.

## Build & Install

```bash
./gradlew assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug           # build + install on connected device/emulator
adb shell am start -n com.shitless.launcher/.HomeActivity  # launch
```

To uninstall: `adb uninstall com.shitless.launcher`

## Set as Default Launcher

After installing, press Home. Android will prompt you to pick a home app —
choose Shitless Launcher → Always.

To revert: Settings → Apps → Default apps → Home app.

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
