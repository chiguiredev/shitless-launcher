# Environment Setup

## JDK 11+

**macOS:** `brew install openjdk@17`
**Linux:** `sudo apt install openjdk-17-jdk`
**Windows:** Download from [adoptium.net](https://adoptium.net)

Add to shell profile:
```bash
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"  # macOS
```

## Android SDK

Download command-line tools from [developer.android.com/studio#command-tools](https://developer.android.com/studio#command-tools), then:

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

## Build

```bash
./gradlew assembleDebug
```
