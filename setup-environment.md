# Environment Setup

## macOS (Homebrew)

```bash
brew install openjdk@17
brew install --cask android-commandlinetools
```

Add to shell profile (`~/.zshrc`):
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
```

> **Note:** `JAVA_HOME` must point to the JDK home inside `libexec/openjdk.jdk/Contents/Home`,
> not the Homebrew prefix root. Gradle will fail to find Java otherwise.

Then accept licenses and install SDK components:
```bash
sdkmanager --licenses
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

## Build

```bash
# Debug build + install
./gradlew installDebug

# Release build + install (minified, signed with debug keystore)
./gradlew installRelease
```
