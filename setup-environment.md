# Environment Setup

## macOS (Homebrew)

```bash
brew install openjdk@17
brew install --cask android-commandlinetools
```

Add to shell profile:
```bash
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
```

Then accept licenses and install SDK components:
```bash
sdkmanager --licenses
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

## Linux

**JDK:** `sudo apt install openjdk-17-jdk`

**Android SDK:** Download command-line tools from [developer.android.com/studio#command-tools](https://developer.android.com/studio#command-tools), then:
```bash
mkdir -p ~/android-sdk/cmdline-tools
unzip commandlinetools-*.zip -d ~/android-sdk/cmdline-tools
mv ~/android-sdk/cmdline-tools/cmdline-tools ~/android-sdk/cmdline-tools/latest

export ANDROID_HOME=~/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

sdkmanager --licenses
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

Add the `export` lines to your shell profile to persist them.

## Windows

**JDK:** Download from [adoptium.net](https://adoptium.net)

**Android SDK:** Download command-line tools from [developer.android.com/studio#command-tools](https://developer.android.com/studio#command-tools) and follow the same `sdkmanager` steps above.

## Build

```bash
./gradlew assembleDebug
```
