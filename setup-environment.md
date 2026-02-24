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

## Build

```bash
./gradlew assembleDebug
```
