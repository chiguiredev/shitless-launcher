# Plan: Port shitless-launcher to Zig + Raylib + JNI

## Context
Port the Kotlin/Jetpack Compose launcher to Zig as a learning exercise.
- Raylib handles rendering, text, and input
- JNI calls into Android's existing Java runtime to get app list/labels (same APIs as Kotlin)
- Zero authored Java/Kotlin files; zero third-party C deps beyond Raylib

## New Project Location
`../shitless-launcher-zig/` (sibling to current project)

## File Structure
```
shitless-launcher-zig/
  build.zig           — cross-compile for aarch64-linux-android, pull raylib-zig
  build.zig.zon       — declares raylib-zig dependency + hash
  Makefile            — aapt package, zipalign, apksigner, adb deploy
  AndroidManifest.xml — NativeActivity + CATEGORY_HOME/DEFAULT + permissions
  res/values/strings.xml
  src/
    main.zig          — main() entry, Raylib loop, spawns app-load thread
    ui.zig            — screen state machine (pinned / search), Raylib draw calls
    jni.zig           — JNI wrappers: PackageManager, UsageStatsManager, startActivity
    apps.zig          — app list: load via JNI, filter, sort by usage score
    usage.zig         — UsageStatsManager via JNI → foreground ms per package
    persist.zig       — pinned list stored in app data dir (simple text file)
    time.zig          — clock_gettime, format HH:MM
    battery.zig       — read /sys/class/power_supply/battery/capacity
    log.zig           — __android_log_write wrapper
```

*Eliminated compared to the original APK-parsing plan:*
*`axml.zig`, `arsc.zig`, `shell.zig`, `render.zig`, `font.zig`, `font_data.zig`, `tools/`*

## Key Technical Decisions

### Raylib
- Dependency: `github.com/Not-Nik/raylib-zig` via `build.zig.zon`
- Owns `ANativeActivity_onCreate` internally; our entry point is `pub fn main() void`
- Text: `rl.drawText()`, input: `rl.getGestureDetected()` / `rl.getTouchPosition()`
- Get Android activity handle: `@as(*android_app, rl.GetAndroidApp()).activity`

### JNI App Discovery (jni.zig)
Replicates exactly what `LauncherViewModel.loadApps()` does, but called from Zig:

```zig
// Get PackageManager from NativeActivity
const env = activity.env;
const cls = env.GetObjectClass(activity.clazz);
const pm = env.CallObjectMethod(activity.clazz,
    env.GetMethodID(cls, "getPackageManager",
        "()Landroid/content/pm/PackageManager;"));

// Build Intent(ACTION_MAIN).addCategory(CATEGORY_LAUNCHER)
const intent_cls = env.FindClass("android/content/Intent");
const intent = env.NewObject(intent_cls, <init_id>, action_main_str);
env.CallObjectMethod(intent, add_category_id, category_launcher_str);

// queryIntentActivities → List<ResolveInfo>
const list = env.CallObjectMethod(pm, query_intent_activities_id, intent, 0);
const size = env.CallIntMethod(list, list_size_id);
for (0..size) |i| {
    const ri = env.CallObjectMethod(list, list_get_id, i);
    const label_cs = env.CallObjectMethod(ri, load_label_id, pm);
    // label_cs is a jstring → convert to Zig []u8 via GetStringUTFChars
    const pkg = env.CallObjectMethod(
        env.CallObjectMethod(ri, get_activity_info_id),
        get_package_name_id);
}
```

Background thread must call `activity.vm.AttachCurrentThread()` before JNI calls.

### JNI Usage Stats (usage.zig)
Replicates `LauncherViewModel.loadUsageStats()`:
```zig
// getSystemService(USAGE_STATS_SERVICE) → UsageStatsManager
// queryAndAggregateUsageStats(startOfDay, now) → Map<String, UsageStats>
// UsageStats.getTotalTimeInForeground() → long (ms)
```
Requires `PACKAGE_USAGE_STATS` permission (user must grant in Settings).

### Launching Apps (jni.zig)
```zig
// pm.getLaunchIntentForPackage(packageName) → Intent
// intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
// activity.clazz.startActivity(intent)
```

### AndroidManifest.xml
Custom manifest (not Raylib's auto-generated one):
```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"/>
<activity android:name="android.app.NativeActivity" ...>
  <meta-data android:name="android.app.lib_name" android:value="main"/>
  <intent-filter>
    <action android:name="android.intent.action.MAIN"/>
    <category android:name="android.intent.category.HOME"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <category android:name="android.intent.category.LAUNCHER"/>
  </intent-filter>
</activity>
```

### APK Assembly (Makefile)
```makefile
package:
    aapt package -f -I $(ANDROID_JAR) -M AndroidManifest.xml -S res \
        -F app-unsigned.apk
    # inject lib/arm64-v8a/libmain.so into APK zip
    zipalign -p -f 4 app-unsigned.apk app-aligned.apk
    apksigner sign --ks ~/.android/debug.keystore \
        --ks-pass pass:android app-aligned.apk
```

### UI Sketch (ui.zig)
```zig
// Pinned screen
rl.clearBackground(rl.BLACK);
rl.drawText(time_buf, pad, top, 22, rl.WHITE);
rl.drawText(batt_buf, w - 60, top, 22, rl.WHITE);
for (pinned, 0..) |app, i| {
    const y = header_h + scroll_offset + row_h * i;
    if (y < 0 or y > h) continue;  // cull
    rl.drawText(app.label, pad, y, 18, rl.WHITE);
    rl.drawText(app.duration, w - 80, y, 14, rl.GRAY);
}
rl.drawText("PHONE", ..., rl.WHITE);
rl.drawText("SEARCH", ..., rl.WHITE);
rl.drawText("CAMERA", ..., rl.WHITE);
```

### Threading
- Main thread: Raylib render loop
- Background thread: JNI PackageManager query + UsageStats (attaches to VM)
- `std.atomic.Value(bool)` dirty flag signals main thread when data is ready
- `std.Thread.Mutex` protects shared app list

## Features to Port
- Two screens: Pinned (default) and Search
- Search/filter by label (hard keyboard only — NativeActivity IME limitation)
- Pin/unpin on long press
- Usage time display (today's foreground ms from UsageStatsManager)
- Sort by usage score
- Bottom action bar: PHONE / SEARCH / CAMERA (text, no icons)
- Time + battery in header
- Momentum scroll

## Phased Delivery

### Phase 1 — Toolchain + Black Screen
- `build.zig` + `build.zig.zon`: cross-compile with raylib-zig for aarch64
- `main.zig`: Raylib init + black clear loop
- Makefile: assemble APK with custom manifest, install
- *Goal: phone shows black screen as default launcher*

### Phase 2 — Static Text + Clock
- `log.zig`, `time.zig`, `battery.zig`
- Render hardcoded list + clock + battery with `rl.drawText()`
- *Goal: text visible, clock ticks*

### Phase 3 — Input + Scroll
- Tap detection, swipe scroll with momentum, long press
- *Goal: scrollable hardcoded list, tap triggers log output*

### Phase 4 — Real App List via JNI
- `jni.zig`: PackageManager + loadLabel()
- `apps.zig`: background thread, dirty flag, main thread redraws
- *Goal: all installed apps with real labels*

### Phase 5 — Launch + Usage Stats
- `jni.zig`: startActivity via pm.getLaunchIntentForPackage()
- `usage.zig`: UsageStatsManager via JNI, sort by today's foreground time

### Phase 6 — Full Feature Parity
- `persist.zig`: pinned list
- Two screens, pin/unpin, search filter, action bar, battery

## Critical Reference Files (do not modify)
- `app/src/main/kotlin/com/shitless/launcher/LauncherViewModel.kt` — JNI call targets
- `app/src/main/kotlin/com/shitless/launcher/screens/PinnedScreen.kt` — layout reference
- `app/src/main/kotlin/com/shitless/launcher/DesignTokens.kt` — colors/spacing
- `app/src/main/AndroidManifest.xml` — permissions + intent filters

## Verification
1. `zig build` → `zig-out/lib/libmain.so` for aarch64
2. `make package` → signed APK
3. `make deploy` → installs + launches; Home button returns to launcher
4. All installed apps visible with correct labels
5. Tap launches app; long press pins/unpins
6. `adb logcat -s shitless-launcher` — no errors
